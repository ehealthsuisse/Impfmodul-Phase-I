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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.admin.bag.vaccination.data.dto.PdfExportOptionsDTO;
import ch.admin.bag.vaccination.data.request.PdfExportRequest;
import ch.fhir.epr.adapter.FhirAdapter;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BasicImmunizationDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.LaboratorySerologyDTO;
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
import org.apache.pdfbox.text.PDFTextStripper;
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
  private final List<BasicImmunizationDTO> basicImmunizationDTOs = new ArrayList<>();
  private final List<LaboratorySerologyDTO> laboratorySerologyDTOs = new ArrayList<>();
  private HumanNameDTO patient;
  private PdfExportRequest pdfExportRequest;

  @BeforeAll
  void setUp() {
    List<String> localJsons = fhirAdapter.getLocalEntities();
    for (String localJson : localJsons) {
      Bundle bundle = fhirAdapter.unmarshallFromString(localJson);
      allergyDTOs.addAll(fhirAdapter.getDTOs(AllergyDTO.class, bundle));
      pastIllnessDTOs.addAll(fhirAdapter.getDTOs(PastIllnessDTO.class, bundle));
      vaccinationDTOs.addAll(fhirAdapter.getDTOs(VaccinationDTO.class, bundle));
      medicalProblemDTOs.addAll(fhirAdapter.getDTOs(MedicalProblemDTO.class, bundle));
      basicImmunizationDTOs.addAll(fhirAdapter.getDTOs(BasicImmunizationDTO.class, bundle));
      laboratorySerologyDTOs.addAll(fhirAdapter.getDTOs(LaboratorySerologyDTO.class, bundle));
      pdfExportRequest = new PdfExportRequest();
      pdfExportRequest.setAllergyCodes(allergyDTOs.stream().map(AllergyDTO::getCode).toList());
      pdfExportRequest.setVaccineCodes(vaccinationDTOs.stream().map(VaccinationDTO::getCode).toList());
      pdfExportRequest.setIllnessCodes(pastIllnessDTOs.stream().map(PastIllnessDTO::getCode).toList());
      pdfExportRequest.setMedicalProblemCodes(medicalProblemDTOs.stream().map(MedicalProblemDTO::getCode).toList());
      pdfExportRequest.setBasicImmunizationCodes(
          basicImmunizationDTOs.stream().map(BasicImmunizationDTO::getCode).toList());
      pdfExportRequest.setLaboratorySerologyCodes(
          laboratorySerologyDTOs.stream().map(LaboratorySerologyDTO::getCode).toList());
      pdfExportRequest.setPdfOptions(new PdfExportOptionsDTO(true, true, true, true, true, true));
    }
    patient = new HumanNameDTO("Hans", "Müller", "Herr", LocalDate.now(), "MALE");
  }

  @Test
  public void test_valueListWithConflicts_noExceptionOccurs() {
    List<ValueDTO> list = Arrays.asList(
      new ValueDTO("14189004", "Masern", "systemA"),
      new ValueDTO("14189004", "OtherType", "systemB"));

    assertDoesNotThrow(() -> pdfExportRequest.setVaccineCodes(list));
  }

  @Test
  public void test_pdf_creation() throws Exception {
    allergyDTOs.getFirst().setComment(
        new CommentDTO(LocalDateTime.now().minusDays(10), " Dr. Michel Dupont ",
            "Comment1"));
    pastIllnessDTOs.getFirst().setComment(
        new CommentDTO(LocalDateTime.now().minusDays(9), " Dr. Martha Musterfrau ",
            "Comment2"));
    medicalProblemDTOs.getFirst().setComment(
        new CommentDTO(LocalDateTime.now().minusDays(8), " Dr. Julia Gertnerin ",
            "Comment3"));

    vaccinationDTOs.clear();
    vaccinationDTOs.add(new VaccinationDTO("id",
        new ValueDTO("123", "aVaccine", "myySystem"),
        Arrays.asList(
            new ValueDTO("397430003", "Diphtherie", "myySystem"),
            new ValueDTO("40468003", "Virale Hepatitis, Typ A", "myySystem")),
        null, 1234, LocalDate.now(), new HumanNameDTO("Michel", "Dupont", "Dr", null, null),
        "organization", "lotNumber", new ValueDTO("reason", null, null), new ValueDTO("status", null, null),
           new ValueDTO("76104008", "Not confirmed", "http://snomed.info/sct")));
    vaccinationDTOs.add(new VaccinationDTO("id",
        new ValueDTO("124", "anotherVaccine", "myySystem"),
        Arrays.asList(
            new ValueDTO("14189004", "Masern", "myySystem"),
            new ValueDTO("4740000", "Herpes zoster", "myySystem"),
            new ValueDTO("777", "myDisease", "myySystem")),
        null, 1234, LocalDate.now(), new HumanNameDTO("Michel", "Dupont", "Dr", null, null),
        "organization", "lotNumber", new ValueDTO("reason", null, null), new ValueDTO("status", null, null),
           new ValueDTO("76104008", "Not confirmed", "http://snomed.info/sct")));

    InputStream stream = pdfService.create(
        createVaccinationRecord("de",
            Arrays.asList(
                new ValueDTO("397430003", "Diphtherie", null),
                new ValueDTO("40468003", "Virale Hepatitis, Typ A", null),
                new ValueDTO("777", "myDisease_de", null))),
        pdfExportRequest);
    generatePDF("vaccinationRecord_de.pdf", stream);

    stream = pdfService.create(
        createVaccinationRecord("en",
            Arrays.asList(
                new ValueDTO("397430003", "Diphtheria", null),
                new ValueDTO("40468003", "Viral hepatitis, type A", null),
                new ValueDTO("777", "myDisease_en", null))),
        pdfExportRequest);
    generatePDF("vaccinationRecord_en.pdf", stream);

    stream = pdfService.create(
        createVaccinationRecord("fr",
            Arrays.asList(
                new ValueDTO("397430003", "diphtérie", null),
                new ValueDTO("40468003", "hépatite virale de type A", null),
                new ValueDTO("777", "myDisease_fr", null))),
        pdfExportRequest);
    generatePDF("vaccinationRecord_fr.pdf", stream);

    stream = pdfService.create(
        createVaccinationRecord("it",
            Arrays.asList(
                new ValueDTO("397430003", "difterite", null),
                new ValueDTO("40468003", "epatite virale tipo A", null),
                new ValueDTO("777", "myDisease_it", null))),
        pdfExportRequest);
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
        createVaccinationRecord("en",
            Arrays.asList(
                new ValueDTO("397430003", "Diphtheria", null),
                new ValueDTO("40468003", "Viral hepatitis, type A", null),
                new ValueDTO("777", "myDisease_en", null))),
        pdfExportRequest);

    PDDocument doc = PDDocument.load(stream);
    assertTrue(doc.isEncrypted());
    doc.close();
  }

  @Test
  void test_pdfContainsCommentSectionsForAllCategories() throws Exception {
    assertThat(vaccinationDTOs).isNotEmpty();
    assertThat(pastIllnessDTOs).isNotEmpty();
    assertThat(medicalProblemDTOs).isNotEmpty();
    assertThat(allergyDTOs).isNotEmpty();

    LocalDateTime now = LocalDateTime.now();
    vaccinationDTOs.getFirst().setComment(new CommentDTO(now.minusDays(1), "Dr. Vaccine", "Vaccination comment"));
    pastIllnessDTOs.getFirst().setComment(new CommentDTO(now.minusDays(2), "Dr. Past", "Past illness comment"));
    medicalProblemDTOs.getFirst().setComment(new CommentDTO(now.minusDays(3), "Dr. Risk", "Medical problem comment"));
    allergyDTOs.getFirst().setComment(new CommentDTO(now.minusDays(4), "Dr. Allergy", "Allergy comment"));

    VaccinationRecordDTO record = createVaccinationRecord("en", List.of(new ValueDTO("397430003", "Diphtheria", null)));

    try (InputStream stream = pdfService.create(record, pdfExportRequest);
        PDDocument document = PDDocument.load(stream)) {
      String pdfText = new PDFTextStripper().getText(document);

      assertThat(pdfText)
          .contains(I18nKey.COMMENTS_FOR_VACCINATIONS.getTranslation("en"))
          .contains(I18nKey.COMMENTS_FOR_PAST_ILLNESSES.getTranslation("en"))
          .contains(I18nKey.COMMENTS_FOR_RISK_FACTORS.getTranslation("en"))
          .contains(I18nKey.COMMENTS_FOR_ADVERSE_EVENTS.getTranslation("en"));
    }
  }

  @Test
  void test_pdfContainsVaccinationAndAdverseEventCommentsWithUnknownVaccineCode() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    List<VaccinationDTO> vaccinations = new ArrayList<>(vaccinationDTOs);
    List<AllergyDTO> allergies = new ArrayList<>(allergyDTOs);

    VaccinationDTO unknownVaccine = new VaccinationDTO("id",
        new ValueDTO("787859002", "Unknown vaccine", "testsystem"),
        List.of(new ValueDTO("14189004", "Masern", "testsystem")),
        null, 1234, LocalDate.now(), new HumanNameDTO("John", "Doe", "Dr", null, null),
        "organization", "lotNumber", new ValueDTO("reason", null, null), new ValueDTO("status", null, null),
        new ValueDTO("59156000", "Confirmed", "http://snomed.info/sct"));
    unknownVaccine.setComment(new CommentDTO(now.minusDays(1), "Dr. Vaccine", "Unknown vaccine comment"));
    vaccinations.add(unknownVaccine);

    allergies.getFirst().setComment(new CommentDTO(now.minusDays(2), "Dr. Allergy", "Allergy comment"));

    PdfExportRequest exportRequest = new PdfExportRequest();
    exportRequest.setVaccineCodes(vaccinations.stream().map(VaccinationDTO::getCode).toList());
    exportRequest.setAllergyCodes(allergies.stream().map(AllergyDTO::getCode).toList());
    exportRequest.setIllnessCodes(pastIllnessDTOs.stream().map(PastIllnessDTO::getCode).toList());
    exportRequest.setMedicalProblemCodes(medicalProblemDTOs.stream().map(MedicalProblemDTO::getCode).toList());
    exportRequest.setBasicImmunizationCodes(basicImmunizationDTOs.stream().map(BasicImmunizationDTO::getCode).toList());
    exportRequest.setLaboratorySerologyCodes(laboratorySerologyDTOs.stream().map(LaboratorySerologyDTO::getCode).toList());
    exportRequest.setPdfOptions(new PdfExportOptionsDTO(false, true, false, false, false, false));

    VaccinationRecordDTO record = createVaccinationRecord("en", allergies, pastIllnessDTOs, vaccinations, medicalProblemDTOs,
        basicImmunizationDTOs, laboratorySerologyDTOs, List.of(new ValueDTO("397430003", "Diphtheria", null)));

    try (InputStream stream = pdfService.create(record, exportRequest);
        PDDocument document = PDDocument.load(stream)) {
      String pdfText = new PDFTextStripper().getText(document);

      assertThat(pdfText)
          .contains(I18nKey.COMMENTS_FOR_VACCINATIONS.getTranslation("en"))
          .contains(I18nKey.COMMENTS_FOR_ADVERSE_EVENTS.getTranslation("en"));
    }
  }

  @Test
  void test_pdfContainsBasicImmunizationAndLaboratorySectionsAndComments() throws Exception {
    assertThat(basicImmunizationDTOs).isNotEmpty();
    assertThat(laboratorySerologyDTOs).isNotEmpty();

    LocalDateTime now = LocalDateTime.now();
    basicImmunizationDTOs.getFirst().setComment(new CommentDTO(now.minusDays(1), "Dr. Basic", "Basic comment"));
    laboratorySerologyDTOs.getFirst().setComment(new CommentDTO(now.minusDays(2), "Dr. Lab", "Laboratory comment"));

    VaccinationRecordDTO record = createVaccinationRecord("en", List.of(new ValueDTO("397430003", "Diphtheria", null)));

    try (InputStream stream = pdfService.create(record, pdfExportRequest);
        PDDocument document = PDDocument.load(stream)) {
      String pdfText = new PDFTextStripper().getText(document);

      assertThat(pdfText)
          .contains(I18nKey.BASIC_IMMUNIZATIONS.getTranslation("en"))
          .contains(I18nKey.LABORATORY_SEROLOGY.getTranslation("en"))
          .contains(I18nKey.COMMENTS_FOR_BASIC_IMMUNIZATIONS.getTranslation("en"))
          .contains(I18nKey.COMMENTS_FOR_LABORATORY_SEROLOGIES.getTranslation("en"));
    }
  }

  @Test
  void test_laboratorySerologyTableUsesTranslatedCodeName() throws Exception {
    String code = "test-laboratory-serology-code";
    String system = "http://loinc.org";
    String originalName = "Original laboratory serology display";
    String translatedName = "Translated laboratory serology display";

    LaboratorySerologyDTO laboratorySerology = new LaboratorySerologyDTO("id", LocalDate.now(),
        new ValueDTO(code, originalName, system), new ValueDTO("final", "Final", null),
        null, new ValueDTO("34", "[iU]/mL", "http://unitsofmeasure.org"), null,
        new HumanNameDTO("John", "Doe", "Dr", null, null), null, "organization");

    PdfExportRequest exportRequest = new PdfExportRequest();
    exportRequest.setAllergyCodes(List.of());
    exportRequest.setIllnessCodes(List.of());
    exportRequest.setMedicalProblemCodes(List.of());
    exportRequest.setBasicImmunizationCodes(List.of());
    exportRequest.setVaccineCodes(List.of());
    exportRequest.setTargetDiseases(List.of());
    exportRequest.setLaboratorySerologyCodes(List.of(new ValueDTO(code, translatedName, system)));
    exportRequest.setPdfOptions(new PdfExportOptionsDTO(false, false, false, false, false, false));

    VaccinationRecordDTO record = createVaccinationRecord("fr", List.of(), List.of(), List.of(), List.of(),
        List.of(), List.of(laboratorySerology), List.of());

    try (InputStream stream = pdfService.create(record, exportRequest);
        PDDocument document = PDDocument.load(stream)) {
      String pdfText = new PDFTextStripper().getText(document);

      assertThat(pdfText)
          .contains(translatedName)
          .doesNotContain(originalName);
    }
  }

  private void generatePDF(String filename, InputStream stream) throws Exception {
    File pdfFile = new File(filename);
    Files.copy(stream, pdfFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    IOUtils.closeQuietly(stream);
  }

  private VaccinationRecordDTO createVaccinationRecord(String lang, List<ValueDTO> targetDiseases) {
    return createVaccinationRecord(lang, allergyDTOs, pastIllnessDTOs, vaccinationDTOs, medicalProblemDTOs,
        basicImmunizationDTOs, laboratorySerologyDTOs, targetDiseases);
  }

  private VaccinationRecordDTO createVaccinationRecord(String lang, List<AllergyDTO> allergies,
      List<PastIllnessDTO> pastIllnesses, List<VaccinationDTO> vaccinations,
      List<MedicalProblemDTO> medicalProblems, List<BasicImmunizationDTO> basicImmunizations,
      List<LaboratorySerologyDTO> laboratorySerologies, List<ValueDTO> targetDiseases) {
    VaccinationRecordDTO record = new VaccinationRecordDTO();
    record.setLang(lang);
    record.setPatient(patient);
    record.setAllergies(allergies);
    record.setPastIllnesses(pastIllnesses);
    record.setVaccinations(vaccinations);
    record.setMedicalProblems(medicalProblems);
    record.setBasicImmunizations(basicImmunizations);
    record.setLaboratorySerologies(laboratorySerologies);
    record.setI18nTargetDiseases(targetDiseases);
    return record;
  }
}
