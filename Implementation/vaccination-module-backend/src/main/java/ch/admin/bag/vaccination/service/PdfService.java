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

import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.WebColors;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.pdfa.PdfADocument;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Generate pdf files according to PDF/1A standard.
 */
@Slf4j
@Service
public class PdfService {
  private static final String FONT_COULD_NOT_BE_INITIALIZED = "Font could not be initialized.";
  private static final String ICC_PROFILE = "font/sRGB_CS_profile.icm";
  private static final String FONT = new ClassPathResource("font/FreeSans.ttf").getPath();
  private static final String FONT_BOLD = new ClassPathResource("font/FreeSansBold.ttf").getPath();
  private static final String FONT_OBLIQUE = new ClassPathResource("font/FreeSansOblique.ttf").getPath();
  private static final Color FONT_COLOR_WHITE = WebColors.getRGBColor("#FFFFFF");
  private static final Color FONT_COLOR_DARKBLUE = WebColors.getRGBColor("#014C8A");
  private static final Color FONT_COLOR_LIGHTBLUE = WebColors.getRGBColor("#C8E8F6");

  private FontProgram font;
  private FontProgram fontBold;
  private FontProgram fontOblique;
  private Color tableBackgoundColor;
  @Autowired
  private PdfOutputConfig pdfOutputConfig;

  public PdfService() {
    initializeFontProgram();
  }

  public InputStream create(VaccinationRecordDTO vaccinationRecordDTO) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      Document document = createDocumentAndItsMeta(out);
      fillDocumentContent(vaccinationRecordDTO, document);
      document.close();

      return new ByteArrayInputStream(out.toByteArray());
    } catch (Exception e) {
      log.error("Exception:", e);
      return null;
    }
  }

  protected String getFormattedDate(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    return date != null ? date.format(formatter) : "-";
  }

  protected String getFormattedDateTime(LocalDateTime dateTime) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm");
    return dateTime != null ? dateTime.format(formatter) : "-";
  }

  private void addBaseVaccinations(Table table, VaccinationRecordDTO vaccinationRecordDTO) throws Exception {
    for (String code : pdfOutputConfig.getBasicVaccination().getCodes()) {
      String targetDiseaseName = getDiseaseName(vaccinationRecordDTO, code);
      List<VaccinationDTO> vaccinations = extract(vaccinationRecordDTO.getVaccinations(), code);
      addRows(table, targetDiseaseName, vaccinations, vaccinationRecordDTO.getLang(),
          pdfOutputConfig.getBasicVaccination().isKeepEmpty());
    }
  }

  private void addLogo(Document document) throws IOException {
    try (InputStream input = new ClassPathResource("EPDLogo.jpg").getInputStream()) {
      byte[] image = input.readAllBytes();
      Image img = new Image(ImageDataFactory.create(image));
      img.setFixedPosition(1, 402, 750, UnitValue.createPointValue(110));
      document.add(img);
    }
  }

  private void addOtherVaccinations(Table table, VaccinationRecordDTO vaccinationRecordDTO) throws Exception {
    for (String code : pdfOutputConfig.getOtherVaccination().getCodes()) {
      String targetDiseaseName = getDiseaseName(vaccinationRecordDTO, code);
      List<VaccinationDTO> vaccinations = extract(vaccinationRecordDTO.getVaccinations(), code);
      addRows(table, targetDiseaseName, vaccinations, vaccinationRecordDTO.getLang(),
          pdfOutputConfig.getOtherVaccination().isKeepEmpty());
    }
  }

  private void addParagraphTitle(Document document, I18nKey key, String lang) throws Exception {
    addParagraphTitle(document, 14, key, lang);
  }

  private void addParagraphTitle(Document document, int fontSize, I18nKey key, String lang) throws Exception {
    Paragraph paragraph = new Paragraph(createText(key.getTranslation(lang) + "\n",
        fontSize, FONT_COLOR_DARKBLUE, true));
    document.add(paragraph);
  }

  private void addPatient(Document document, HumanNameDTO patient, String lang) throws Exception {
    if (patient == null) {
      log.warn("addPatient patient is null");
      return;
    }
    Paragraph paragraph =
        new Paragraph(createText(I18nKey.LASTNAME.getTranslation(lang) + ": ", 10, FONT_COLOR_DARKBLUE, true));
    paragraph.add(createText(patient.getLastName() + "\n"));
    paragraph.add(createText(I18nKey.FIRSTNAME.getTranslation(lang) + ": ", 10, FONT_COLOR_DARKBLUE, true));
    paragraph.add(createText(patient.getFirstName() + "\n"));
    paragraph.add(createText(I18nKey.BIRTHDAY.getTranslation(lang) + ": ", 10, FONT_COLOR_DARKBLUE, true));
    paragraph.add(createText(getFormattedDate(patient.getBirthday()) + "\n"));
    paragraph.add(createText(I18nKey.GENDER.getTranslation(lang) + ": ", 10, FONT_COLOR_DARKBLUE, true));
    paragraph.add(createText(patient.getGender()));
    document.add(paragraph);
  }

  private void addPrintedParagraph(Document document, String lang) {
    Paragraph paragraph = new Paragraph();
    paragraph.add(createText(I18nKey.PRINTED1.getTranslation(lang) + "\n", 9, FONT_COLOR_DARKBLUE, true));
    paragraph.add(createText(I18nKey.PRINTED2.getTranslation(lang) + getFormattedDate(LocalDate.now()), 9));
    paragraph.setFixedPosition(1, 402, 700, UnitValue.createPercentValue(90));
    document.add(paragraph);
  }

  private void addRemainingVaccinations(Table table, VaccinationRecordDTO vaccinationRecordDTO) {
    for (VaccinationDTO dto : vaccinationRecordDTO.getVaccinations()) {
      for (ValueDTO targetDisease : dto.getTargetDiseases()) {
        if (!pdfOutputConfig.getBasicVaccination().getCodes().contains(targetDisease.getCode()) && //
            !pdfOutputConfig.getOtherVaccination().getCodes().contains(targetDisease.getCode())) {
          String targetDiseaseName = getDiseaseName(vaccinationRecordDTO, targetDisease.getCode());
          addRows(table, targetDiseaseName, Arrays.asList(dto), vaccinationRecordDTO.getLang(), true);
        }
      }
    }
  }

  private void addRow(Document document, AllergyDTO dto, String lang) {
    if (dto == null) {
      log.warn("addRow AllergyDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    Text textAllergy = new Text(dto.getCode().getName())
        .setFont(PdfFontFactory.createFont(fontBold, PdfEncodings.WINANSI, EmbeddingStrategy.FORCE_EMBEDDED));
    Text authorAllergy =
        new Text(", " + getFormattedDate(dto.getOccurrenceDate()) + ", " + dto.getAuthor().getUser().getFullName())
            .setFont(PdfFontFactory.createFont(fontOblique, PdfEncodings.WINANSI, EmbeddingStrategy.FORCE_EMBEDDED));

    Paragraph allergy = new Paragraph();
    if (dto.isValidated()) {
      allergy.add(createText("(+) ", null, null, true));
    }
    allergy.add(textAllergy);
    allergy.add(authorAllergy);

    document.add(allergy);

    for (CommentDTO commentDTO : dto.getComments()) {
      Text textComment = new Text(commentDTO.getText().replace("\n", " "))
          .setFont(PdfFontFactory.createFont(font, PdfEncodings.WINANSI, EmbeddingStrategy.FORCE_EMBEDDED));
      Text authorComment =
          new Text(getFormattedDateTime(commentDTO.getDate()) + ", " + commentDTO.getAuthor().getFullName() + "\n")
              .setFont(PdfFontFactory.createFont(fontOblique, PdfEncodings.WINANSI, EmbeddingStrategy.FORCE_EMBEDDED));
      Paragraph comment = new Paragraph();
      comment.add(authorComment);
      comment.add(textComment);
      document.add(comment);
    }
  }

  private void addRow(Table table, MedicalProblemDTO dto) {
    if (dto == null) {
      log.warn("addRow MedicalProblemDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    table.addCell(createCell(dto.isValidated() ? "+" : ""));
    table.addCell(createCell(getFormattedDate(dto.getRecordedDate())));
    table.addCell(createCell(getFormattedDate(dto.getBegin())));
    table.addCell(createCell(getFormattedDate(dto.getEnd())));
    if (dto.getCode() != null) {
      table.addCell(createCell(dto.getCode().getName()));
    }
    if (dto.getClinicalStatus() != null) {
      table.addCell(createCell(dto.getClinicalStatus().getName()));
    }
    swapColor();
  }

  private void addRow(Table table, PastIllnessDTO dto) {
    if (dto == null) {
      log.warn("addRow PastIllnessDTO null");
      return;
    }

    log.debug("addRow {}", dto.getCode().getName());
    table.addCell(createCell(dto.isValidated() ? "+" : ""));
    table.addCell(createCell(getFormattedDate(dto.getRecordedDate())));
    table.addCell(createCell(getFormattedDate(dto.getBegin())));
    table.addCell(createCell(getFormattedDate(dto.getEnd())));
    if (dto.getCode() != null) {
      table.addCell(createCell(dto.getCode().getName()));
    }

    swapColor();
  }

  private void addRow(Table table, VaccinationDTO dto, String lang) {
    if (dto == null) {
      log.warn("addRow vaccinationDTO null");
      return;
    }
    log.debug("addRow {}", dto.getCode().getName());
    table.addCell(createCell(dto.isValidated() ? "+" : ""));
    table.addCell(createCell(getFormattedDate(dto.getOccurrenceDate())));

    if (dto.getCode() != null) {
      table.addCell(createCell(dto.getCode().getName()));
    }

    if (dto.getRecorder() != null) {
      table.addCell(createCell(dto.getRecorder().getPrefix() + " " + dto.getRecorder().getFirstName() + " "
          + dto.getRecorder().getLastName()));
    }
  }

  private void addRows(Table table, String targetDisease, List<VaccinationDTO> dtos, String lang, boolean keepEmpty) {

    log.debug("addRows targetDisease:{} {}", targetDisease, dtos.size());

    if (!keepEmpty && dtos.isEmpty()) {
      return;
    }

    Cell targetDiseaseCell = new Cell(dtos.size(), 1);
    targetDiseaseCell.setBackgroundColor(tableBackgoundColor);
    targetDiseaseCell.add(new Paragraph(targetDisease));
    table.addCell(targetDiseaseCell);

    if (dtos != null && !dtos.isEmpty()) {
      for (VaccinationDTO dto : dtos) {
        addRow(table, dto, lang);
      }
    } else {
      table.addCell(createCell(""));
      table.addCell(createCell(""));
      table.addCell(createCell(""));
      table.addCell(createCell(""));
    }

    swapColor();
  }

  private void addVaccinationTableHeader(Table table, String lang) {
    Stream.of(I18nKey.DISEASE.getTranslation(lang), //
        I18nKey.VALIDATED.getTranslation(lang), //
        I18nKey.DATE.getTranslation(lang), //
        I18nKey.VACCINE.getTranslation(lang), //
        I18nKey.TREATING.getTranslation(lang))
        .forEach(columnTitle -> createHeaderCell(table, columnTitle));
    tableBackgoundColor = WebColors.getRGBColor("#FFFFFF");
  }

  private void createAllergies(Document document, List<AllergyDTO> dtos, String lang) {
    log.debug("create {} allergies", dtos == null ? 0 : dtos.size());

    if (dtos != null) {
      for (AllergyDTO dto : dtos) {
        addRow(document, dto, lang);
      }
    }
  }

  private Cell createCell(String text) {
    Cell cell = new Cell();
    cell.setBackgroundColor(tableBackgoundColor);
    if ("+".equals(text)) {
      cell.setTextAlignment(TextAlignment.CENTER);
    }
    cell.add(new Paragraph(text));
    return cell;
  }

  private Document createDocumentAndItsMeta(ByteArrayOutputStream out) throws Exception {
    PdfADocument pdf = new PdfADocument(new PdfWriter(out),
        PdfAConformanceLevel.PDF_A_1A,
        new PdfOutputIntent("Custom", "", "http://www.color.org",
            "sRGB IEC61966-2.1", new ClassPathResource(ICC_PROFILE).getInputStream()));

    Document document = new Document(pdf);
    document.setFont(PdfFontFactory.createFont(font, PdfEncodings.WINANSI, EmbeddingStrategy.FORCE_EMBEDDED));
    document.getPdfDocument().setTagged();

    return document;
  }

  private void createHeaderCell(Table table, String columnTitle) {
    Cell header = new Cell();
    header.setBackgroundColor(FONT_COLOR_DARKBLUE);
    header.add(new Paragraph(createText(columnTitle, 10, FONT_COLOR_WHITE, true)));
    table.addCell(header);
  }

  private Table createMedicalProblemTable(List<MedicalProblemDTO> dtos, String lang) {
    log.debug("create {} allergies", dtos == null ? 0 : dtos.size());
    UnitValue[] columnWidths = {new UnitValue(UnitValue.PERCENT, 10),
        new UnitValue(UnitValue.PERCENT, 15),
        new UnitValue(UnitValue.PERCENT, 15),
        new UnitValue(UnitValue.PERCENT, 15),
        new UnitValue(UnitValue.PERCENT, 30),
        new UnitValue(UnitValue.PERCENT, 15)};
    Table table = createTable(columnWidths);
    Stream.of(I18nKey.VALIDATED.getTranslation(lang), //
        I18nKey.DATE.getTranslation(lang),
        I18nKey.BEGIN.getTranslation(lang),
        I18nKey.END.getTranslation(lang),
        I18nKey.MEDICAL_PROBLEM.getTranslation(lang),
        I18nKey.CLINICAL_STATUS.getTranslation(lang))
        .forEach(columnTitle -> createHeaderCell(table, columnTitle));
    tableBackgoundColor = FONT_COLOR_WHITE;

    if (dtos != null) {
      for (MedicalProblemDTO dto : dtos) {
        addRow(table, dto);
      }
    }

    return table;
  }

  private Table createPastIllnessTable(List<PastIllnessDTO> dtos, String lang) {
    log.debug("create {} pastIllness", dtos == null ? 0 : dtos.size());
    UnitValue[] columnWidths = {new UnitValue(UnitValue.PERCENT, 10), new UnitValue(UnitValue.PERCENT, 15),
        new UnitValue(UnitValue.PERCENT, 15),
        new UnitValue(UnitValue.PERCENT, 15), new UnitValue(UnitValue.PERCENT, 45)};
    Table table = createTable(columnWidths);
    Stream.of(I18nKey.VALIDATED.getTranslation(lang),
        I18nKey.DATE.getTranslation(lang),
        I18nKey.BEGIN.getTranslation(lang),
        I18nKey.END.getTranslation(lang),
        I18nKey.PASTILLNESS.getTranslation(lang))
        .forEach(columnTitle -> createHeaderCell(table, columnTitle));
    tableBackgoundColor = WebColors.getRGBColor("#FFFFFF");

    if (dtos != null) {
      for (PastIllnessDTO dto : dtos) {
        addRow(table, dto);
      }
    }

    return table;
  }

  private Table createTable(UnitValue[] columnWidths) {
    Table table = new Table(columnWidths);
    table.setWidth(new UnitValue(UnitValue.PERCENT, 95));
    return table;
  }

  private Text createText(String content) {
    return createText(content, 10, null, false);
  }

  private Text createText(String content, Integer fontSize) {
    return createText(content, fontSize, null, false);
  }

  private Text createText(String content, Integer fontSize, Color color, boolean isBold) {
    Text text = new Text(content != null ? content : "");
    if (color != null) {
      text.setFontColor(color);
    }
    if (isBold) {
      text.setBold();
    }
    if (fontSize != null) {
      text.setFontSize(fontSize);
    }
    return text;
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

  private void fillDocumentContent(VaccinationRecordDTO vaccinationRecordDTO, Document document) throws Exception {
    addPatient(document, vaccinationRecordDTO.getPatient(), vaccinationRecordDTO.getLang());
    addLogo(document);
    addPrintedParagraph(document, vaccinationRecordDTO.getLang());
    addParagraphTitle(document, 20, I18nKey.VACCINATION_RECORD, vaccinationRecordDTO.getLang());

    LineSeparator line = new LineSeparator(new SolidLine());
    line.setWidth(UnitValue.createPercentValue(95));
    document.add(line);

    addParagraphTitle(document, I18nKey.BASIC_VACCINATION, vaccinationRecordDTO.getLang());

    UnitValue[] columnWidths = {new UnitValue(UnitValue.PERCENT, 30), new UnitValue(UnitValue.PERCENT, 10),
        new UnitValue(UnitValue.PERCENT, 15), new UnitValue(UnitValue.PERCENT, 20),
        new UnitValue(UnitValue.PERCENT, 25)};
    Table table = createTable(columnWidths);
    addVaccinationTableHeader(table, vaccinationRecordDTO.getLang());
    addBaseVaccinations(table, vaccinationRecordDTO);
    document.add(table);

    document.add(new AreaBreak(AreaBreakType.NEXT_AREA));
    addParagraphTitle(document, I18nKey.OTHER_VACCINATION, vaccinationRecordDTO.getLang());
    table = createTable(columnWidths);
    addVaccinationTableHeader(table, vaccinationRecordDTO.getLang());
    addOtherVaccinations(table, vaccinationRecordDTO);
    addRemainingVaccinations(table, vaccinationRecordDTO);
    document.add(table);

    document.add(new Paragraph("\n"));
    addParagraphTitle(document, I18nKey.ADVERSE_EVENTS, vaccinationRecordDTO.getLang());
    createAllergies(document, vaccinationRecordDTO.getAllergies(), vaccinationRecordDTO.getLang());

    document.add(new Paragraph("\n"));
    addParagraphTitle(document, I18nKey.PASTILLNESSES, vaccinationRecordDTO.getLang());
    table = createPastIllnessTable(vaccinationRecordDTO.getPastIllnesses(),
        vaccinationRecordDTO.getLang());
    document.add(table);

    document.add(new Paragraph("\n"));
    addParagraphTitle(document, I18nKey.MEDICAL_PROBLEMS, vaccinationRecordDTO.getLang());
    table = createMedicalProblemTable(vaccinationRecordDTO.getMedicalProblems(),
        vaccinationRecordDTO.getLang());
    document.add(table);
  }

  private String getDiseaseName(VaccinationRecordDTO vaccinationRecordDTO, String code) {
    for (ValueDTO valueDTO : vaccinationRecordDTO.getI18nTargetDiseases()) {
      if (code.equals(valueDTO.getCode())) {
        return valueDTO.getName();
      }
    }
    return code;
  }

  private void initializeFontProgram() {
    try {
      // Font must be created per document, thus here, we only create the fontProgramm which
      // is then reused per document usage.
      font = FontProgramFactory.createFont(FONT);
      fontBold = FontProgramFactory.createFont(FONT_BOLD);
      fontOblique = FontProgramFactory.createFont(FONT_OBLIQUE);
    } catch (IOException ex) {
      log.error(FONT_COULD_NOT_BE_INITIALIZED);
      throw new TechnicalException(FONT_COULD_NOT_BE_INITIALIZED, ex);
    }
  }

  private void swapColor() {
    if (tableBackgoundColor.equals(FONT_COLOR_WHITE)) {
      tableBackgoundColor = FONT_COLOR_LIGHTBLUE;
    } else {
      tableBackgoundColor = FONT_COLOR_WHITE;
    }
  }
}
