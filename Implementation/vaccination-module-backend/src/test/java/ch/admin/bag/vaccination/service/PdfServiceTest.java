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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.admin.bag.vaccination.data.request.TranslationsRequest;
import ch.fhir.epr.adapter.FhirAdapter;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 *
 * Test {@link PdfService}
 *
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public class PdfServiceTest {

  @Autowired
  private PdfService pdfService;
  @Autowired
  private FhirAdapter fhirAdapter;

  private final List<AllergyDTO> allergyDTOs = new ArrayList<>();
  private final List<PastIllnessDTO> pastIllnessDTOs = new ArrayList<>();
  private final List<VaccinationDTO> vaccinationDTOs = new ArrayList<>();
  private final List<MedicalProblemDTO> medicalProblemDTOs = new ArrayList<>();
  private HumanNameDTO patient;
  private TranslationsRequest translationsRequest;

  @BeforeAll
  void setUp() {
    List<String> localJsons = fhirAdapter.getLocalEntities();
    for (String localJson : localJsons) {
      Bundle bundle = fhirAdapter.unmarshallFromString(localJson);
      allergyDTOs.addAll(fhirAdapter.getDTOs(AllergyDTO.class, bundle));
      pastIllnessDTOs.addAll(fhirAdapter.getDTOs(PastIllnessDTO.class, bundle));
      vaccinationDTOs.addAll(fhirAdapter.getDTOs(VaccinationDTO.class, bundle));
      medicalProblemDTOs.addAll(fhirAdapter.getDTOs(MedicalProblemDTO.class, bundle));
      translationsRequest = new TranslationsRequest();
      translationsRequest.setAllergyCodes(allergyDTOs.stream().map(AllergyDTO::getCode).toList());
      translationsRequest.setVaccineCodes(vaccinationDTOs.stream().map(VaccinationDTO::getCode).toList());
      translationsRequest.setIllnessCodes(pastIllnessDTOs.stream().map(PastIllnessDTO::getCode).toList());
      translationsRequest.setMedicalProblemCodes(medicalProblemDTOs.stream().map(MedicalProblemDTO::getCode).toList());
    }
    patient = new HumanNameDTO("Hans", "Müller", "Herr", LocalDate.now(), "MALE");
  }

  @Test
  public void test_pdf_creation() throws Exception {
    allergyDTOs.getFirst().setComment(
        new CommentDTO(LocalDateTime.now().minusDays(10), " Dr. Michel Dupont ",
            "Comment1"));

    vaccinationDTOs.add(new VaccinationDTO("id",
        new ValueDTO("123", "myVaccine", "myySystem"),
        Arrays.asList(
            new ValueDTO("14189004", "Masern", "myySystem"),
            new ValueDTO("4740000", "Herpes zoster", "myySystem"),
            new ValueDTO("777", "myDisease", "myySystem")),
        null, 1234, LocalDate.now(), new HumanNameDTO("John", "Doe", "Dr", null, null),
        "organization", "lotNumber", new ValueDTO("reason", null, null), new ValueDTO("status", null, null),
            new ValueDTO("59156000", "Confirmed", "http://snomed.info/sct")));
    vaccinationDTOs.add(new VaccinationDTO("id",
        new ValueDTO("123", "anotherVaccine", "myySystem"),
        Arrays.asList(
            new ValueDTO("14189004", "Masern", "myySystem"),
            new ValueDTO("4740000", "Herpes zoster", "myySystem"),
            new ValueDTO("777", "myDisease", "myySystem")),
        null, 1234, LocalDate.now(), new HumanNameDTO("Michel", "Dupont", "Dr", null, null),
        "organization", "lotNumber", new ValueDTO("reason", null, null), new ValueDTO("status", null, null),
           new ValueDTO("76104008", "Not confirmed", "http://snomed.info/sct")));

    InputStream stream = pdfService.create(
        new VaccinationRecordDTO("de", patient, allergyDTOs, pastIllnessDTOs, vaccinationDTOs, medicalProblemDTOs,
            Arrays.asList(
                new ValueDTO("397430003", "Diphtherie", null),
                new ValueDTO("40468003", "Virale Hepatitis, Typ A", null),
                new ValueDTO("777", "myDisease_de", null))), translationsRequest);
    generatePDF("vaccinationRecord_de.pdf", stream);

    stream =
        pdfService.create(
            new VaccinationRecordDTO("en", patient, allergyDTOs, pastIllnessDTOs, vaccinationDTOs, medicalProblemDTOs,
                Arrays.asList(
                    new ValueDTO("397430003", "Diphtheria", null),
                    new ValueDTO("40468003", "Viral hepatitis, type A", null),
                    new ValueDTO("777", "myDisease_en", null))), translationsRequest);
    generatePDF("vaccinationRecord_en.pdf", stream);

    stream = pdfService.create(
        new VaccinationRecordDTO("fr", patient, allergyDTOs, pastIllnessDTOs, vaccinationDTOs, medicalProblemDTOs,
            Arrays.asList(
                new ValueDTO("397430003", "diphtérie", null),
                new ValueDTO("40468003", "hépatite virale de type A", null),
                new ValueDTO("777", "myDisease_fr", null))), translationsRequest);
    generatePDF("vaccinationRecord_fr.pdf", stream);

    stream = pdfService.create(
        new VaccinationRecordDTO("it", patient, allergyDTOs, pastIllnessDTOs, vaccinationDTOs, medicalProblemDTOs,
            Arrays.asList(
                new ValueDTO("397430003", "difterite", null),
                new ValueDTO("40468003", "epatite virale tipo A", null),
                new ValueDTO("777", "myDisease_it", null))), translationsRequest);
    generatePDF("vaccinationRecord_it.pdf", stream);
  }

  @Test
  public void test_translation() {
    assertThat(I18nKey.BIRTHDAY.getTranslation("en")).isEqualTo("Birthday");
    assertThat(I18nKey.BIRTHDAY.getTranslation("de")).isEqualTo("Geburtsdatum");
    assertThat(I18nKey.BIRTHDAY.getTranslation("fr")).isEqualTo("Date de naissance");
    assertThat(I18nKey.BIRTHDAY.getTranslation("it")).isEqualTo("Data di nascita");
    assertThat(I18nKey.BIRTHDAY.getTranslation("EnglishIfNotDefined")).isEqualTo("Birthday");
    assertThat(I18nKey.BIRTHDAY.getTranslation(null)).isEqualTo("Birthday");
  }

  @Test
  public void test_pdfCreated_isEncrypted() throws IOException {
    InputStream stream = pdfService.create(
        new VaccinationRecordDTO("en", patient, allergyDTOs, pastIllnessDTOs, vaccinationDTOs, medicalProblemDTOs,
            Arrays.asList(
                new ValueDTO("397430003", "Diphtheria", null),
                new ValueDTO("40468003", "Viral hepatitis, type A", null),
                new ValueDTO("777", "myDisease_en", null))), translationsRequest);

    PDDocument doc = PDDocument.load(stream);
    // check that generated PDF is encrypted
    assertTrue(doc.isEncrypted());
    doc.close();

  }

  private void generatePDF(String filename, InputStream stream) throws Exception {
    File pdfFile = new File(filename);
    Files.copy(stream, pdfFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    IOUtils.closeQuietly(stream);
  }
}
