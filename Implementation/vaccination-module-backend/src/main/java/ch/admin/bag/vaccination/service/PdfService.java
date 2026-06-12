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
import ch.admin.bag.vaccination.data.dto.PdfExportOptionsDTO;
import ch.admin.bag.vaccination.data.request.PdfExportRequest;
import ch.admin.bag.vaccination.exception.BusinessException;
import ch.admin.bag.vaccination.service.config.PdfOutputConfig;
import ch.admin.bag.vaccination.utils.DateComparator;
import ch.admin.bag.vaccination.utils.PdfServiceUtils;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BasicImmunizationDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.LaboratorySerologyDTO;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
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
  private static final String ICC_PROFILE = "font/sRGB_CS_profile.icm";

  private static final float[] ADVERSE_EVENTS_TABLE_WIDTHS = {10, 15, 45, 30};
  private static final float[] VACCINE_TABLE_WIDTHS = {30, 10, 15, 20, 25};
  private static final float[] MEDICAL_PROBLEMS_TABLE_WIDTHS = {10, 15, 15, 15, 30, 15};
  private static final float[] PASTILLNESS_TABLE_WIDTHS = {10, 15, 15, 15, 45};
  private static final float[] BASIC_IMMUNIZATION_TABLE_WIDTHS = {10, 20, 70};
  private static final float[] LABORATORY_SEROLOGY_TABLE_WIDTHS = {10, 15, 35, 20, 20};

  private static final String EMPTY_CONTENT = "";
  private static final String EMPTY_USER_PASSWORD = "";
  private static final String VACCINE_UNKNOWN_CODE = "787859002";

  private final ThreadLocal<PdfExportRequest> translationsRequest = new ThreadLocal<>();

  @Autowired
  private PdfOutputConfig pdfOutputConfig;

  public InputStream create(VaccinationRecordDTO vaccinationRecordDTO, PdfExportRequest translationsRequest) {
    this.translationsRequest.set(translationsRequest);
    try (PDDocument document = createDocumentAndItsMeta();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PdfDocument pdfDocument = new PdfDocument(document);
      fillDocumentContent(vaccinationRecordDTO, pdfDocument, translationsRequest.getPdfOptions());

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
    pdfDocument.setRowBackgoundColor(PdfServiceUtils.COLOR_WHITE);
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument);
    contentStream.setFont(font, PdfServiceUtils.FONT_SIZE_TITLE);
    contentStream.beginText();
    contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, pdfDocument.getYPositionOfTheLastLine());

    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(I18nKey.ADVERSE_EVENTS.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    PdfServiceUtils.updateYposition(PdfServiceUtils.LONG_LINE_BREAK, pdfDocument);
    BaseTable table = PdfServiceUtils.createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    addAdverseEventsTableHeader(table, vaccinationRecordDTO.getLang(), PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    PdfServiceUtils.updateYposition(1.5f * PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    List<AllergyDTO> dtos = DateComparator.sortByDateDesc(vaccinationRecordDTO.getAllergies(),
        Comparator.comparing(AllergyDTO::getOccurrenceDate));
    log.debug("create {} allergies", dtos == null ? 0 : dtos.size());
    if (dtos != null) {
      for (AllergyDTO dto : dtos) {
        addAdverseEventRow(table, dto, pdfDocument);
      }
    }
    table.draw();
  }

  private void addAdverseEventsTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    List<String> headers = List.of(I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang),
        I18nKey.ADVERSE_EVENT.getTranslation(lang), I18nKey.TREATING.getTranslation(lang));

    PdfServiceUtils.addTableHeader(table, height, headers, ADVERSE_EVENTS_TABLE_WIDTHS, pdfDocument);
  }

  private void addAdverseEventComments(VaccinationRecordDTO record, PdfDocument pdfDocument) throws IOException {
    PdfServiceUtils.addCommentsSection(record.getAllergies(), record, pdfDocument,
        I18nKey.COMMENTS_FOR_ADVERSE_EVENTS.getTranslation(record.getLang()), AllergyDTO::getOccurrenceDate,
        dto -> getTranslation(translationsRequest.get().getAllergyCodeToName(), dto.getCode()));
  }

  private void addBaseVaccinations(BaseTable table, VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    pdfDocument.setRowBackgoundColor(PdfServiceUtils.COLOR_WHITE);
    addVaccinationTableHeader(table, vaccinationRecordDTO.getLang(), PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    addVaccinationBasedOnTargetDisease(vaccinationRecordDTO, pdfDocument, table,
        pdfOutputConfig.getBasicVaccination().getCodes(), pdfOutputConfig.getBasicVaccination().isKeepEmpty());

    table.draw();
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
    float imageXPosition = pageWidth - PdfServiceUtils.MARGIN - imageWidth;
    float imageYPosition = pageSize.getHeight() - PdfServiceUtils.MARGIN - imageHeight;

    contentStream.drawImage(pdImage, imageXPosition, imageYPosition, imageWidth, imageHeight);
    contentStream.setFont(PdfServiceUtils.getFont(PdfServiceUtils.DEFAULT_FONT, pdfDocument), PdfServiceUtils.FONT_SIZE);
    contentStream.beginText();
    contentStream.newLineAtOffset(imageXPosition, imageYPosition);
    contentStream.newLineAtOffset(0, -PdfServiceUtils.LONG_LINE_BREAK);
    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(I18nKey.PRINTED1.getTranslation(lang), contentStream);
    contentStream.newLineAtOffset(0, -PdfServiceUtils.LONG_LINE_BREAK);
    contentStream.setNonStrokingColor(Color.BLACK);
    PdfServiceUtils.writeText(I18nKey.PRINTED2.getTranslation(lang), contentStream);
    PdfServiceUtils.writeText(PdfServiceUtils.getFormattedDate(LocalDate.now()), contentStream);
    contentStream.endText();
    pdfDocument.setYPositionOfTheLastLine(imageYPosition - PdfServiceUtils.MARGIN);

    contentStream.close();
  }

  private void addBasicImmunizations(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(PdfServiceUtils.COLOR_WHITE);
    PDPage currentPage = document.getPage(document.getNumberOfPages() - 1);
    PDPageContentStream contentStream = new PDPageContentStream(document, currentPage,
        PDPageContentStream.AppendMode.APPEND, true, true);
    PDType0Font font = PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument);
    contentStream.setFont(font, 15);
    contentStream.beginText();
    contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, pdfDocument.getYPositionOfTheLastLine());
    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(I18nKey.BASIC_IMMUNIZATIONS.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    PdfServiceUtils.updateYposition(15, pdfDocument);
    BaseTable table = PdfServiceUtils.createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    addBasicImmunizationsTableHeader(table, vaccinationRecordDTO.getLang(), PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    PdfServiceUtils.updateYposition(PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    List<BasicImmunizationDTO> basicImmunizations = DateComparator.sortByDateDesc(
        vaccinationRecordDTO.getBasicImmunizations(), Comparator.comparing(BasicImmunizationDTO::getOnsetDate));
    for (BasicImmunizationDTO basicImmunization : basicImmunizations) {
      addBasicImmunizationRow(table, basicImmunization, pdfDocument);
    }
    table.draw();

    PdfServiceUtils.updateYposition(PdfServiceUtils.CELL_HEIGHT, pdfDocument);
  }

  private void addBasicImmunizationsTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    List<String> headers = List.of(I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang),
        I18nKey.BASIC_IMMUNIZATION.getTranslation(lang));

    PdfServiceUtils.addTableHeader(table, height, headers, BASIC_IMMUNIZATION_TABLE_WIDTHS, pdfDocument);
  }

  private void addBasicImmunizationRow(BaseTable table, BasicImmunizationDTO dto, PdfDocument pdfDocument)
      throws IOException {
    if (dto == null) {
      log.warn("addRow BasicImmunizationDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    Row<PDPage> row = table.createRow(15);
    Cell<PDPage> cell = PdfServiceUtils.addCell(dto.isValidated() ? "+" : "", BASIC_IMMUNIZATION_TABLE_WIDTHS[0], row,
        pdfDocument);
    cell.setAlign(HorizontalAlignment.CENTER);
    PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getOnsetDate()),
        BASIC_IMMUNIZATION_TABLE_WIDTHS[1], row, pdfDocument);
    if (dto.getCode() != null) {
      PdfServiceUtils.addCell(getTranslation(translationsRequest.get().getBasicImmunizationCodeToName(), dto.getCode()) +
          (dto.getComment() != null ? "*" : ""), BASIC_IMMUNIZATION_TABLE_WIDTHS[2], row, pdfDocument);
    }
    PdfServiceUtils.updateYposition(row.getHeight(), pdfDocument);
    PdfServiceUtils.setRowColor(row, pdfDocument.getRowBackgoundColor());
    PdfServiceUtils.swapColor(pdfDocument);
  }

  private void addMedicalProblems(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(PdfServiceUtils.COLOR_WHITE);
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument);
    contentStream.setFont(font, 15);
    contentStream.beginText();
    contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, pdfDocument.getYPositionOfTheLastLine());
    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(I18nKey.RISK_FACTORS.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    PdfServiceUtils.updateYposition(15, pdfDocument);
    BaseTable table = PdfServiceUtils.createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    addMedicalProblemsTableHeader(table, vaccinationRecordDTO.getLang(), PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    PdfServiceUtils.updateYposition(PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    List<MedicalProblemDTO> medicalProblems = DateComparator.sortByDateDesc(vaccinationRecordDTO.getMedicalProblems(),
        Comparator.comparing(MedicalProblemDTO::getRecordedDate));
    for (MedicalProblemDTO medicalProblem : medicalProblems) {
      addMedicalProblemRow(table, medicalProblem, pdfDocument, vaccinationRecordDTO.getLang());
    }
    table.draw();

    PdfServiceUtils.updateYposition(PdfServiceUtils.CELL_HEIGHT, pdfDocument);
  }

  private void addMedicalProblemsTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    List<String> headers = List.of(I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang),
        I18nKey.BEGIN.getTranslation(lang), I18nKey.END.getTranslation(lang),
        I18nKey.RISK_FACTOR.getTranslation(lang), I18nKey.CLINICAL_STATUS.getTranslation(lang));

    PdfServiceUtils.addTableHeader(table, height, headers, MEDICAL_PROBLEMS_TABLE_WIDTHS, pdfDocument);
  }

  private void addMedicalProblemsComments(VaccinationRecordDTO record, PdfDocument pdfDocument) throws IOException {
    PdfServiceUtils.addCommentsSection(record.getMedicalProblems(), record, pdfDocument,
        I18nKey.COMMENTS_FOR_RISK_FACTORS.getTranslation(record.getLang()), MedicalProblemDTO::getRecordedDate,
        dto -> getTranslation(translationsRequest.get().getMedicalProblemCodeToName(), dto.getCode()));
  }

  private void addNotLegalDocument(String lang, PdfDocument pdfDocument) throws IOException {
    PDDocument pdDocument = pdfDocument.getPdDocument();
    PDPage page = pdDocument.getPage(pdDocument.getNumberOfPages() - 1);
    PDPageContentStream stream =
        new PDPageContentStream(pdDocument, page, PDPageContentStream.AppendMode.APPEND, true, true);
    stream.setFont(PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument), PdfServiceUtils.FONT_SIZE);
    stream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    stream.beginText();
    stream.newLineAtOffset(PdfServiceUtils.MARGIN, pdfDocument.getYPositionOfTheLastLine());
    PdfServiceUtils.writeText(I18nKey.LEGAL_REMARK.getTranslation(lang), stream);
    stream.endText();
    stream.close();
  }

  private void addOtherVaccinations(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(PdfServiceUtils.COLOR_WHITE);
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument);
    contentStream.setFont(font, PdfServiceUtils.FONT_SIZE_TITLE);
    contentStream.beginText();
    contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, pdfDocument.getYPositionOfTheLastLine());

    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(I18nKey.OTHER_VACCINATION.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    PdfServiceUtils.updateYposition(PdfServiceUtils.LONG_LINE_BREAK, pdfDocument);
    BaseTable table = PdfServiceUtils.createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    addVaccinationTableHeader(table, vaccinationRecordDTO.getLang(), PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    PdfServiceUtils.updateYposition(PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    addVaccinationBasedOnTargetDisease(vaccinationRecordDTO, pdfDocument, table,
        pdfOutputConfig.getOtherVaccination().getCodes(), pdfOutputConfig.getOtherVaccination().isKeepEmpty());

    List<String> otherTargetDiseases = vaccinationRecordDTO.getI18nTargetDiseases().stream()
        .map(ValueDTO::getCode)
        .filter(disease -> !pdfOutputConfig.getBasicVaccination().getCodes().contains(disease))
        .filter(disease -> !pdfOutputConfig.getOtherVaccination().getCodes().contains(disease))
        .toList();
    addVaccinationBasedOnTargetDisease(vaccinationRecordDTO, pdfDocument, table, otherTargetDiseases,
        pdfOutputConfig.getOtherVaccination().isKeepEmpty());
    addVaccinationUnknownSpecialCase(vaccinationRecordDTO, pdfDocument, table);
    float nextY = table.draw();
    pdfDocument.setYPositionOfTheLastLine(nextY);
  }

  private void addVaccinationUnknownSpecialCase(VaccinationRecordDTO vaccinationRecord, PdfDocument pdfDocument,
      BaseTable table) throws IOException {
    List<VaccinationDTO> unknownVaccinations = vaccinationRecord.getVaccinations().stream()
        .filter(vaccination -> ValueListService.UNKNOWN_VACCINE_CODE.equals(vaccination.getCode().getCode()))
        .filter(unknownVaccination -> unknownVaccination.getTargetDiseases().isEmpty())
        .toList();
    if (!unknownVaccinations.isEmpty()) {
      addVaccineRows(table, I18nKey.NO_VACCINATION_PROTECTION.getTranslation(vaccinationRecord.getLang()),
          unknownVaccinations, false, pdfDocument);
    }
  }

  private void addVaccinationComments(VaccinationRecordDTO record, PdfDocument pdfDocument,
      boolean includeVaccinationsComments) throws IOException {
    List<VaccinationDTO> vaccinations = record.getVaccinations();
    if (!includeVaccinationsComments) {
      vaccinations = record.getVaccinations().stream()
          .filter(vaccinationDTO -> VACCINE_UNKNOWN_CODE.equals(vaccinationDTO.getCode().getCode()))
          .toList();
    }
    PdfServiceUtils.addCommentsSection(vaccinations, record, pdfDocument,
        I18nKey.COMMENTS_FOR_VACCINATIONS.getTranslation(record.getLang()), VaccinationDTO::getOccurrenceDate,
        dto -> getTranslation(translationsRequest.get().getVaccineCodeToName(), dto.getCode()));
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
        contentStream.newLineAtOffset(pageSize.getWidth() / 2 - PdfServiceUtils.MARGIN, 15);
        PDType0Font font = PdfServiceUtils.getFont(PdfServiceUtils.DEFAULT_FONT, pdfDocument);
        contentStream.setFont(font, PdfServiceUtils.FONT_SIZE - 2.5f);
        PdfServiceUtils.writeText(patientName, contentStream);
        contentStream.newLineAtOffset(pageSize.getWidth() / 2 - 12, 0);
        PdfServiceUtils.writeText(i + "/" + allPages.getCount(), contentStream);
        contentStream.endText();
      }
    }
  }

  private void addPastIllnesses(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(PdfServiceUtils.COLOR_WHITE);
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument);
    contentStream.setFont(font, 15);

    contentStream.beginText();
    contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, pdfDocument.getYPositionOfTheLastLine());
    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(I18nKey.PASTILLNESSES.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    PdfServiceUtils.updateYposition(1.5f * 15, pdfDocument);
    BaseTable table = PdfServiceUtils.createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    addPastIllnessTableHeader(table, vaccinationRecordDTO.getLang(), PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    PdfServiceUtils.updateYposition(1.5f * PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    List<PastIllnessDTO> dtos = DateComparator.sortByDateDesc(vaccinationRecordDTO.getPastIllnesses(),
        Comparator.comparing(PastIllnessDTO::getRecordedDate));
    if (dtos != null) {
      for (PastIllnessDTO dto : dtos) {
        addPastIllnessRow(table, dto, pdfDocument);
      }
    }

    table.draw();
  }

  private void addPastIllnessTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    List<String> headers = List.of(I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang),
        I18nKey.BEGIN.getTranslation(lang), I18nKey.END.getTranslation(lang), I18nKey.PASTILLNESS.getTranslation(lang));
    PdfServiceUtils.addTableHeader(table, height, headers, PASTILLNESS_TABLE_WIDTHS, pdfDocument);
  }

  private void addPastIllnessComments(VaccinationRecordDTO record, PdfDocument pdfDocument) throws IOException {
    PdfServiceUtils.addCommentsSection(record.getPastIllnesses(), record, pdfDocument,
        I18nKey.COMMENTS_FOR_PAST_ILLNESSES.getTranslation(record.getLang()), PastIllnessDTO::getRecordedDate,
        dto -> getTranslation(translationsRequest.get().getIllnessCodeToName(), dto.getCode()));
  }

  private void addPatient(HumanNameDTO patient, String lang, PdfDocument pdfDocument) throws IOException {
    PDPage firstPage = pdfDocument.getPdDocument().getPage(0);
    PDRectangle pageSize = firstPage.getMediaBox();

    try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument.getPdDocument(), firstPage,
        PDPageContentStream.AppendMode.APPEND, true, true)) {
      contentStream.beginText();
      contentStream.setFont(PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument), PdfServiceUtils.FONT_SIZE);
      contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, pageSize.getHeight() - PdfServiceUtils.MARGIN); // start 50 units from top-left corner
      addPatientInformation(I18nKey.LASTNAME.getTranslation(lang), patient.getLastName(), contentStream, pdfDocument);
      addPatientInformation(I18nKey.FIRSTNAME.getTranslation(lang), patient.getFirstName(), contentStream, pdfDocument);
      addPatientInformation(I18nKey.BIRTHDAY.getTranslation(lang), PdfServiceUtils.getFormattedDate(patient.getBirthday()),
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
    contentStream.newLineAtOffset(0, -PdfServiceUtils.LONG_LINE_BREAK);
    contentStream.setFont(PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument), PdfServiceUtils.FONT_SIZE);
    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(title + ": ", contentStream);
    contentStream.setFont(PdfServiceUtils.getFont(PdfServiceUtils.DEFAULT_FONT, pdfDocument), PdfServiceUtils.FONT_SIZE);
    contentStream.setNonStrokingColor(Color.BLACK);
    PdfServiceUtils.writeText(info, contentStream);
  }

  private void addAdverseEventRow(BaseTable table, AllergyDTO dto, PdfDocument pdfDocument) throws IOException {
    if (dto == null) {
      log.warn("addRow AllergyDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    Row<PDPage> row = table.createRow(15);
    Cell<PDPage> cell = PdfServiceUtils.addCell(dto.isValidated() ? "+" : "", ADVERSE_EVENTS_TABLE_WIDTHS[0], row,
        pdfDocument);
    cell.setAlign(HorizontalAlignment.CENTER);
    PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getOccurrenceDate()), ADVERSE_EVENTS_TABLE_WIDTHS[1], row, pdfDocument);
    if (dto.getCode() != null) {
      PdfServiceUtils.addCell(getTranslation(translationsRequest.get().getAllergyCodeToName(), dto.getCode()) +
          (dto.getComment() != null ? "*" : ""), ADVERSE_EVENTS_TABLE_WIDTHS[2], row, pdfDocument);
    }

    String recorderOrOrganization = dto.getRecorder() != null &&
        !dto.getRecorder().getFullName().isBlank() ? dto.getRecorder().getFullName() : dto.getOrganization();
    PdfServiceUtils.addCell(recorderOrOrganization, ADVERSE_EVENTS_TABLE_WIDTHS[3], row, pdfDocument);

    PdfServiceUtils.updateYposition(row.getHeight(), pdfDocument);
    PdfServiceUtils.setRowColor(row, pdfDocument.getRowBackgoundColor());
    PdfServiceUtils.swapColor(pdfDocument);
  }

  private void addMedicalProblemRow(BaseTable table, MedicalProblemDTO dto, PdfDocument pdfDocument, String lang) throws IOException {
    if (dto == null) {
      log.warn("addRow MedicalProblemDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    Row<PDPage> row = table.createRow(15);
    Cell<PDPage> cell = PdfServiceUtils.addCell(dto.isValidated() ? "+" : "", MEDICAL_PROBLEMS_TABLE_WIDTHS[0], row,
        pdfDocument);
    cell.setAlign(HorizontalAlignment.CENTER);
    PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getRecordedDate()), MEDICAL_PROBLEMS_TABLE_WIDTHS[1], row, pdfDocument);
    PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getBegin()), MEDICAL_PROBLEMS_TABLE_WIDTHS[2], row, pdfDocument);
    PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getEnd()), MEDICAL_PROBLEMS_TABLE_WIDTHS[3], row, pdfDocument);
    if (dto.getCode() != null) {
      PdfServiceUtils.addCell(getTranslation(translationsRequest.get().getMedicalProblemCodeToName(), dto.getCode()) +
          (dto.getComment() != null ? "*" : ""), MEDICAL_PROBLEMS_TABLE_WIDTHS[4], row, pdfDocument);
    }
    if (dto.getClinicalStatus() != null) {
      String value = I18nKey.ACTIVE.name().equals(dto.getClinicalStatus().getName().toUpperCase()) ?
          I18nKey.ACTIVE.getTranslation(lang) : I18nKey.INACTIVE.getTranslation(lang);
      PdfServiceUtils.addCell(value, MEDICAL_PROBLEMS_TABLE_WIDTHS[5], row, pdfDocument);
    }
    PdfServiceUtils.setRowColor(row, pdfDocument.getRowBackgoundColor());
    PdfServiceUtils.swapColor(pdfDocument);
    PdfServiceUtils.updateYposition(row.getHeight(), pdfDocument);
  }

  private void addPastIllnessRow(BaseTable table, PastIllnessDTO dto, PdfDocument pdfDocument) throws IOException {
    if (dto == null) {
      log.warn("addRow PastIllnessDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    Row<PDPage> row = table.createRow(15);
    Cell<PDPage> cell = PdfServiceUtils.addCell(dto.isValidated() ? "+" : "", PASTILLNESS_TABLE_WIDTHS[0], row,
        pdfDocument);
    cell.setAlign(HorizontalAlignment.CENTER);
    PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getRecordedDate()), PASTILLNESS_TABLE_WIDTHS[1], row, pdfDocument);
    PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getBegin()), PASTILLNESS_TABLE_WIDTHS[2], row, pdfDocument);
    PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getEnd()), PASTILLNESS_TABLE_WIDTHS[3], row, pdfDocument);
    if (dto.getCode() != null) {
      PdfServiceUtils.addCell(getTranslation(translationsRequest.get().getIllnessCodeToName(), dto.getCode()) +
          (dto.getComment() != null ? "*" : ""), PASTILLNESS_TABLE_WIDTHS[4], row, pdfDocument);
    }
    PdfServiceUtils.updateYposition(row.getHeight(), pdfDocument);
    PdfServiceUtils.setRowColor(row, pdfDocument.getRowBackgoundColor());
    PdfServiceUtils.swapColor(pdfDocument);
  }

  private void addVaccinationBasedOnTargetDisease(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument,
      BaseTable table, List<String> codes, boolean keepEmptyEntries)
      throws IOException {
    for (String code : codes) {
      String targetDiseaseName = PdfServiceUtils.getDiseaseName(vaccinationRecordDTO, code);
      List<VaccinationDTO> vaccinations = PdfServiceUtils.extract(vaccinationRecordDTO.getVaccinations(), code);
      addVaccineRows(table, targetDiseaseName, vaccinations, keepEmptyEntries, pdfDocument);
    }
  }

  private void addVaccinationRecordTitle(String lang, PdfDocument pdfDocument) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    float yPositionOftheLastLine = pdfDocument.getYPositionOfTheLastLine();
    PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(0),
        PDPageContentStream.AppendMode.APPEND, true, true);
    PDType0Font font = PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument);
    contentStream.setFont(font, 25);
    contentStream.beginText();
    contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, yPositionOftheLastLine - 10);
    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(I18nKey.VACCINATION_RECORD.getTranslation(lang), contentStream);
    contentStream.endText();
    contentStream.close();
    PdfServiceUtils.addSeparationLine(pdfDocument);
  }

  private void addBasicVaccinationsTitle(VaccinationRecordDTO vaccinationRecordDTO, PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument);
    contentStream.setFont(font, 15);
    contentStream.beginText();
    contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, pdfDocument.getYPositionOfTheLastLine());
    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(I18nKey.BASIC_VACCINATION.getTranslation(vaccinationRecordDTO.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();
    PdfServiceUtils.updateYposition(PdfServiceUtils.LONG_LINE_BREAK, pdfDocument);
  }

  private void addVaccinationTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    java.util.List<String> headers = java.util.List.of(I18nKey.DISEASE.getTranslation(lang),
        I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang), I18nKey.VACCINE.getTranslation(lang),
        I18nKey.TREATING.getTranslation(lang));

    PdfServiceUtils.addTableHeader(table, height, headers, VACCINE_TABLE_WIDTHS, pdfDocument);
  }

  private void addVaccineRows(BaseTable table, String targetDisease, List<VaccinationDTO> dtos, boolean keepEmpty,
      PdfDocument pdfDocument) throws IOException {
    log.debug("addRows targetDisease:{} {}", targetDisease, dtos.size());
    if (!keepEmpty && dtos.isEmpty()) {
      return;
    }

    Row<PDPage> row = table.createRow(PdfServiceUtils.CELL_HEIGHT);
    var spanncell = PdfServiceUtils.addCell(targetDisease, VACCINE_TABLE_WIDTHS[0], row, pdfDocument);
    if (!dtos.isEmpty()) {
      boolean hasMultipleEntries = dtos.size() > 1;
      if (hasMultipleEntries) {
        // keep 1st row without lines
        spanncell.setBottomBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
      }

      // for each row, produce validated | date | vaccination | author and empty cell for next row
      for (int i = 0; i < dtos.size(); i++) {
        VaccinationDTO dto = dtos.get(i);

        Cell<PDPage> validatedCell = PdfServiceUtils.addCell(dto.isValidated() ? "+" : "", VACCINE_TABLE_WIDTHS[1], row, pdfDocument);
        validatedCell.setAlign(HorizontalAlignment.CENTER);

        PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getOccurrenceDate()), VACCINE_TABLE_WIDTHS[2], row, pdfDocument);
        String vaccineName = getTranslation(translationsRequest.get().getVaccineCodeToName(), dto.getCode());
        PdfServiceUtils.addCell(vaccineName + (dto.getComment() != null ? "*" : ""), VACCINE_TABLE_WIDTHS[3], row, pdfDocument);

        String recorderOrOrganization = dto.getRecorder() != null &&
            !dto.getRecorder().getFullName().isBlank() ? dto.getRecorder().getFullName() : dto.getOrganization();
        PdfServiceUtils.addCell(recorderOrOrganization, VACCINE_TABLE_WIDTHS[4], row, pdfDocument);
        PdfServiceUtils.setRowColor(row, pdfDocument.getRowBackgoundColor());

        boolean nonLastItems = i < dtos.size() - 1;
        boolean lastItem = i == dtos.size() - 1;
        // Special case when the last item of a specific disease is the first item on a new page
        if (lastItem) {
          spanncell.setTopBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
        }
        // Will create new row with first cell empty cell
        if (nonLastItems) {
          spanncell.setTopBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
          PdfServiceUtils.updateYposition(PdfServiceUtils.CELL_HEIGHT, pdfDocument);
          row = table.createRow(PdfServiceUtils.CELL_HEIGHT);
          spanncell = PdfServiceUtils.addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[0], row, pdfDocument);
          if (i < dtos.size() - 2) {
            spanncell.setBottomBorderStyle(LineStyle.produceDashed(pdfDocument.getRowBackgoundColor(), 0));
          }
        }
      }
    } else {
      PdfServiceUtils.addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[1], row, pdfDocument);
      PdfServiceUtils.addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[2], row, pdfDocument);
      PdfServiceUtils.addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[3], row, pdfDocument);
      PdfServiceUtils.addCell(EMPTY_CONTENT, VACCINE_TABLE_WIDTHS[4], row, pdfDocument);
      PdfServiceUtils.setRowColor(row, pdfDocument.getRowBackgoundColor());
    }
    PdfServiceUtils.updateYposition(PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    PdfServiceUtils.swapColor(pdfDocument);
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
    pdfDocument.setYPositionOfTheLastLine(newPage.getMediaBox().getHeight() - PdfServiceUtils.MARGIN);
    document.addPage(newPage);
    return new PDPageContentStream(document, newPage, PDPageContentStream.AppendMode.APPEND, true, true);
  }

  private void fillDocumentContent(VaccinationRecordDTO record, PdfDocument pdfDocument, PdfExportOptionsDTO pdfOptions)
      throws Exception {
    renderHeader(record, pdfDocument);
    renderBasicImmunizations(record, pdfDocument, pdfOptions);
    renderVaccinations(record, pdfDocument, pdfOptions);
    renderAdverseEvents(record, pdfDocument);
    renderIllnesses(record, pdfDocument, pdfOptions);
    renderLaboratorySerology(record, pdfDocument, pdfOptions);
    renderMedicalProblems(record, pdfDocument, pdfOptions);
    renderLegalRemark(record, pdfDocument);
  }

  private void renderBasicImmunizations(VaccinationRecordDTO record, PdfDocument pdfDocument,
      PdfExportOptionsDTO pdfOptions) throws IOException {
    if (record.getBasicImmunizations() == null || record.getBasicImmunizations().isEmpty()) {
      return;
    }
    addBasicImmunizations(record, pdfDocument);
    if (pdfOptions.isIncludeBasicImmunizationsComments()) {
      addBasicImmunizationComments(record, pdfDocument);
    }
  }

  private void addBasicImmunizationComments(VaccinationRecordDTO record, PdfDocument pdfDocument) throws IOException {
    PdfServiceUtils.addCommentsSection(record.getBasicImmunizations(), record, pdfDocument,
        I18nKey.COMMENTS_FOR_BASIC_IMMUNIZATIONS.getTranslation(record.getLang()), BasicImmunizationDTO::getOnsetDate,
        dto -> getTranslation(translationsRequest.get().getBasicImmunizationCodeToName(), dto.getCode()));
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

  private String getTranslation(Map<Pair<String, String>, String> valuesCodeToName, ValueDTO valueDTO) {
    return valuesCodeToName.getOrDefault(Pair.of(valueDTO.getCode(), valueDTO.getSystem()), valueDTO.getName());
  }

  private void renderAdverseEvents(VaccinationRecordDTO record, PdfDocument pdfDocument) throws IOException {
    addAdverseEvents(record, pdfDocument);
    addAdverseEventComments(record, pdfDocument);
  }

  private void renderHeader(VaccinationRecordDTO record, PdfDocument pdfDocument) throws IOException {
    addPatient(record.getPatient(), record.getLang(), pdfDocument);
    addLogo(record.getLang(), pdfDocument);
    addVaccinationRecordTitle(record.getLang(), pdfDocument);
  }

  private void renderIllnesses(VaccinationRecordDTO record, PdfDocument pdfDocument, PdfExportOptionsDTO pdfOptions)
      throws IOException {
    addPastIllnesses(record, pdfDocument);
    if (pdfOptions.isIncludeIllnessesComments()) {
      addPastIllnessComments(record, pdfDocument);
    }
  }

  private void renderLegalRemark(VaccinationRecordDTO record, PdfDocument pdfDocument) throws IOException {
    if (pdfOutputConfig.isLegalCommentVisible()) {
      PdfServiceUtils.addSeparationLine(pdfDocument);
      addNotLegalDocument(record.getLang(), pdfDocument);
    }
  }

  private void renderMedicalProblems(VaccinationRecordDTO record, PdfDocument pdfDocument, PdfExportOptionsDTO pdfOptions)
      throws IOException {
    addMedicalProblems(record, pdfDocument);
    if (pdfOptions.isIncludeMedicalProblemsComments()) {
      addMedicalProblemsComments(record, pdfDocument);
    }
  }

  private void renderVaccinations(VaccinationRecordDTO record, PdfDocument pdfDocument, PdfExportOptionsDTO pdfOptions)
      throws IOException {
    addBasicVaccinationsTitle(record, pdfDocument);
    PDDocument document = pdfDocument.getPdDocument();
    addBaseVaccinations(PdfServiceUtils.createTable(pdfDocument.getYPositionOfTheLastLine(),
        document.getNumberOfPages() - 1, document), record, pdfDocument);
    addOtherVaccinations(record, pdfDocument);
    addVaccinationComments(record, pdfDocument, pdfOptions.isIncludeVaccinationsComments());
  }

  private void renderLaboratorySerology(VaccinationRecordDTO record, PdfDocument pdfDocument,
      PdfExportOptionsDTO pdfOptions) throws IOException {
    List<LaboratorySerologyDTO> laboratorySerologies = record.getLaboratorySerologies();
    if (laboratorySerologies.isEmpty()) {
      return;
    }
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(PdfServiceUtils.COLOR_WHITE);
    PDPageContentStream contentStream = createNewPageContentStream(pdfDocument, document);
    PDType0Font font = PdfServiceUtils.getFont(PdfServiceUtils.BOLD, pdfDocument);
    contentStream.setFont(font, PdfServiceUtils.FONT_SIZE_TITLE);
    contentStream.beginText();
    contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, pdfDocument.getYPositionOfTheLastLine());
    contentStream.setNonStrokingColor(PdfServiceUtils.COLOR_DARKBLUE);
    PdfServiceUtils.writeText(I18nKey.LABORATORY_SEROLOGY.getTranslation(record.getLang()), contentStream);
    contentStream.endText();
    contentStream.close();

    PdfServiceUtils.updateYposition(PdfServiceUtils.LONG_LINE_BREAK, pdfDocument);
    BaseTable table = PdfServiceUtils.createTable(pdfDocument.getYPositionOfTheLastLine(),
        document.getNumberOfPages() - 1, document);
    addLaboratorySerologyTableHeader(table, record.getLang(), PdfServiceUtils.CELL_HEIGHT, pdfDocument);
    PdfServiceUtils.updateYposition(PdfServiceUtils.CELL_HEIGHT, pdfDocument);

    for (LaboratorySerologyDTO dto : laboratorySerologies.stream()
        .sorted(Comparator.comparing(LaboratorySerologyDTO::getRecordedDate,
            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        .toList()) {
      addLaboratorySerologyRow(table, dto, pdfDocument);
    }
    table.draw();
    PdfServiceUtils.updateYposition(PdfServiceUtils.CELL_HEIGHT, pdfDocument);

    if (pdfOptions.isIncludeLaboratorySerologiesComments()) {
      addLaboratorySerologyComments(record, pdfDocument);
    }
  }

  private void addLaboratorySerologyComments(VaccinationRecordDTO record, PdfDocument pdfDocument) throws IOException {
    PdfServiceUtils.addCommentsSection(record.getLaboratorySerologies(), record, pdfDocument,
        I18nKey.COMMENTS_FOR_LABORATORY_SEROLOGIES.getTranslation(record.getLang()),
        LaboratorySerologyDTO::getRecordedDate,
        dto -> getTranslation(translationsRequest.get().getLaboratorySerologyCodeToName(), dto.getCode()));
  }

  private void addLaboratorySerologyTableHeader(BaseTable table, String lang, float height, PdfDocument pdfDocument)
      throws IOException {
    List<String> headers = List.of(I18nKey.VALIDATED.getTranslation(lang), I18nKey.DATE.getTranslation(lang),
        I18nKey.LABORATORY_TEST.getTranslation(lang), I18nKey.RESULT.getTranslation(lang),
        I18nKey.TREATING.getTranslation(lang));
    PdfServiceUtils.addTableHeader(table, height, headers, LABORATORY_SEROLOGY_TABLE_WIDTHS, pdfDocument);
  }

  private void addLaboratorySerologyRow(BaseTable table, LaboratorySerologyDTO dto, PdfDocument pdfDocument)
      throws IOException {
    Row<PDPage> row = table.createRow(PdfServiceUtils.CELL_HEIGHT);
    boolean validated = dto.isValidated();
    Cell<PDPage> validatedCell = PdfServiceUtils.addCell(validated ? "+" : "",
        LABORATORY_SEROLOGY_TABLE_WIDTHS[0], row, pdfDocument);
    validatedCell.setAlign(HorizontalAlignment.CENTER);

    PdfServiceUtils.addCell(PdfServiceUtils.getFormattedDate(dto.getRecordedDate()),
        LABORATORY_SEROLOGY_TABLE_WIDTHS[1], row, pdfDocument);
    PdfServiceUtils.addCell(getTranslation(translationsRequest.get().getLaboratorySerologyCodeToName(), dto.getCode())
        + (dto.getComment() != null ? "*" : ""), LABORATORY_SEROLOGY_TABLE_WIDTHS[2], row, pdfDocument);
    PdfServiceUtils.addCell(getLaboratoryResult(dto.getValue()), LABORATORY_SEROLOGY_TABLE_WIDTHS[3], row, pdfDocument);
    PdfServiceUtils.addCell(getLaboratoryPerformer(dto), LABORATORY_SEROLOGY_TABLE_WIDTHS[4], row, pdfDocument);

    PdfServiceUtils.updateYposition(row.getHeight(), pdfDocument);
    PdfServiceUtils.setRowColor(row, pdfDocument.getRowBackgoundColor());
    PdfServiceUtils.swapColor(pdfDocument);
  }

  private String getLaboratoryResult(ValueDTO value) {
    if (value == null) {
      return EMPTY_CONTENT;
    }
    String numericValue = value.getCode();
    String unit = value.getName();
    return Stream.of(numericValue, unit)
        .filter(Objects::nonNull)
        .filter(text -> !text.isBlank())
        .collect(Collectors.joining(" "));
  }

  private String getLaboratoryPerformer(LaboratorySerologyDTO dto) {
    if (dto.getRecorder() != null && !dto.getRecorder().getFullName().isBlank()) {
      return dto.getRecorder().getFullName();
    }
    return dto.getOrganization() != null ? dto.getOrganization() : EMPTY_CONTENT;
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
}
