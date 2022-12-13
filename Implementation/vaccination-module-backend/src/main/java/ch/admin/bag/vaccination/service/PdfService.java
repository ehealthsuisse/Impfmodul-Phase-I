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
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generate pdf files
 *
 */
@Slf4j
@Service
public class PdfService {

  public InputStream create(VaccinationRecordDTO vaccinationRecordDTO) {
    try {

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Document document = new Document();
      // document.setPageSize(PageSize.A4.rotate());
      PdfWriter.getInstance(document, out);

      document.open();

      addPatient(document, vaccinationRecordDTO.getPatient(), vaccinationRecordDTO.getLang());

      document.add(new Paragraph("\n"));

      addChunk(document, I18nKey.VACCINATION_RECORD, vaccinationRecordDTO.getLang(),
          FontFactory.getFont(FontFactory.HELVETICA, 18, BaseColor.BLACK));

      document.add(new Paragraph("\n"));
      document.add(new Chunk(new LineSeparator()));
      document.add(new Paragraph("\n"));

      addChunk(document, I18nKey.VACCINATION, vaccinationRecordDTO.getLang(), null);
      PdfPTable table = createVaccinationTable(vaccinationRecordDTO.getVaccinations(), vaccinationRecordDTO.getLang());
      document.add(table);

      addChunk(document, I18nKey.ALLERGY, vaccinationRecordDTO.getLang(), null);
      table = createAllergyTable(vaccinationRecordDTO.getAllergies(), vaccinationRecordDTO.getLang());
      document.add(table);

      addChunk(document, I18nKey.PASTILLNESS, vaccinationRecordDTO.getLang(), null);
      table = createPastIllnessTable(vaccinationRecordDTO.getPastIllnesses(), vaccinationRecordDTO.getLang());
      document.add(table);

      // Path path = Paths.get(ClassLoader.getSystemResource("BAG_logo.png").toURI());
      // Image img = Image.getInstance(path.toAbsolutePath().toString());
      // document.add(img);

      document.close();

      InputStream in = new ByteArrayInputStream(out.toByteArray());

      return in;

    } catch (Exception e) {
      log.warn("Exception:{}" + e.getMessage());
      return null;
    }
  }

  private void addChunk(Document document, I18nKey key, String lang, Font font) throws Exception {
    Chunk chunk = new Chunk(key.getTranslation(lang));
    if (font != null) {
      chunk.setFont(font);
    }
    document.add(chunk);
  }

  private void addPatient(Document document, HumanNameDTO patient, String lang) throws Exception {
    if (patient == null) {
      log.warn("addPatient patient is null");
      return;
    }
    Chunk chunk = new Chunk(I18nKey.LASTNAME.getTranslation(lang) + ":" + patient.getLastName());
    document.add(chunk);
    document.add(new Paragraph(""));
    chunk = new Chunk(I18nKey.FIRSTNAME.getTranslation(lang) + ":" + patient.getFirstName());
    document.add(chunk);
    document.add(new Paragraph(""));
    chunk = new Chunk(I18nKey.BIRTHDAY.getTranslation(lang) + ":" + patient.getBirthday());
    document.add(chunk);
    document.add(new Paragraph(""));
    chunk = new Chunk(I18nKey.GENDER.getTranslation(lang) + ":" + patient.getGender());
    document.add(chunk);
  }

  private void addRows(PdfPTable table, AllergyDTO dto, String lang) {
    if (dto == null) {
      log.warn("addRows AllergyDTO null");
      return;
    }
    log.debug("addRows {}", dto.getAllergyCode().getName());
    table.addCell(getFormattedDate(dto.getOccurrenceDate(), lang));
    if (dto.getAllergyCode() != null) {
      table.addCell(dto.getAllergyCode().getName());
    }
    if (dto.getClinicalStatus() != null) {
      table.addCell(dto.getClinicalStatus().getName());
    }
  }

  private void addRows(PdfPTable table, PastIllnessDTO dto, String lang) {
    if (dto == null) {
      log.warn("addRows PastIllnessDTO null");
      return;
    }
    log.debug("addRows {}", dto.getIllnessCode().getName());
    table.addCell(getFormattedDate(dto.getRecordedDate(), lang));
    if (dto.getIllnessCode() != null) {
      table.addCell(dto.getIllnessCode().getName());
    }
    if (dto.getClinicalStatus() != null) {
      table.addCell(dto.getClinicalStatus().getName());
    }
  }

  private void addRow(PdfPTable table, String targetDisease, VaccinationDTO dto, String lang) {
    if (dto == null) {
      log.warn("addRow vaccinationDTO null");
      return;
    }
    log.debug("addRow {} {}", targetDisease, dto.getVaccineCode().getName());

    table.addCell(targetDisease);

    table.addCell(getFormattedDate(dto.getOccurrenceDate(), lang));

    if (dto.getVaccineCode() != null) {
      table.addCell(dto.getVaccineCode().getName());
    }

    if (dto.getRecorder() != null) {
      table.addCell(dto.getRecorder().getPrefix() + " " + dto.getRecorder().getFirstName() + " "
          + dto.getRecorder().getLastName());
    }
  }

  private void addRows(PdfPTable table, VaccinationDTO dto, String lang) {
    if (dto == null) {
      log.warn("addRows vaccinationDTO null");
      return;
    }
    log.debug("addRows {}", dto.getVaccineCode().getName());

    if (dto.getTargetDiseases() != null) {
      for (int i = 0; i < dto.getTargetDiseases().size(); i++) {
        addRow(table, dto.getTargetDiseases().get(i).getName(), dto, lang);
      }
    } else {
      addRow(table, "-", dto, lang);
    }
  }

  private void addAllergyTableHeader(PdfPTable table, String lang) {
    Stream.of( //
        I18nKey.DATE.getTranslation(lang), //
        I18nKey.ALLERGY.getTranslation(lang), //
        I18nKey.CLINICAL_STATUS.getTranslation(lang))
        .forEach(columnTitle -> {
          PdfPCell header = new PdfPCell();
          header.setBackgroundColor(BaseColor.LIGHT_GRAY);
          // header.setBorderWidth(2);
          header.setPhrase(new Phrase(columnTitle));
          table.addCell(header);
        });
  }

  private void addPastIllnessTableHeader(PdfPTable table, String lang) {
    Stream.of( //
        I18nKey.DATE.getTranslation(lang), //
        I18nKey.PASTILLNESS.getTranslation(lang), //
        I18nKey.CLINICAL_STATUS.getTranslation(lang))
        .forEach(columnTitle -> {
          PdfPCell header = new PdfPCell();
          header.setBackgroundColor(BaseColor.LIGHT_GRAY);
          // header.setBorderWidth(2);
          header.setPhrase(new Phrase(columnTitle));
          table.addCell(header);
        });
  }

  private void addVaccinationTableHeader(PdfPTable table, String lang) {
    Stream.of( //
        I18nKey.DISEASE.getTranslation(lang), //
        I18nKey.DATE.getTranslation(lang), //
        I18nKey.VACCINE.getTranslation(lang), //
        I18nKey.TREATING.getTranslation(lang))
        .forEach(columnTitle -> {
          PdfPCell header = new PdfPCell();
          header.setBackgroundColor(BaseColor.LIGHT_GRAY);
          // header.setBorderWidth(2);
          header.setPhrase(new Phrase(columnTitle));
          table.addCell(header);
        });
  }

  private PdfPTable createAllergyTable(List<AllergyDTO> dtos, String lang) {
    log.debug("create {} allergies", dtos == null ? 0 : dtos.size());
    float[] columnWidths = {3, 8, 5};
    PdfPTable table = new PdfPTable(columnWidths);
    table.setWidthPercentage(95);
    addAllergyTableHeader(table, lang);

    if (dtos != null) {
      for (AllergyDTO dto : dtos) {
        addRows(table, dto, lang);
      }
    }

    return table;
  }

  private PdfPTable createPastIllnessTable(List<PastIllnessDTO> dtos, String lang) {
    log.debug("create {} pastIllness", dtos == null ? 0 : dtos.size());
    float[] columnWidths = {3, 8, 5};
    PdfPTable table = new PdfPTable(columnWidths);
    table.setWidthPercentage(95);
    addPastIllnessTableHeader(table, lang);

    if (dtos != null) {
      for (PastIllnessDTO dto : dtos) {
        addRows(table, dto, lang);
      }
    }

    return table;
  }

  private PdfPTable createVaccinationTable(List<VaccinationDTO> dtos, String lang) {
    log.debug("create {} vaccinations", dtos == null ? 0 : dtos.size());

    Collections.sort(dtos, new Comparator<VaccinationDTO>() {
      @Override
      public int compare(VaccinationDTO v1, VaccinationDTO v2) {
        int compare = v1.getTargetDiseases().get(0).getName().compareTo(v2.getTargetDiseases().get(0).getName());
        if (compare == 0) {
          return v1.getOccurrenceDate().compareTo(v2.getOccurrenceDate());
        }
        return compare;
      }
    });

    float[] columnWidths = {5, 5, 5, 5};
    PdfPTable table = new PdfPTable(columnWidths);
    table.setWidthPercentage(95);
    addVaccinationTableHeader(table, lang);

    if (dtos != null) {
      for (VaccinationDTO dto : dtos) {
        addRows(table, dto, lang);
      }
    }

    return table;
  }

  protected String getFormattedDate(LocalDate date, String lang) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern( //
        I18nKey.DATE_FORMAT.getTranslation(lang), //
        I18nKey.DATE_FORMAT.getLocale(lang));
    String formattedDate = date.format(formatter);
    return formattedDate;
  }
}
