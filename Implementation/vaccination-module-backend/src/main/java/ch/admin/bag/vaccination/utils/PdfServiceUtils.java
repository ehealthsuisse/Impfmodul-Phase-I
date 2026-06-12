/**
 * Copyright (c) 2026 eHealth Suisse
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
package ch.admin.bag.vaccination.utils;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.Row;
import be.quodlibet.boxable.line.LineStyle;
import ch.admin.bag.vaccination.data.PdfDocument;
import ch.admin.bag.vaccination.service.I18nKey;
import ch.admin.bag.vaccination.service.PdfService;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.BasicImmunizationDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;

/**
 * Util class for {@link PdfService}
 */
@Slf4j
public class PdfServiceUtils {
  private static final String FONT = new ClassPathResource("font/FreeSans.ttf").getPath();
  private static final String FONT_BOLD = new ClassPathResource("font/FreeSansBold.ttf").getPath();
  private static final String FONT_OBLIQUE = new ClassPathResource("font/FreeSansOblique.ttf").getPath();

  public static final Color COLOR_DARKBLUE = Color.decode("#014C8A");
  public static final Color COLOR_LIGHTBLUE = Color.decode("#C8E8F6");
  public static final Color COLOR_WHITE = Color.decode("#FFFFFF");

  public static final String BOLD = "bold";
  public static final String DEFAULT_FONT = "";
  public static final String OBLIQUE = "oblique";

  public static final float CELL_HEIGHT = 15;
  public static final float FONT_SIZE = 10;
  public static final float FONT_SIZE_TITLE = 15;
  public static final float LONG_LINE_BREAK = 20;
  public static final float MARGIN = 50;
  private static final float[] ONE_COLUMN_TABLE_WIDTH = {100};

  public static Cell<PDPage> addCell(String content, float width, Row<PDPage> headerRow, PdfDocument pdfDocument)
      throws IOException {
    return addCell(DEFAULT_FONT, content, width, headerRow, pdfDocument);
  }

  public static Cell<PDPage> addCell(String font, String content, float width, Row<PDPage> headerRow, PdfDocument pdfDocument)
      throws IOException {
    Cell<PDPage> cell = headerRow.createCell(width, content);
    cell.setFont(PdfServiceUtils.getFont(font, pdfDocument));
    return cell;
  }

  /**
   * Shared helper that renders any "comments" section for DTO lists containing remarks.
   * It filters entries that actually have comments, ensures enough space, writes the section title,
   * and finally delegates to {@link #drawCommentsForTable(List, VaccinationRecordDTO, PdfDocument, Function, Function)}
   * using the provided extractors.
   *
   * @param <T> DTO type extending {@link BaseDTO}
   * @param dtos full list of domain entries to be checked for comments
   * @param record overall vaccination record (used mainly for localization)
   * @param pdfDocument mutable PDF context to draw onto
   * @param title localized title for the comments block
   * @param dateExtractor function extracting the display date for each DTO
   * @param translationExtractor function providing the localized label for each DTO
   * @throws IOException if writing to the PDF fails
   */
  public static <T extends BaseDTO> void addCommentsSection(List<T> dtos, VaccinationRecordDTO record,
      PdfDocument pdfDocument, String title, Function<T, LocalDate> dateExtractor,
      Function<T, String> translationExtractor) throws IOException {
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    List<T> dtosWithComments = dtos.stream().filter(dto -> dto.getComment() != null).toList();

    if (dtosWithComments.isEmpty()) {
      return;
    }

    ensureSufficientSpaceForContent(dtosWithComments.size(), pdfDocument);
    addTitleSection(record, pdfDocument, title);
    drawCommentsForTable(dtosWithComments, record, pdfDocument, dateExtractor, translationExtractor);
  }

  public static void addSeparationLine(PdfDocument pdfDocument)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    PDPageContentStream contentStreamLine =
        new PDPageContentStream(document, document.getPage(document.getNumberOfPages() - 1),
            PDPageContentStream.AppendMode.APPEND, true, true);
    float yPositionOftheLastLine = pdfDocument.getYPositionOfTheLastLine();
    contentStreamLine.moveTo(PdfServiceUtils.MARGIN, yPositionOftheLastLine - 35); // Starting point (x, y)
    contentStreamLine.lineTo(PDRectangle.A4.getWidth() - PdfServiceUtils.MARGIN, yPositionOftheLastLine - 35); // Ending point (x, y)
    pdfDocument.setYPositionOfTheLastLine(yPositionOftheLastLine - 70);
    contentStreamLine.stroke();
    contentStreamLine.close();
  }

  public static void addTableHeader(BaseTable table, float height, List<String> headers, float[] widths,
      PdfDocument pdfDocument) throws IOException {
    Row<PDPage> headerRow = table.createRow(height);
    for (int i = 0; i < headers.size(); i++) {
      Cell<PDPage> cell = PdfServiceUtils.addCell(headers.get(i), widths[i], headerRow, pdfDocument);
      cell.setTextColor(PdfServiceUtils.COLOR_WHITE);
      cell.setFillColor(PdfServiceUtils.COLOR_DARKBLUE);
    }
  }

  public static void addTableRow(BaseTable table, BaseDTO dto, PdfDocument pdfDocument, String lang, String translation,
      LocalDate occurrenceDate) throws IOException {
    if (dto == null) {
      return;
    }
    Row<PDPage> row = table.createRow(PdfServiceUtils.CELL_HEIGHT);
    Cell<PDPage> cell = addTableCell(dto, pdfDocument, row, translation, occurrenceDate);
    addCommentRow(table, dto, pdfDocument, cell, lang);
    PdfServiceUtils.updateYposition(row.getHeight(), pdfDocument);
    PdfServiceUtils.swapColor(pdfDocument);
  }

  public static BaseTable createTable(float yStart, int pageNumber, PDDocument document) throws IOException {
    PDPage page = document.getPage(pageNumber);
    float tableWidth = page.getMediaBox().getWidth() - 2 * PdfServiceUtils.MARGIN;
    return new BaseTable(yStart, document.getPage(0).getMediaBox().getHeight() - PdfServiceUtils.MARGIN,
        PdfServiceUtils.MARGIN, tableWidth, PdfServiceUtils.MARGIN, document, page, true, true);
  }

  public static List<VaccinationDTO> extract(List<VaccinationDTO> inputs, String code) {
    return DateComparator.sortByDateDesc(inputs, Comparator.comparing(VaccinationDTO::getOccurrenceDate))
        .stream()
        .filter(dto -> dto.getTargetDiseases().stream().anyMatch(td -> td.getCode().equals(code)))
        .toList();
  }

  public static String getDiseaseName(VaccinationRecordDTO vaccinationRecordDTO, String code) {
    for (ValueDTO valueDTO : vaccinationRecordDTO.getI18nTargetDiseases()) {
      if (code.equals(valueDTO.getCode())) {
        return valueDTO.getName();
      }
    }
    return code;
  }

  public static String getFormattedDate(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    return date != null ? date.format(formatter) : "-";
  }

  public static PDType0Font getFont(String fontname, PdfDocument pdfDocument) throws IOException {
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

  public static void updateYposition(float delta, PdfDocument pdfDocument) {
    float yPositionOfTheLastLine = pdfDocument.getYPositionOfTheLastLine();
    boolean isNoNewPageNecessary = yPositionOfTheLastLine - delta > PdfServiceUtils.MARGIN;
    if (isNoNewPageNecessary) {
      pdfDocument.setYPositionOfTheLastLine(yPositionOfTheLastLine - delta);
    } else {
      pdfDocument.setYPositionOfTheLastLine(PDRectangle.A4.getHeight() - PdfServiceUtils.MARGIN);
    }
  }

  public static void setRowColor(Row<PDPage> row, Color rowBackgoundColor) {
    for (Cell<PDPage> cell : row.getCells()) {
      cell.setFillColor(rowBackgoundColor);
    }
  }

  public static void swapColor(PdfDocument pdfDocument) {
    if (PdfServiceUtils.COLOR_WHITE.equals(pdfDocument.getRowBackgoundColor())) {
      pdfDocument.setRowBackgoundColor(COLOR_LIGHTBLUE);
    } else {
      pdfDocument.setRowBackgoundColor(PdfServiceUtils.COLOR_WHITE);
    }
  }

  public static void writeText(String value, PDPageContentStream contentStream) throws IOException {
    if (value != null) {
      contentStream.showText(value);
    }
  }

  private static void addCommentRow(BaseTable table, BaseDTO dto, PdfDocument pdfDocument, Cell<PDPage> cell,
      String lang) throws IOException {
    CommentDTO commentDTO = dto.getComment();
    if (commentDTO != null) {
      addCommentCells(pdfDocument, commentDTO, table, lang);

      // Remove bottom border from allergy/vaccination cell so that together with the comment appear as one block
      cell.setBottomBorderStyle(null);

      Row<PDPage> lastRow = table.getRows().getLast();
      PdfServiceUtils.updateYposition(lastRow.getHeight(), pdfDocument);
      PdfServiceUtils.setRowColor(lastRow, pdfDocument.getRowBackgoundColor());
    }
  }

  private static void addCommentCells(PdfDocument pdfDocument, CommentDTO commentDTO, BaseTable table, String lang)
      throws IOException {
    String header = I18nKey.LAST_MODIFIED_BY.getTranslation(lang) + commentDTO.getAuthor() + ", " +
        getFormattedDateTime(commentDTO.getDate());

    String[] textLines = commentDTO.getText().split("\\R");

    // First row: header + all comment lines in the same cell (multiline)
    StringBuilder firstCellContent = new StringBuilder(header);
    for (String line : textLines) {
      firstCellContent.append("<br>").append(line.trim());
    }

    Row<PDPage> row = table.createRow(PdfServiceUtils.CELL_HEIGHT / 2);
    Cell<PDPage> cell = addCell(firstCellContent.toString(), ONE_COLUMN_TABLE_WIDTH[0], row, pdfDocument);
    cell.setFontSize(8);
    cell.setBottomBorderStyle(new LineStyle(Color.BLACK, 1.3f));
    setRowColor(row, pdfDocument.getRowBackgoundColor());
  }

  private static Cell<PDPage> addTableCell(BaseDTO dto, PdfDocument pdfDocument, Row<PDPage> row, String translation,
      LocalDate occurrenceDate) throws IOException {
    log.debug("addRow {}", dto.getCode().getName());
    String value = (dto.getComment() != null ? "* " + translation : translation) + ", " +
        getFormattedDate(occurrenceDate);
    if (!(dto instanceof BasicImmunizationDTO)) {
      String performer = dto.getRecorder() != null ? dto.getRecorder().getFullName() : dto.getOrganization();
      value = value + ", " + performer;
    }
    Cell<PDPage> cell = addCell(PdfServiceUtils.BOLD, value, ONE_COLUMN_TABLE_WIDTH[0], row, pdfDocument);
    cell.setFontSize(PdfServiceUtils.FONT_SIZE);
    PdfServiceUtils.setRowColor(row, pdfDocument.getRowBackgoundColor());
    return cell;
  }

  private static void addTitleSection(VaccinationRecordDTO record, PdfDocument pdfDocument, String title) throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    PDPage lastPage = document.getPage(document.getNumberOfPages() - 1);

    try (PDPageContentStream contentStream = new PDPageContentStream(document, lastPage, PDPageContentStream.AppendMode.APPEND, true)) {
      // Setup font and positioning
      PDType0Font font = getFont(BOLD, pdfDocument);
      contentStream.setFont(font, PdfServiceUtils.FONT_SIZE_TITLE);
      contentStream.setNonStrokingColor(COLOR_DARKBLUE);

      // Position and write title
      float spaceAbove = PdfServiceUtils.MARGIN;
      float y = pdfDocument.getYPositionOfTheLastLine() - spaceAbove;

      contentStream.beginText();
      contentStream.newLineAtOffset(PdfServiceUtils.MARGIN, y);
      writeText(title, contentStream);
      contentStream.endText();
    }

    // Update position after title
    float titleHeight = PdfServiceUtils.FONT_SIZE_TITLE * 1.2f;
    float spaceAbove = PdfServiceUtils.MARGIN;
    float currentY = pdfDocument.getYPositionOfTheLastLine();

    pdfDocument.setYPositionOfTheLastLine(currentY - spaceAbove - titleHeight);
    updateYposition(8, pdfDocument);
  }

  /**
   * Renders the comments table for the provided DTO list by reusing a generic drawing process.
   * Entries use the supplied extractors to resolve dates and code names before delegating to {@code addTableRow}.
   *
   * @param <T> DTO type extending {@link BaseDTO}
   * @param dtos source entries containing comments
   * @param record overall vaccination record for localization data
   * @param pdfDocument target PDF document context
   * @param dateExtractor function that provides the display date for each DTO
   * @param translationExtractor function that provides the localized label for each DTO
   * @throws IOException if the PDF table cannot be generated
   */
  private static <T extends BaseDTO> void drawCommentsForTable(List<T> dtos, VaccinationRecordDTO record,
      PdfDocument pdfDocument, Function<T, LocalDate> dateExtractor, Function<T, String> translationExtractor)
      throws IOException {
    PDDocument document = pdfDocument.getPdDocument();
    pdfDocument.setRowBackgoundColor(COLOR_WHITE);
    BaseTable table = createTable(pdfDocument.getYPositionOfTheLastLine(), document.getNumberOfPages() - 1,
        document);
    for (T dto : dtos) {
      LocalDate date = dateExtractor.apply(dto);
      String translation = translationExtractor.apply(dto);
      addTableRow(table, dto, pdfDocument, record.getLang(), translation, date);
    }
    table.draw();
  }

  /**
   * Ensures there is sufficient space on the current page for the unknown vaccinations content.
   * Calculates the required height for the title, spacing, and table based on the number of vaccinations.
   * If insufficient space is available, creates a new page and resets the Y position.
   *
   * @param vaccinationCount the number of unknown vaccinations to be displayed in the table
   * @param pdfDocument the PDF document being modified
   */
  private static void ensureSufficientSpaceForContent(int vaccinationCount, PdfDocument pdfDocument) {
    float titleHeight = FONT_SIZE_TITLE * 1.2f;
    int rowCount = vaccinationCount + 1;
    float estimatedTableHeight = rowCount * CELL_HEIGHT;
    float neededHeight = MARGIN + titleHeight + 8 + estimatedTableHeight;

    float currentY = pdfDocument.getYPositionOfTheLastLine();
    float bottomMargin = 50f;

    if (currentY - neededHeight < bottomMargin) {
      PDDocument document = pdfDocument.getPdDocument();
      PDPage newPage = new PDPage(document.getPage(0).getMediaBox());
      document.addPage(newPage);

      float pageHeight = document.getPage(0).getMediaBox().getHeight();
      pdfDocument.setYPositionOfTheLastLine(pageHeight - 20);
    }
  }

  private static String getFormattedDateTime(LocalDateTime dateTime) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm");
    return dateTime != null ? dateTime.format(formatter) : "-";
  }

  private static PDType0Font getPDType0Font(PDDocument document, String fontPath) throws IOException {
    return PDType0Font.load(document, new ClassPathResource(fontPath).getInputStream());
  }
}
