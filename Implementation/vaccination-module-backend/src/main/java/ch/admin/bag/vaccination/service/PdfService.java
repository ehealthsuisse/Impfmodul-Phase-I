/**
 * Copyright (c) 2022 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ch.admin.bag.vaccination.service;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.HorizontalAlignment;
import be.quodlibet.boxable.Row;
import be.quodlibet.boxable.line.LineStyle;
import ch.admin.bag.vaccination.data.PdfDocument;
import ch.admin.bag.vaccination.data.request.TranslationsRequest;
import ch.admin.bag.vaccination.exception.BusinessException;
import ch.admin.bag.vaccination.utils.DateComparator;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;

/**
 * Generate pdf files according to PDF/1A standard, using Pdf-box library
 */
@Slf4j
@Service
public class PdfService {
  private static final String FONT = new ClassPathResource("font/FreeSans.ttf").getPath();
  private static final String FONT_BOLD = new ClassPathResource("font/FreeSansBold.ttf").getPath();
  private static final String FONT_OBLIQUE = new ClassPathResource("font/FreeSansOblique.ttf").getPath();

  private static final String ICC_PROFILE = "font/sRGB_CS_profile.icm";
  private static final Color COLOR_DARKBLUE = Color.decode("#014C8A");
  private static final Color COLOR_LIGHTBLUE = Color.decode("#C8E8F6");
  private static final Color COLOR_WHITE = Color.decode("#FFFFFF");
  private static final float[] VACCINE_TABLE_WIDTHS = {30, 10, 15, 20, 25};
  private static final float[] MEDICAL_PROBLEMS_TABLE_WIDTHS = {10, 15, 15, 15, 30, 15};
  private static final float[] PASTILLNESS_TABLE_WIDTHS = {10, 15, 15, 15, 45};
  private static final float[] ADVERSE_EVENTS_TABLE_WIDTHS = {100};
  private static final float MARGIN = 50;
  private static final float CELL_HEIGHT = 15;
  private static final float LONG_LINE_BREAK = 20;
  private static final float FONT_SIZE = 10;
  private static final float FONT_SIZE_TITLE = 15;
  private static final String BOLD = "bold";
  private static final String OBLIQUE = "oblique";
  private static final String EMPTY_USER_PASSWORD = "";
  private static final String EMPTY_CONTENT = "";
  private static final String DEFAULT_FONT = "";
  private final ThreadLocal<TranslationsRequest> translationsRequest = new ThreadLocal<>();

  @Autowired
  private PdfOutputConfig pdfOutputConfig;

  public InputStream create(VaccinationRecordDTO vaccinationRecordDTO, TranslationsRequest translationsRequest) {
    this.translationsRequest.set(translationsRequest);
    try (PDDocument document = createDocumentAndItsMeta();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PdfDocument pdfDocument = new PdfDocument(document);
      fillDocumentContent(vaccinationRecordDTO, pdfDocument);

      addPageNumbers(pdfDocument, vaccinationRecordDTO.getPatient().getFullName());
      document.save(out);

      out.flush();
      byte[] pdf = out.toByteArray();
      ByteArrayInputStream validateIn = new ByteArrayInputStream(pdf);
      validatePdf(validateIn);
      validateIn.close();

      return setReadOnlyPermissions(pdf);
    } catch (Exception ex) {
      log.error("error while exportToPDF {}", ex.getMessage());
      throw new TechnicalException("Pdf generation failed", ex);
    }
  }

  private void addAdverseEvents(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = getFont(BOLD, pdfDocument);
    contentStream.setFont(font, FONT_SIZE_TITLE);
    contentStream.beginText();
    updateYposition(MARGIN, pdfDocument);
    contentStream.newLineAtOffset(MARGIN, pdfDocument.getYPositionOfTheLastLine());

    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(I18nKey.ADVERSE_EVENTS.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    updateYposition(LONG_LINE_BREAK, pdfDocument);
    BaseTable table = createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    List<AllergyDTO> dtos = DateComparator.sortByDateDesc(vaccinationRecordDTO.getAllergies(),
        Comparator.comparing(AllergyDTO::getOccurrenceDate));
    log.debug("create {} allergies", dtos == null ? 0 : dtos.size());
    if (dtos != null) {
      for (AllergyDTO dto : dtos) {
        addAllergyRow(table, dto, pdfDocument, vaccinationRecordDTO.getLang());
      }
    }
    table.draw();
  }

  private Cell<PDPage> addAllergyCell(AllergyDTO dto, PdfDocument pdfDocument, Row<PDPage> row) throws IOException {
    log.debug("addRow {}", dto.getCode().getName());
    String value = (dto.isValidated() ? "(+) " : "") + getTranslation(translationsRequest.get().getAllergyCodeToName(), dto.getCode()) + ", " +
        getFormattedDate(dto.getOccurrenceDate()) + ", " + dto.getAuthor().getUser().getFullName();
    Cell<PDPage> allergyCell = addCell(BOLD, value, ADVERSE_EVENTS_TABLE_WIDTHS[0], row, pdfDocument);
    allergyCell.setFontSize(FONT_SIZE);
    setRowColor(row, pdfDocument.getRowBackgoundColor());
    return allergyCell;
  }

  private void addAllergyRow(BaseTable table, AllergyDTO dto, PdfDocument pdfDocument, String lang) throws IOException {
    if (dto == null) {
      log.warn("addRow AllergyDTO null");
      return;
    }
    Row<PDPage> row = table.createRow(CELL_HEIGHT);
    Cell<PDPage> allergyCell = addAllergyCell(dto, pdfDocument, row);
    addCommentRow(table, dto, pdfDocument, allergyCell, lang);
    updateYposition(row.getHeight(), pdfDocument);
    swapColor(pdfDocument);
  }

  private void addBaseVaccinations(BaseTable table, VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    addVaccinationTableHeader(table, vaccinationRecordDTO.getLang(), CELL_HEIGHT, pdfDocument);
    addVaccinationBasedOnTargetDisease(vaccinationRecordDTO, pdfDocument, table,
        pdfOutputConfig.getBasicVaccination().getCodes(), pdfOutputConfig.getBasicVaccination().isKeepEmpty());

    table.draw();
  }

  private Cell<PDPage> addCell(String content, float width, Row<PDPage> headerRow, PdfDocument pdfDocument)
      throws IOException {
    return addCell(DEFAULT_FONT, content, width, headerRow, pdfDocument);
  }

  private Cell<PDPage> addCell(String font, String content, float width, Row<PDPage> headerRow, PdfDocument pdfDocument)
      throws IOException {
    Cell<PDPage> cell = headerRow.createCell(width, content);
    cell.setFont(getFont(font, pdfDocument));
    return cell;
  }

  private void addCommentCells(PdfDocument pdfDocument, CommentDTO commentDTO, BaseTable table, String lang)
      throws IOException {
    String header = I18nKey.LAST_MODIFIED_BY.getTranslation(lang) + commentDTO.getAuthor() + " " +
        getFormattedDateTime(commentDTO.getDate());
    String[] textLines = commentDTO.getText().split("\\R");

    float colWidth = ADVERSE_EVENTS_TABLE_WIDTHS[0];

    // First row: header
    Row<PDPage> headerRow = table.createRow(CELL_HEIGHT / 2);
    Cell<PDPage> headerCell = addCell(header, colWidth, headerRow, pdfDocument);
    headerCell.setFontSize(8);
    headerCell.setBottomBorderStyle(null);
    setRowColor(headerRow, pdfDocument.getRowBackgoundColor());

    // Each line of the comment's text goes into a new row
    for (int i = 0; i < textLines.length; i++) {
      String line = textLines[i];
      Row<PDPage> lineRow = table.createRow(CELL_HEIGHT / 2);
      Cell<PDPage> lineCell = addCell(line.trim(), colWidth, lineRow, pdfDocument);
      lineCell.setFontSize(8);
      lineCell.setTopBorderStyle(null);
      setRowColor(lineRow, pdfDocument.getRowBackgoundColor());

      if (i == textLines.length - 1) {
        // Set bottom border only after the texts last line
        lineCell.setBottomBorderStyle(new LineStyle(Color.BLACK, 1.3f));
      } else {
        lineCell.setBottomBorderStyle(null);
      }
    }
  }

  private void addCommentRow(BaseTable table, AllergyDTO dto, PdfDocument pdfDocument, Cell<PDPage> allergyCell,
      String lang) throws IOException {
    CommentDTO commentDTO = dto.getComment();
    if (commentDTO != null) {
      addCommentCells(pdfDocument, commentDTO, table, lang);

      // Remove bottom border from allergy cell so allergy and comment appear as one block
      allergyCell.setBottomBorderStyle(null);

      Row<PDPage> lastRow = table.getRows().get(table.getRows().size() - 1);
      updateYposition(lastRow.getHeight(), pdfDocument);
      setRowColor(lastRow, pdfDocument.getRowBackgoundColor());
    }
  }

  private void addLogo(String lang, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document,
        PdfService.class.getResourceAsStream(I18nKey.LOGO_FILE_NAME.getTranslation(lang)).readAllBytes(), "EPLogo");
    PDPageContentStream contentStream =
        new PDPageContentStream(document, document.getPage(0), PDPageContentStream.AppendMode.APPEND, true, true);
    PDRectangle pageSize = document.getPage(0).getMediaBox();
    float pageWidth = pageSize.getWidth();
    float imageWidth = 120;
    float imageHeight = pdImage.getHeight() * (imageWidth / pdImage.getWidth());
    float imageXPosition = pageWidth - MARGIN - imageWidth;
    float imageYPosition = pageSize.getHeight() - MARGIN - imageHeight;

    contentStream.drawImage(pdImage, imageXPosition, imageYPosition, imageWidth, imageHeight);
    contentStream.setFont(getFont(DEFAULT_FONT, pdfDocument), FONT_SIZE);
    contentStream.beginText();
    contentStream.newLineAtOffset(imageXPosition, imageYPosition);
    contentStream.newLineAtOffset(0, -LONG_LINE_BREAK);
    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(I18nKey.PRINTED1.getTranslation(lang), contentStream);
    contentStream.newLineAtOffset(0, -LONG_LINE_BREAK);
    contentStream.setNonStrokingColor(Color.BLACK);
    writeText(I18nKey.PRINTED2.getTranslation(lang), contentStream);
    writeText(getFormattedDate(LocalDate.now()), contentStream);
    contentStream.endText();
    pdfDocument.setYPositionOfTheLastLine(imageYPosition - MARGIN);

    contentStream.close();
  }

  private void addMedicalProblems(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = getFont(BOLD, pdfDocument);
    contentStream.setFont(font, 15);
    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, pdfDocument.getYPositionOfTheLastLine());
    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(I18nKey.RISK_FACTORS.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    updateYposition(15, pdfDocument);
    BaseTable table = createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    addMedicalProblemsTableHeader(table, vaccinationRecordDTO.getLang(), CELL_HEIGHT, pdfDocument);
    updateYposition(CELL_HEIGHT, pdfDocument);
    List<MedicalProblemDTO> medicalProblems = DateComparator.sortByDateDesc(vaccinationRecordDTO.getMedicalProblems(),
        Comparator.comparing(MedicalProblemDTO::getRecordedDate));
    for (MedicalProblemDTO medicalProblem : medicalProblems) {
      addRow(table, medicalProblem, pdfDocument, vaccinationRecordDTO.getLang());
    }
    table.draw();

    updateYposition(2 * CELL_HEIGHT, pdfDocument);
  }

  private void addMedicalProblemsTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    List<String> headers = List.of(I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang),
        I18nKey.BEGIN.getTranslation(lang), I18nKey.END.getTranslation(lang),
        I18nKey.RISK_FACTOR.getTranslation(lang), I18nKey.CLINICAL_STATUS.getTranslation(lang));

    addTableHeader(table, height, headers, MEDICAL_PROBLEMS_TABLE_WIDTHS, pdfDocument);
  }

  private void addNotLegalDocument(String lang, PdfDocument pdfDocument) throws IOException {
    PDDocument pdDocument = pdfDocument.getPdDocument();
    PDPage page = pdDocument.getPage(pdDocument.getNumberOfPages() - 1);
    PDPageContentStream stream =
        new PDPageContentStream(pdDocument, page, PDPageContentStream.AppendMode.APPEND, true, true);
    stream.setFont(getFont(BOLD, pdfDocument), FONT_SIZE);
    stream.setNonStrokingColor(COLOR_DARKBLUE);
    stream.beginText();
    stream.newLineAtOffset(MARGIN, pdfDocument.getYPositionOfTheLastLine());
    writeText(I18nKey.LEGAL_REMARK.getTranslation(lang), stream);
    stream.endText();
    stream.close();
  }

  private void addOtherVaccinations(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = getFont(BOLD, pdfDocument);
    contentStream.setFont(font, FONT_SIZE_TITLE);
    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, pdfDocument.getYPositionOfTheLastLine());

    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(I18nKey.OTHER_VACCINATION.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    updateYposition(LONG_LINE_BREAK, pdfDocument);
    BaseTable table = createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    addVaccinationTableHeader(table, vaccinationRecordDTO.getLang(), CELL_HEIGHT, pdfDocument);
    updateYposition(CELL_HEIGHT, pdfDocument);
    addVaccinationBasedOnTargetDisease(vaccinationRecordDTO, pdfDocument, table,
        pdfOutputConfig.getOtherVaccination().getCodes(), pdfOutputConfig.getOtherVaccination().isKeepEmpty());

    List<String> otherTargetDiseases = vaccinationRecordDTO.getI18nTargetDiseases().stream()
        .map(ValueDTO::getCode)
        .filter(disease -> !pdfOutputConfig.getBasicVaccination().getCodes().contains(disease))
        .filter(disease -> !pdfOutputConfig.getOtherVaccination().getCodes().contains(disease))
        .toList();
    addVaccinationBasedOnTargetDisease(vaccinationRecordDTO, pdfDocument, table, otherTargetDiseases,
        pdfOutputConfig.getOtherVaccination().isKeepEmpty());
    table.draw();
  }

  private void addPageNumbers(PdfDocument pdfDocument, String patientName) throws IOException {
    PDPageTree allPages = pdfDocument.getPdDocument().getDocumentCatalog().getPages();

    for (int i = 1; i <= allPages.getCount(); i++) {
      PDPage page = allPages.get(i - 1);
      PDRectangle pageSize = page.getMediaBox();
      try (PDPageContentStream contentStream =
          new PDPageContentStream(pdfDocument.getPdDocument(), page, PDPageContentStream.AppendMode.APPEND, true,
              true)) {

        // move to buttonline
        contentStream.moveTo(0, 30);
        contentStream.lineTo(pageSize.getWidth(), 30);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.newLineAtOffset(pageSize.getWidth() / 2 - MARGIN, 15);
        PDType0Font font = getFont(DEFAULT_FONT, pdfDocument);
        contentStream.setFont(font, FONT_SIZE - 2.5f);
        writeText(patientName, contentStream);
        contentStream.newLineAtOffset(pageSize.getWidth() / 2 - 12, 0);
        writeText(i + "/" + allPages.getCount(), contentStream);
        contentStream.endText();
        contentStream.close();
      }
    }
  }

  private void addPastIllnessTable(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = getFont(BOLD, pdfDocument);
    contentStream.setFont(font, 15);

    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, pdfDocument.getYPositionOfTheLastLine());
    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(I18nKey.PASTILLNESSES.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    updateYposition(1.5f * 15, pdfDocument);
    BaseTable table = createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    addPastIllnessTableHeader(table, vaccinationRecordDTO.getLang(), CELL_HEIGHT, pdfDocument);
    updateYposition(1.5f * CELL_HEIGHT, pdfDocument);
    List<PastIllnessDTO> dtos = DateComparator.sortByDateDesc(vaccinationRecordDTO.getPastIllnesses(),
        Comparator.comparing(PastIllnessDTO::getRecordedDate));
    if (dtos != null) {
      for (PastIllnessDTO dto : dtos) {
        addRow(table, dto, pdfDocument);
      }
    }

    table.draw();
  }

  private void addPastIllnessTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    List<String> headers = List.of(I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang),
        I18nKey.BEGIN.getTranslation(lang), I18nKey.END.getTranslation(lang), I18nKey.PASTILLNESS.getTranslation(lang));
    addTableHeader(table, height, headers, PASTILLNESS_TABLE_WIDTHS, pdfDocument);
  }

  private void addPatient(HumanNameDTO patient, String lang, PdfDocument pdfDocument) throws IOException {
    PDPage firstPage = pdfDocument.getPdDocument().getPage(0);
    PDRectangle pageSize = firstPage.getMediaBox();

    try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument.getPdDocument(), firstPage,
        PDPageContentStream.AppendMode.APPEND, true, true)) {
      contentStream.beginText();
      contentStream.setFont(getFont(BOLD, pdfDocument), FONT_SIZE);
      contentStream.newLineAtOffset(MARGIN, pageSize.getHeight() - MARGIN); // start 50 units from top-left corner
      addPatientInformation(I18nKey.LASTNAME.getTranslation(lang), patient.getLastName(), contentStream, pdfDocument);
      addPatientInformation(I18nKey.FIRSTNAME.getTranslation(lang), patient.getFirstName(), contentStream, pdfDocument);
      addPatientInformation(I18nKey.BIRTHDAY.getTranslation(lang), getFormattedDate(patient.getBirthday()),
          contentStream, pdfDocument);

      String gender = patient.getGender() != null ? patient.getGender() : I18nKey.UNKNOWN.name();
      gender = I18nKey.exists(gender) ? I18nKey.valueOf(gender).getTranslation(lang)
          : I18nKey.UNKNOWN.getTranslation(lang);
      addPatientInformation(I18nKey.GENDER.getTranslation(lang), gender, contentStream, pdfDocument);
      contentStream.endText();
    }
  }

  private void addPatientInformation(String title, String info, PDPageContentStream contentStream,
      PdfDocument pdfDocument)
      throws IOException {
    contentStream.newLineAtOffset(0, -LONG_LINE_BREAK);
    contentStream.setFont(getFont(BOLD, pdfDocument), FONT_SIZE);
    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(title + ": ", contentStream);
    contentStream.setFont(getFont(DEFAULT_FONT, pdfDocument), FONT_SIZE);
    contentStream.setNonStrokingColor(Color.BLACK);
    writeText(info, contentStream);
  }

  private void addRow(BaseTable table, MedicalProblemDTO dto, PdfDocument pdfDocument, String lang) throws IOException {
    if (dto == null) {
      log.warn("addRow MedicalProblemDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    Row<PDPage> row = table.createRow(15);
    Cell<PDPage> cell = addCell(dto.isValidated() ? "+" : "", MEDICAL_PROBLEMS_TABLE_WIDTHS[0], row,
        pdfDocument);
    cell.setAlign(HorizontalAlignment.CENTER);
    addCell(getFormattedDate(dto.getRecordedDate()), MEDICAL_PROBLEMS_TABLE_WIDTHS[1], row, pdfDocument);
    addCell(getFormattedDate(dto.getBegin()), MEDICAL_PROBLEMS_TABLE_WIDTHS[2], row, pdfDocument);
    addCell(getFormattedDate(dto.getEnd()), MEDICAL_PROBLEMS_TABLE_WIDTHS[3], row, pdfDocument);
    if (dto.getCode() != null) {
      addCell(getTranslation(translationsRequest.get().getMedicalProblemCodeToName(), dto.getCode()), MEDICAL_PROBLEMS_TABLE_WIDTHS[4], row, pdfDocument);
    }
    if (dto.getClinicalStatus() != null) {
      String value = I18nKey.ACTIVE.name().equals(dto.getClinicalStatus().getName().toUpperCase()) ?
          I18nKey.ACTIVE.getTranslation(lang) : I18nKey.INACTIVE.getTranslation(lang);
      addCell(value, MEDICAL_PROBLEMS_TABLE_WIDTHS[5], row, pdfDocument);
    }
    setRowColor(row, pdfDocument.getRowBackgoundColor());
    swapColor(pdfDocument);
    updateYposition(row.getHeight(), pdfDocument);
  }

  private void addRow(BaseTable table, PastIllnessDTO dto, PdfDocument pdfDocument) throws IOException {
    if (dto == null) {
      log.warn("addRow PastIllnessDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    Row<PDPage> row = table.createRow(15);
    Cell<PDPage> cell = addCell(dto.isValidated() ? "+" : "", PASTILLNESS_TABLE_WIDTHS[0], row,
        pdfDocument);
    cell.setAlign(HorizontalAlignment.CENTER);
    addCell(getFormattedDate(dto.getRecordedDate()), PASTILLNESS_TABLE_WIDTHS[1], row, pdfDocument);
    addCell(getFormattedDate(dto.getBegin()), PASTILLNESS_TABLE_WIDTHS[2], row, pdfDocument);
    addCell(getFormattedDate(dto.getEnd()), PASTILLNESS_TABLE_WIDTHS[3], row, pdfDocument);
    if (dto.getCode() != null) {
      addCell(getTranslation(translationsRequest.get().getIllnessCodeToName(), dto.getCode()), PASTILLNESS_TABLE_WIDTHS[4], row, pdfDocument);
    }
    updateYposition(row.getHeight(), pdfDocument);
    setRowColor(row, pdfDocument.getRowBackgoundColor());
    swapColor(pdfDocument);
  }

  private void addSeparationLine(PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    PDPageContentStream contentStreamLine =
        new PDPageContentStream(document, document.getPage(document.getNumberOfPages() - 1),
            PDPageContentStream.AppendMode.APPEND, true, true);
    float yPositionOftheLastLine = pdfDocument.getYPositionOfTheLastLine();
    contentStreamLine.moveTo(MARGIN, yPositionOftheLastLine - 35); // Starting point (x, y)
    contentStreamLine.lineTo(PDRectangle.A4.getWidth() - MARGIN, yPositionOftheLastLine - 35); // Ending point (x, y)
    pdfDocument.setYPositionOfTheLastLine(yPositionOftheLastLine - 70);
    contentStreamLine.stroke();
    contentStreamLine.close();
  }

  private void addTableHeader(BaseTable table, float height, List<String> headers, float[] widths,
      PdfDocument pdfDocument) throws IOException {
    Row<PDPage> headerRow = table.createRow(height);
    for (int i = 0; i < headers.size(); i++) {
      Cell<PDPage> cell = addCell(headers.get(i), widths[i], headerRow, pdfDocument);
      cell.setTextColor(COLOR_WHITE);
      cell.setFillColor(COLOR_DARKBLUE);
    }
  }

  private void addVaccinationBasedOnTargetDisease(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument,
      BaseTable table, List<String> codes, boolean keepEmptyEntries)
      throws IOException {
    for (String code : codes) {
      String targetDiseaseName = getDiseaseName(vaccinationRecordDTO, code);
      List<VaccinationDTO> vaccinations = extract(vaccinationRecordDTO.getVaccinations(), code);
      addVaccinRows(table, targetDiseaseName, vaccinations, keepEmptyEntries, pdfDocument);
    }
  }

  private void addVaccinationRecordTitle(String lang, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    float yPositionOftheLastLine = pdfDocument.getYPositionOfTheLastLine();
    PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(0),
        PDPageContentStream.AppendMode.APPEND, true, true);
    PDType0Font font = getFont(BOLD, pdfDocument);
    contentStream.setFont(font, 25);
    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, yPositionOftheLastLine - 10);
    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(I18nKey.VACCINATION_RECORD.getTranslation(lang), contentStream);

    contentStream.setFont(font, 15);
    contentStream.newLineAtOffset(0, -50);
    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(I18nKey.BASIC_VACCINATION.getTranslation(lang), contentStream);
    contentStream.endText();
    contentStream.close();
    addSeparationLine(pdfDocument);
  }

  private void addVaccinationTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    java.util.List<String> headers = java.util.List.of(I18nKey.DISEASE.getTranslation(lang),
        I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang), I18nKey.VACCINE.getTranslation(lang),
        I18nKey.TREATING.getTranslation(lang));

    addTableHeader(table, height, headers, VACCINE_TABLE_WIDTHS, pdfDocument);
  }

  private void addVaccinRows(BaseTable table, String targetDisease, List<VaccinationDTO> dtos, boolean keepEmpty,
      PdfDocument pdfDocument) throws IOException {
    log.debug("addRows targetDisease:{} {}", targetDisease, dtos.size());
    if (!keepEmpty && dtos.isEmpty()) {
      return;
    }

    Row<PDPage> row = table.createRow(CELL_HEIGHT);
    var spanncell = addCell(targetDisease, VACCINE_TABLE_WIDTHS[0], row, pdfDocument);
    if (!dtos.isEmpty()) {
      boolean hasMultipleEntries = dtos.size() > 1;
      if (hasMultipleEntries) {
        // keep 1st row without lines
        spanncell.setBottomBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
      }

      // for each row, produce validated | date | vaccination | author and empty cell for next row
      for (int i = 0; i < dtos.size(); i++) {
        VaccinationDTO dto = dtos.get(i);

        Cell<PDPage> validatedCell = addCell(dto.isValidated() ? "+" : "", VACCINE_TABLE_WIDTHS[1], row, pdfDocument);
        validatedCell.setAlign(HorizontalAlignment.CENTER);

        addCell(getFormattedDate(dto.getOccurrenceDate()), VACCINE_TABLE_WIDTHS[2], row, pdfDocument);
        addCell(getTranslation(translationsRequest.get().getVaccineCodeToName(), dto.getCode()), VACCINE_TABLE_WIDTHS[3], row, pdfDocument);

        String recorderOrOrganization = dto.getRecorder() != null &&
            !dto.getRecorder().getFullName().isBlank() ? dto.getRecorder().getFullName() : dto.getOrganization();
        addCell(recorderOrOrganization, VACCINE_TABLE_WIDTHS[4], row, pdfDocument);
        setRowColor(row, pdfDocument.getRowBackgoundColor());

        boolean nonLastItems = i < dtos.size() - 1;
        boolean lastItem = i == dtos.size() - 1;
        // Special case when the last item of a specific disease is the first item on a new page
        if (lastItem) {
          spanncell.setTopBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
        }
        // Will create new row with first cell empty cell
        if (nonLastItems) {
          spanncell.setTopBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
          updateYposition(CELL_HEIGHT, pdfDocument);
          row = table.createRow(CELL_HEIGHT);
          spanncell = addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[0], row, pdfDocument);
          if (i < dtos.size() - 2) {
            spanncell.setBottomBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
          }
        }
      }
    } else {
      addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[1], row, pdfDocument);
      addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[2], row, pdfDocument);
      addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[3], row, pdfDocument);
      addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[4], row, pdfDocument);
      setRowColor(row, pdfDocument.getRowBackgoundColor());
    }
    updateYposition(CELL_HEIGHT, pdfDocument);
    swapColor(pdfDocument);
  }

  private PDDocument createDocumentAndItsMeta() throws Exception {
    PDDocument document = new PDDocument();
    PDPage page = new PDPage(PDRectangle.A4);
    document.addPage(page);

    PDMetadata metadata = new PDMetadata(document);
    page.setMetadata(metadata);

    XMPMetadata xmp = XMPMetadata.createXMPMetadata();
    PDFAIdentificationSchema pdfaid = xmp.createAndAddPDFAIdentificationSchema();
    pdfaid.setPart(1);
    pdfaid.setConformance("A");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new XmpSerializer().serialize(xmp, baos, true);
    metadata.importXMPMetadata(baos.toByteArray());

    PDDocumentCatalog catalog = document.getDocumentCatalog();
    PDMarkInfo markInfo = catalog.getMarkInfo();
    if (markInfo == null) {
      markInfo = new PDMarkInfo();
      catalog.setMarkInfo(markInfo);
    }

    PDStructureTreeRoot treeRoot = new PDStructureTreeRoot();
    catalog.setStructureTreeRoot(treeRoot);
    catalog.getMarkInfo().setMarked(true);
    document.getDocumentCatalog().setMetadata(metadata);

    InputStream colorProfile = new ClassPathResource(ICC_PROFILE).getInputStream();
    PDOutputIntent oi = new PDOutputIntent(document, colorProfile);
    oi.setInfo("sRGB IEC61966-2.1");
    oi.setOutputCondition("sRGB IEC61966-2.1");
    oi.setOutputConditionIdentifier("sRGB IEC61966-2.1");
    oi.setRegistryName("http://www.color.org");
    document.getDocumentCatalog().addOutputIntent(oi);

    return document;
  }

  private PDPageContentStream createNewPageContentStream(PdfDocument pdfDocument, PDDocument document)
      throws IOException {
    PDPage newPage = new PDPage((PDRectangle.A4));
    pdfDocument.setYPositionOfTheLastLine(newPage.getMediaBox().getHeight() - MARGIN);
    document.addPage(newPage);
    return new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
  }

  private BaseTable createTable(float yStart, int pageNumber, PDDocument document) throws IOException {
    PDPage page = document.getPage(pageNumber);
    float tableWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
    return new BaseTable(yStart, document.getPage(0).getMediaBox().getHeight() - MARGIN,
        MARGIN, tableWidth, MARGIN, document, page, true, true);
  }

  private List<VaccinationDTO> extract(List<VaccinationDTO> inputs, String code) {
    return DateComparator.sortByDateDesc(inputs,
        Comparator.comparing(VaccinationDTO::getOccurrenceDate))
        .stream()
        .filter(dto -> dto.getTargetDiseases().stream().anyMatch(td -> td.getCode().equals(code)))
        .toList();
  }

  private void fillDocumentContent(VaccinationRecordDTO record, PdfDocument pdfDocument) throws Exception {
    addPatient(record.getPatient(), record.getLang(), pdfDocument);
    addLogo(record.getLang(), pdfDocument);
    addVaccinationRecordTitle(record.getLang(), pdfDocument);
    addBaseVaccinations(createTable(pdfDocument.getYPositionOfTheLastLine(), 0, pdfDocument.getPdDocument()),
        record, pdfDocument);
    addOtherVaccinations(record, pdfDocument);
    addAdverseEvents(record, pdfDocument);
    addPastIllnessTable(record, pdfDocument);
    addMedicalProblems(record, pdfDocument);
    if (pdfOutputConfig.isLegalCommentVisible()) {
      addSeparationLine(pdfDocument);
      addNotLegalDocument(record.getLang(), pdfDocument);
    }
  }

  private String getDiseaseName(VaccinationRecordDTO vaccinationRecordDTO, String code) {
    for (ValueDTO valueDTO : vaccinationRecordDTO.getI18nTargetDiseases()) {
      if (code.equals(valueDTO.getCode())) {
        return valueDTO.getName();
      }
    }
    return code;
  }

  private PDType0Font getFont(String fontname, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    return switch (fontname) {
      case BOLD -> {
        if (pdfDocument.getBold() == null) {
          pdfDocument.setBold(getPDType0Font(document, FONT_BOLD));
        }
        yield pdfDocument.getBold();
      }
      case OBLIQUE -> {
        if (pdfDocument.getOblique() == null) {
          pdfDocument.setOblique(getPDType0Font(document, FONT_OBLIQUE));
        }
        yield pdfDocument.getOblique();
      }
      default -> {
        if (pdfDocument.getNormal() == null) {
          pdfDocument.setNormal(getPDType0Font(document, FONT));
        }
        yield pdfDocument.getNormal();
      }
    };
  }

  private String getFormattedDate(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    return date != null ? date.format(formatter) : "-";
  }

  private String getFormattedDateTime(LocalDateTime dateTime) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm");
    return dateTime != null ? dateTime.format(formatter) : "-";
  }

  private PDType0Font getPDType0Font(PDDocument document, String fontPath) throws IOException {
    return PDType0Font.load(document, new ClassPathResource(fontPath).getInputStream());
  }

  private StandardProtectionPolicy getStandardProtectionPolicy() {
    int keyLength = 256;

    AccessPermission ap = new AccessPermission();

    // Disable all
    ap.setCanModifyAnnotations(false);
    ap.setCanAssembleDocument(false);
    ap.setCanFillInForm(false);
    ap.setCanModify(false);
    ap.setCanExtractContent(false);
    ap.setCanExtractForAccessibility(false);

    // The user password is empty ("") so user can read without password. The admin password is
    // set to lock/encrypt the document.
    StandardProtectionPolicy spp = new StandardProtectionPolicy(UUID.randomUUID().toString(), EMPTY_USER_PASSWORD, ap);
    spp.setEncryptionKeyLength(keyLength);
    spp.setPermissions(ap);
    return spp;
  }

  private String getTranslation(Map<String, String> valuesCodeToName, ValueDTO valueDTO) {
    return valuesCodeToName.getOrDefault(valueDTO.getCode(), valueDTO.getName());
  }

  private InputStream setReadOnlyPermissions(byte[] pdfFile) throws IOException {
    try (PDDocument doc = PDDocument.load(pdfFile);
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      doc.protect(getStandardProtectionPolicy());
      doc.save(bos);

      return new ByteArrayInputStream(bos.toByteArray());
    } catch (Exception exception) {
      log.error("Error while adding read-only protection to PDF {}", exception.getMessage());
      throw exception;
    }
  }

  private void setRowColor(Row<PDPage> row, Color rowBackgoundColor) {
    for (Cell<PDPage> cell : row.getCells()) {
      cell.setFillColor(rowBackgoundColor);
    }
  }

  private void swapColor(PdfDocument pdfDocument) {
    if (COLOR_WHITE.equals(pdfDocument.getRowBackgoundColor())) {
      pdfDocument.setRowBackgoundColor(COLOR_LIGHTBLUE);
    } else {
      pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    }
  }


  private void updateYposition(float delta, PdfDocument pdfDocument) {
    float yPositionOfTheLastLine = pdfDocument.getYPositionOfTheLastLine();
    boolean isNoNewPageNecessary = yPositionOfTheLastLine - delta > MARGIN;
    if (isNoNewPageNecessary) {
      pdfDocument.setYPositionOfTheLastLine(yPositionOfTheLastLine - delta);
    } else {
      pdfDocument.setYPositionOfTheLastLine(PDRectangle.A4.getHeight() - MARGIN);
    }
  }

  private void validatePdf(InputStream inputStream) throws Exception {
    VeraGreenfieldFoundryProvider.initialise();
    PDFAFlavour flavour = PDFAFlavour.fromString("1a");
    PDFAParser parser = Foundries.defaultInstance().createParser(inputStream, flavour);
    PDFAValidator validator = Foundries.defaultInstance().createValidator(flavour, false);
    ValidationResult result = validator.validate(parser);

    if (!result.isCompliant()) {
      throw new BusinessException("Generated pdf is not PDF/1A compatible.");
    }
  }

  private void writeText(String value, PDPageContentStream contentStream) throws IOException {
    if (value != null) {
      contentStream.showText(value);
    }
  }
}
