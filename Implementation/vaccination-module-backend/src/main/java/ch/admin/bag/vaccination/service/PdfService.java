/**
 * Copyright (c) 2022 eHealth Suisse
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
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
import ch.admin.bag.vaccination.exception.BusinessException;
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
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
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
  private static final float[] VACCIN_TABLE_WITHS = {30, 10, 15, 20, 25};
  private static final float[] MEDICAL_PROBLEMS_TABLE_WITHS = {10, 15, 15, 15, 30, 15};
  private static final float[] PASTILNESS_TABLE_WITHS = {10, 15, 15, 15, 45};
  private static final float MARGIN = 50;
  private static final float CELL_HEIGHT = 15;
  private static final float LONG_LINE_BREAK = 20;
  private static final float SHORT_LINE_BREAK = 15;
  private static final float FONT_SIZE = 10;
  private static final float FONT_SIZE_TITLE = 15;
  private static final String BOLD = "bold";
  private static final String OBLIQUE = "oblique";

  @Autowired
  private PdfOutputConfig pdfOutputConfig;

  public InputStream create(VaccinationRecordDTO vaccinationRecordDTO) {
    try {
      PDDocument document = createDocumentAndItsMeta();
      PdfDocument pdfDocument = new PdfDocument(document);
      fillDocumentContent(vaccinationRecordDTO, pdfDocument);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      document.save(out);
      document.close();

      byte[] pdf = out.toByteArray();
      ByteArrayInputStream validateIn = new ByteArrayInputStream(pdf);
      validatePdf(validateIn);
      validateIn.close();

      return new ByteArrayInputStream(pdf);
    } catch (Exception ex) {
      log.error("error while exportToPDF {}", ex.getMessage());
      throw new TechnicalException("Pdf generation failed", ex);
    }
  }

  private void addAdverseEvents(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    PDPage page = document.getPage(document.getNumberOfPages() - 1);
    PDPageContentStream contentStream =
        new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
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
    List<AllergyDTO> dtos = vaccinationRecordDTO.getAllergies();
    log.debug("create {} allergies", dtos == null ? 0 : dtos.size());
    if (dtos != null) {
      for (AllergyDTO dto : dtos) {
        addRow(dto, pdfDocument);
      }
    }
  }

  private void addAdverseEventsCommentText(CommentDTO commentDTO, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    if (pdfDocument.getYPositionOfTheLastLine() == PDRectangle.A4.getHeight() - MARGIN) {
      document.addPage(new PDPage(PDRectangle.A4));
    }

    PDPageContentStream contentStream = new PDPageContentStream(document,
        document.getPage(document.getNumberOfPages() - 1), PDPageContentStream.AppendMode.APPEND, true, true);
    PDType0Font font = getFont("", pdfDocument);
    contentStream.setFont(font, FONT_SIZE);
    contentStream.beginText();
    contentStream.newLineAtOffset(50, pdfDocument.getYPositionOfTheLastLine());
    writeText(getFormattedDateTime(commentDTO.getDate()) + ", " + commentDTO.getAuthor(), contentStream);
    updateYposition(SHORT_LINE_BREAK, pdfDocument);
    contentStream.newLineAtOffset(0, -SHORT_LINE_BREAK);
    writeText(commentDTO.getText().replace("\n", " "), contentStream);
    updateYposition(SHORT_LINE_BREAK, pdfDocument);
    contentStream.endText();
    contentStream.close();

  }

  private void addAdverseEventsText(AllergyDTO dto, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    if (pdfDocument.getYPositionOfTheLastLine() == PDRectangle.A4.getHeight() - MARGIN) {
      document.addPage(new PDPage(PDRectangle.A4));
    }

    PDPageContentStream contentStream = new PDPageContentStream(document,
        document.getPage(document.getNumberOfPages() - 1), PDPageContentStream.AppendMode.APPEND, true, true);
    PDType0Font font = getFont(BOLD, pdfDocument);
    contentStream.setFont(font, FONT_SIZE);
    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, pdfDocument.getYPositionOfTheLastLine());
    if (dto.isValidated()) {
      writeText("(+) ", contentStream);
    }
    writeText(dto.getCode().getName(), contentStream);
    contentStream.setFont(getFont("", pdfDocument), 10);
    writeText(", " + getFormattedDate(dto.getOccurrenceDate()) + ", " + dto.getAuthor().getUser().getFullName(),
        contentStream);
    updateYposition(LONG_LINE_BREAK, pdfDocument);
    contentStream.endText();
    contentStream.close();
  }

  private void addBaseVaccinations(BaseTable table, VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    addVaccinationTableHeader(table, vaccinationRecordDTO.getLang(), CELL_HEIGHT, pdfDocument);
    for (String code : pdfOutputConfig.getBasicVaccination().getCodes()) {
      String targetDiseaseName = getDiseaseName(vaccinationRecordDTO, code);
      List<VaccinationDTO> vaccinations = extract(vaccinationRecordDTO.getVaccinations(), code);
      addVaccinRows(table, targetDiseaseName, vaccinations, pdfOutputConfig.getBasicVaccination().isKeepEmpty(),
          pdfDocument);
    }

    table.draw();
  }

  private Cell<PDPage> addCell(String content, float width, Row<PDPage> headerRow, PdfDocument pdfDocument) throws IOException {
    Cell<PDPage> cell = headerRow.createCell(width, content);
    cell.setFont(getFont("", pdfDocument));
    return cell;
  }

  private void addLogo(String lang, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document,
        PdfService.class.getResourceAsStream("/EPDLogo.jpg").readAllBytes(), "EPLogo");
    PDPageContentStream contentStream =
        new PDPageContentStream(document, document.getPage(0), PDPageContentStream.AppendMode.APPEND, true, true);
    PDRectangle pageSize = document.getPage(0).getMediaBox();
    float pageWidth = pageSize.getWidth();
    float imageWidth = 120;
    float imageHeight = pdImage.getHeight() * (imageWidth / pdImage.getWidth());
    float imageXPosition = pageWidth - MARGIN - imageWidth;
    float imageYPosition = pageSize.getHeight() - MARGIN - imageHeight;

    contentStream.drawImage(pdImage, imageXPosition, imageYPosition, imageWidth, imageHeight);
    contentStream.setFont(getFont("", pdfDocument), FONT_SIZE);
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

  private void addMedicalProblems(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    updateYposition(MARGIN, pdfDocument);
    PDPage page = document.getPage(document.getNumberOfPages() - 1);
    PDPageContentStream contentStream =
        new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
    PDType0Font font = getFont(BOLD, pdfDocument);
    contentStream.setFont(font, 15);
    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, pdfDocument.getYPositionOfTheLastLine());
    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(I18nKey.MEDICAL_PROBLEM.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    updateYposition(15, pdfDocument);
    BaseTable table = createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document, vaccinationRecordDTO.getLang());
    addMedicalProblemsTableHeader(table, vaccinationRecordDTO.getLang(), CELL_HEIGHT, pdfDocument);
    updateYposition(CELL_HEIGHT, pdfDocument);
    for (MedicalProblemDTO medicalProblem : vaccinationRecordDTO.getMedicalProblems()) {
      addRow(table, medicalProblem, pdfDocument);
    }
    table.draw();
  }

  private void addMedicalProblemsTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    List<String> headers = List.of(I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang),
        I18nKey.BEGIN.getTranslation(lang), I18nKey.END.getTranslation(lang),
        I18nKey.MEDICAL_PROBLEM.getTranslation(lang), I18nKey.CLINICAL_STATUS.getTranslation(lang));

    addTableHeader(table, height, headers, MEDICAL_PROBLEMS_TABLE_WITHS, pdfDocument);
  }

  private void addOtherVaccinations(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    PDPage newPage = new PDPage((PDRectangle.A4));
    pdfDocument.setYPositionOfTheLastLine(newPage.getMediaBox().getHeight() - MARGIN);
    document.addPage(newPage);
    PDPageContentStream contentStream =
        new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
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
        document, vaccinationRecordDTO.getLang());
    addVaccinationTableHeader(table, vaccinationRecordDTO.getLang(), CELL_HEIGHT, pdfDocument);
    updateYposition(CELL_HEIGHT, pdfDocument);
    for (String code : pdfOutputConfig.getOtherVaccination().getCodes()) {
      String targetDiseaseName = getDiseaseName(vaccinationRecordDTO, code);
      List<VaccinationDTO> vaccinations = extract(vaccinationRecordDTO.getVaccinations(), code);
      addVaccinRows(table, targetDiseaseName, vaccinations, pdfOutputConfig.getOtherVaccination().isKeepEmpty(),
          pdfDocument);
    }

    table.draw();
  }

  private void addPastIllnessTable(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    updateYposition(MARGIN, pdfDocument);
    // checkNewPageNeeded();
    PDPage page = document.getPage(document.getNumberOfPages() - 1);
    PDPageContentStream contentStream =
        new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
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
        document, vaccinationRecordDTO.getLang());
    addPastIllnessTableHeader(table, vaccinationRecordDTO.getLang(), CELL_HEIGHT, pdfDocument);
    updateYposition(1.5f * CELL_HEIGHT, pdfDocument);
    List<PastIllnessDTO> dtos = vaccinationRecordDTO.getPastIllnesses();
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
    addTableHeader(table, height, headers, PASTILNESS_TABLE_WITHS, pdfDocument);
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
      addPatientInformation(I18nKey.GENDER.getTranslation(lang), patient.getGender(), contentStream, pdfDocument);
      contentStream.endText();
    }
  }

  private void addPatientInformation(String title, String info, PDPageContentStream contentStream, PdfDocument pdfDocument)
      throws IOException {
    contentStream.newLineAtOffset(0, -LONG_LINE_BREAK);
    contentStream.setFont(getFont(BOLD, pdfDocument), FONT_SIZE);
    contentStream.setNonStrokingColor(COLOR_DARKBLUE);
    writeText(title + ": ", contentStream);
    contentStream.setFont(getFont("", pdfDocument), FONT_SIZE);
    contentStream.setNonStrokingColor(Color.BLACK);
    writeText(info, contentStream);
  }

  private void addRow(BaseTable table, MedicalProblemDTO dto, PdfDocument pdfDocument) throws IOException {
    if (dto == null) {
      log.warn("addRow MedicalProblemDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    Row<PDPage> row = table.createRow(15);
    Cell<PDPage> cell = addCell(dto.isValidated() ? "+" : "", MEDICAL_PROBLEMS_TABLE_WITHS[0], row, pdfDocument);
    cell.setAlign(HorizontalAlignment.CENTER);
    addCell(getFormattedDate(dto.getRecordedDate()), MEDICAL_PROBLEMS_TABLE_WITHS[1], row, pdfDocument);
    addCell(getFormattedDate(dto.getBegin()), MEDICAL_PROBLEMS_TABLE_WITHS[2], row, pdfDocument);
    addCell(getFormattedDate(dto.getEnd()), MEDICAL_PROBLEMS_TABLE_WITHS[3], row, pdfDocument);
    if (dto.getCode() != null) {
      addCell(dto.getCode().getName(), MEDICAL_PROBLEMS_TABLE_WITHS[4], row, pdfDocument);
    }
    if (dto.getClinicalStatus() != null) {
      addCell(dto.getClinicalStatus().getName(), MEDICAL_PROBLEMS_TABLE_WITHS[5], row, pdfDocument);
    }
    setRowColor(row, pdfDocument.getRowBackgoundColor());
    swapColor(pdfDocument);
  }

  private void addRow(BaseTable table, PastIllnessDTO dto, PdfDocument pdfDocument) throws IOException {
    if (dto == null) {
      log.warn("addRow PastIllnessDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    Row<PDPage> row = table.createRow(15);
    Cell<PDPage> cell = addCell(dto.isValidated() ? "+" : "", PASTILNESS_TABLE_WITHS[0], row, pdfDocument);
    cell.setAlign(HorizontalAlignment.CENTER);
    addCell(getFormattedDate(dto.getRecordedDate()), PASTILNESS_TABLE_WITHS[1], row, pdfDocument);
    addCell(getFormattedDate(dto.getBegin()), PASTILNESS_TABLE_WITHS[2], row, pdfDocument);
    addCell(getFormattedDate(dto.getEnd()), PASTILNESS_TABLE_WITHS[3], row, pdfDocument);
    if (dto.getCode() != null) {
      addCell(dto.getCode().getName(), PASTILNESS_TABLE_WITHS[4], row, pdfDocument);
    }
    updateYposition(row.getHeight(), pdfDocument);
    setRowColor(row, pdfDocument.getRowBackgoundColor());
    swapColor(pdfDocument);
  }

  private void addRow(AllergyDTO dto, PdfDocument fontType) throws IOException {
    if (dto == null) {
      log.warn("addRow AllergyDTO null");
      return;
    }
    log.debug("addRow {}", dto.getCode().getName());
    addAdverseEventsText(dto, fontType);
    for (CommentDTO commentDTO : dto.getComments()) {
      addAdverseEventsCommentText(commentDTO, fontType);
    }
  }

  private void addSeparationLine(String lang, PdfDocument pdfDocument) throws IOException {
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
    PDPageContentStream contentStreamLine = new PDPageContentStream(document, document.getPage(0),
        PDPageContentStream.AppendMode.APPEND, true, true);
    contentStreamLine.moveTo(MARGIN, yPositionOftheLastLine - 35); // Starting point (x, y)
    contentStreamLine.lineTo(PDRectangle.A4.getWidth() - MARGIN, yPositionOftheLastLine - 35); // Ending point (x, y)
    pdfDocument.setYPositionOfTheLastLine(yPositionOftheLastLine - 70);
    contentStreamLine.stroke();
    contentStreamLine.close();


  }


  private void addTableHeader(BaseTable table, float height, List<String> headers, float[] widths, PdfDocument pdfDocument)
      throws IOException {
    Row<PDPage> headerRow = table.createRow(height);
    for (int i = 0; i < headers.size(); i++) {
      Cell<PDPage> cell = addCell(headers.get(i), widths[i], headerRow, pdfDocument);
      cell.setTextColor(COLOR_WHITE);
      cell.setFillColor(COLOR_DARKBLUE);
    }
  }

  private void addVaccinationTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    java.util.List<String> headers = java.util.List.of(I18nKey.DISEASE.getTranslation(lang),
        I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang), I18nKey.VACCINE.getTranslation(lang),
        I18nKey.TREATING.getTranslation(lang));

    addTableHeader(table, height, headers, VACCIN_TABLE_WITHS, pdfDocument);
  }

  private void addVaccinRows(BaseTable table, String targetDisease, List<VaccinationDTO> dtos, boolean keepEmpty,
      PdfDocument pdfDocument) throws IOException {
    log.debug("addRows targetDisease:{} {}", targetDisease, dtos.size());

    if (!keepEmpty && dtos.isEmpty()) {
      return;
    }
    Row<PDPage> row = table.createRow(CELL_HEIGHT);
    var spanncell = addCell(targetDisease, VACCIN_TABLE_WITHS[0], row, pdfDocument);

    if (dtos != null && !dtos.isEmpty()) {
      if (dtos.size() > 1) {
        spanncell.setBottomBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
      }

      for (int i = 0; i < dtos.size(); i++) {
        VaccinationDTO dto = dtos.get(i);

        Cell<PDPage> cell = addCell(dto.isValidated() ? "+" : "", VACCIN_TABLE_WITHS[1], row, pdfDocument);
        cell.setAlign(HorizontalAlignment.CENTER);

        addCell(getFormattedDate(dto.getOccurrenceDate()), VACCIN_TABLE_WITHS[2], row, pdfDocument);
        if (dto.getCode() != null) {
          addCell(dto.getCode().getName(), VACCIN_TABLE_WITHS[3], row, pdfDocument);
        }
        if (dto.getRecorder() != null) {
          addCell(dto.getRecorder().getPrefix() + " " + dto.getRecorder().getFirstName() + " "
              + dto.getRecorder().getLastName(), VACCIN_TABLE_WITHS[4], row, pdfDocument);
        }
        setRowColor(row, pdfDocument.getRowBackgoundColor());

        if (i < dtos.size() - 1) {
          updateYposition(row.getHeight(), pdfDocument);
          row = table.createRow(CELL_HEIGHT);
          spanncell = addCell("", VACCIN_TABLE_WITHS[0], row, pdfDocument);
          if (i < dtos.size() - 2 && pdfDocument.getYPositionOfTheLastLine() >= 180) {
            spanncell.setBottomBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
          }
        }

      }
    } else {
      addCell("", VACCIN_TABLE_WITHS[1], row, pdfDocument);
      addCell("", VACCIN_TABLE_WITHS[2], row, pdfDocument);
      addCell("", VACCIN_TABLE_WITHS[3], row, pdfDocument);
      addCell("", VACCIN_TABLE_WITHS[4], row, pdfDocument);
      setRowColor(row, pdfDocument.getRowBackgoundColor());
    }
    updateYposition(row.getHeight(), pdfDocument);
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

  private BaseTable createTable(float yStart, int pageNumber, PDDocument document, String lang) throws IOException {
    PDPage page = document.getPage(pageNumber);
    float tableWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
    return new BaseTable(yStart, document.getPage(0).getMediaBox().getHeight() - MARGIN,
        MARGIN, tableWidth, MARGIN, document, page, true, true);
  }

  private List<VaccinationDTO> extract(List<VaccinationDTO> inputs, String code) {
    List<VaccinationDTO> vaccinations = new ArrayList<>();
    for (VaccinationDTO input : inputs) {
      for (ValueDTO targetDisease : input.getTargetDiseases()) {
        if (targetDisease.getCode().equals(code)) {
          vaccinations.add(input);
        }
      }
    }
    return vaccinations;
  }

  private void fillDocumentContent(VaccinationRecordDTO record, PdfDocument pdfDocument) throws Exception {
    addPatient(record.getPatient(), record.getLang(), pdfDocument);
    addLogo(record.getLang(), pdfDocument);
    addSeparationLine(record.getLang(), pdfDocument);
    addBaseVaccinations(createTable(pdfDocument.getYPositionOfTheLastLine(), 0, pdfDocument.getPdDocument(),
            record.getLang()), record, pdfDocument);
    addOtherVaccinations(record, pdfDocument);
    addAdverseEvents(record, pdfDocument);
    addPastIllnessTable(record, pdfDocument);
    addMedicalProblems(record, pdfDocument);
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

  private PDType0Font getPDType0Font(PDDocument document, String fontPath) throws IOException {
    return PDType0Font.load(document, new ClassPathResource(fontPath).getInputStream());
  }

  private String getFormattedDate(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    return date != null ? date.format(formatter) : "-";
  }

  private String getFormattedDateTime(LocalDateTime dateTime) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm");
    return dateTime != null ? dateTime.format(formatter) : "-";
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
    boolean isNewPageNecessary = yPositionOfTheLastLine - delta > MARGIN;
    if (isNewPageNecessary) {
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
