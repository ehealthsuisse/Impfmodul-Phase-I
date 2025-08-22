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
package ch.fhir.epr.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ch.fhir.epr.TestMain;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import jdk.jfr.Description;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.CompositionRelatesToComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestMain.class)
@ActiveProfiles("test")
@Slf4j
class FhirAdapterTest {
  private static final String CONFIG_TESTFILES_PATH = "config/testfiles";
  @Autowired
  private FhirAdapter fhirAdapter;
  @Autowired
  private FhirConverterIfc fhirConverter;

  @Test
  void createBundle_createVaccinationRecord_noExceptionOccurs() {
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    VaccinationRecordDTO record = getVaccinationRecordFromTestfile(patientIdentifier);

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), patientIdentifier, record);

    assertEquals(FhirConstants.META_VACCINATION_RECORD_TYPE_URL, bundle.getMeta().getProfile().getFirst().getValue());
    assertEquals(FhirConstants.META_VACD_COMPOSITION_VAC_REC_URL,
        FhirUtils.getResource(Composition.class, bundle).getMeta().getProfile().getFirst().getValue());
  }

  @Test
  void createBundle_medicalProblem_contentIsCorrect() {
    Bundle originalBundle =
        fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-D-D3-HCP1-C1.json");
    List<MedicalProblemDTO> medicalProblems = fhirAdapter.getDTOs(MedicalProblemDTO.class, originalBundle);
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), patientIdentifier, medicalProblems.getFirst());
    MedicalProblemDTO problem = getFirstDTO(MedicalProblemDTO.class, bundle);
    // new object was created so ID must no longer be equal!
    assertThat(problem.getId()).isNotEqualTo("30327ea1-6893-4c65-896e-c32c394f1ec6");
    assertThat(problem.getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(problem.getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(problem.getCode().getCode()).isEqualTo("402196005");
    assertThat(problem.getCode().getName()).isEqualTo("Atopische Dermatitis im Kindesalter");
    assertThat(problem.getOrganization()).isEqualTo("Gruppenpraxis CH");
  }

  @Test
  void createBundle_vaccinationRecord_forceIADocument_returnedBundleIsAnIAM() {
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    VaccinationRecordDTO record = getVaccinationRecordFromTestfile(patientIdentifier);

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), patientIdentifier, record, true);

    assertEquals(FhirConstants.META_VACCINATION_TYPE_URL, bundle.getMeta().getProfile().getFirst().getValue());
    assertEquals(FhirConstants.META_VACD_COMPOSITION_IMMUN_URL,
        FhirUtils.getResource(Composition.class, bundle).getMeta().getProfile().getFirst().getValue());
  }

  @Test
  void getDTOs_vaccinationRecordAsBundle_doNotReturnAnyEntries() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle_vaccinationRecord");

    assertEquals(0, fhirAdapter.getDTOs(VaccinationDTO.class, bundle).size());
  }

  // test cases are defined in config/fhir.yml (4) and config/testfiles/json (1)
  @Test
  void getLocalEntities_noInput_returnAllTestCasesAndTestfilesExceptEmptyBundle() {
    assertEquals(5, fhirAdapter.getLocalEntities().size());
  }

  @Test
  @Description("This test verifies the parsing of a bundle containing 2 Immunization records. The first record is deemed "
      + "invalid due to a missing occurrence date and is excluded from further processing. Parsing continues, and the "
      + "second record is validated.")
  void parsingBundle_immunizationWithoutOccurenceDateIsExcluded_parsingContinues() {
    Bundle bundle = createBundle("testfiles/Bundle-A-D6-HCP1-C1-parsingBundleTest.json");
    assertThat(fhirAdapter.getDTOs(VaccinationDTO.class, bundle).size()).isEqualTo(1);
  }

  @Test
  void test_A_D1() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");

    assertThat(bundle.getId()).isEqualTo("Bundle/A-D1-P-C1");
    assertThat(bundle.getEntry().size()).isEqualTo(7);
    assertThat(
        FhirUtils.getPractitionerRole(bundle, "PractitionerRole/TC-HCP1-ORG1-ROLE-performer")
            .getPractitioner().getReference()).isEqualTo("Practitioner/TC-HCP1-C1");

    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/TC-HCP1-C1").getNameFirstRep()
        .getFamily()).isEqualTo("Müller");
    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/TC-HCP1-C1").getNameFirstRep()
        .getGivenAsSingleString()).isEqualTo("Peter");
    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/TC-HCP1-C1").getNameFirstRep()
        .getPrefixAsSingleString()).isEqualTo("Dr. med.");
    assertThat(FhirUtils.getAuthor(bundle).getUser().getLastName()).isEqualTo("Wegmueller");
    assertThat(FhirUtils.getAuthor(bundle).getRole()).isEqualTo("PAT");

    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();
    assertThat(vaccinationDTO.getDoseNumber()).isEqualTo(1);
    assertThat(vaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
    assertThat(vaccinationDTO.getCode().getCode()).isEqualTo("558");
    assertThat(vaccinationDTO.getId()).isNotNull();
    assertThat(vaccinationDTO.getStatus().getCode()).isEqualTo("completed");
    assertThat(vaccinationDTO.isValidated()).isFalse();

    assertThat(vaccinationDTO.getRecorder().getFirstName()).isEqualTo("Peter");
    assertThat(vaccinationDTO.getRecorder().getLastName()).isEqualTo("Müller");
    assertThat(vaccinationDTO.getRecorder().getPrefix()).isEqualTo("Dr. med.");

    assertThat(vaccinationDTO.getAuthor().getUser().getLastName()).isEqualTo("Wegmueller");
    assertThat(vaccinationDTO.getAuthor().getRole()).isEqualTo("PAT");

    assertThat(vaccinationDTO.getComment().getText())
        .isEqualTo("Der Patient hat diese Impfung ohne jedwelcher nebenwirkungen gut vertragen.");
    assertThat(vaccinationDTO.getComment().getAuthor()).isEqualTo("Dr. med. Peter Müller");
  }

  @Test
  void test_A_D1_authorNicolaiLütschg_isNotValidated() {
    Bundle bundle =
        fhirAdapter.unmarshallFromFile("src/test/resources/testfiles/Bundle-A-D1-P-C1-AuthorOtherPractitioner.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();
    assertTrue(vaccinationDTO.isValidated());

    bundle =
        fhirAdapter.unmarshallFromFile("src/test/resources/testfiles/Bundle-A-D1-P-C1-AuthorNicolaiLütschg.json");
    vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    vaccinationDTO = vaccinations.getFirst();
    assertFalse(vaccinationDTO.isValidated());
  }

  @Test
  void test_A_D1_withoutPractitionerRole_stillFindValues() {
    Bundle bundle =
        fhirAdapter.unmarshallFromFile("src/test/resources/testfiles/Bundle-A-D1-P-C1-removedPractitionerRole.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();

    assertThat(vaccinationDTO.getRecorder().getFirstName()).isEqualTo("Peter");
    assertThat(vaccinationDTO.getRecorder().getLastName()).isEqualTo("Müller");
    assertThat(vaccinationDTO.getRecorder().getPrefix()).isEqualTo("Dr. med.");
    assertThat(vaccinationDTO.getOrganization()).isEqualTo("Gruppenpraxis Müller");
  }

  @Test
  void test_A_D1_legacyCrossReferenceIsRecognized() {
    Bundle bundle = createBundle("testfiles/Bundle-A-D1-P-C1-legacyCrossreference.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();
    assertEquals("232b5ecd-9028-4f50-9961-cf2bf47a2475", vaccinationDTO.getRelatedId());
  }

  @Test
  void test_A_D1_currentCrossReferenceIsRecognized() {
    Bundle bundle = createBundle("testfiles/Bundle-A-D1-P-C1-currentCrossreference.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();
    assertEquals("0faaf429-4f04-4324-b703-2dff77a53a90", vaccinationDTO.getRelatedId());
  }

  @Test
  void test_A_D1_xNotes() {
    Bundle bundle = createBundle("testfiles/Bundle-A-D1-P-C1-xNotes.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();
    assertThat(vaccinationDTO.getDoseNumber()).isEqualTo(1);
    assertThat(vaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
    assertThat(vaccinationDTO.getCode().getCode()).isEqualTo("558");
    assertThat(vaccinationDTO.getId()).isNotNull();
    assertThat(vaccinationDTO.getStatus().getCode()).isEqualTo("completed");
    assertNull(vaccinationDTO.getVerificationStatus());
    assertThat(vaccinationDTO.isValidated()).isFalse();

    assertThat(vaccinationDTO.getRecorder().getFirstName()).isEqualTo("Peter");
    assertThat(vaccinationDTO.getRecorder().getLastName()).isEqualTo("Müller");
    assertThat(vaccinationDTO.getRecorder().getPrefix()).isEqualTo("Dr. med.");

    assertThat(vaccinationDTO.getAuthor().getUser().getLastName()).isEqualTo("Wegmueller");
    assertThat(vaccinationDTO.getAuthor().getRole()).isEqualTo("PAT");

    assertNotNull(vaccinationDTO.getComment());
    assertThat(vaccinationDTO.getComment().getAuthor()).isEqualTo("Someone");
    assertThat(vaccinationDTO.getComment().getDate()).isEqualTo(LocalDateTime.of(2021, 6, 4, 0, 0));
    assertThat(vaccinationDTO.getComment().getText()).isEqualTo("""
    DDDD
    
    Dr. med. Peter Müller, 03.06.2021
    CCCC
    
    Monika Wegmueller, 02.06.2021
    BBBB
    
    Dr. med. Peter Müller, 01.06.2021
    AAAA""");
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    vaccinationDTO.setLotNumber("AHAVB946A_2");
    vaccinationDTO.setVerificationStatus(new ValueDTO("59156000", "Confirmed", "http://snomed.info/sct"));
    AuthorDTO author = vaccinationDTO.getAuthor();
    author.setRole("HCP");

    CommentDTO comment = new CommentDTO(null, null, "BlaBla");
    vaccinationDTO.setComment(comment);
    Bundle updatedBundle =
        fhirAdapter.update(patientIdentifier, vaccinationDTO, bundle, vaccinationDTO.getId());

    Composition composition = FhirUtils.getResource(Composition.class, updatedBundle);
    List<CompositionRelatesToComponent> relatesTo = composition.getRelatesTo();
    assertEquals("replaces", relatesTo.get(0).getCode().toCode());
    assertEquals("urn:uuid:b505b90a-f241-41ca-859a-b55a6103e753", relatesTo.get(0).getTargetReference().getReference());

    List<DomainResource> resources = fhirAdapter.getSectionResources(updatedBundle, SectionType.IMMUNIZATION);
    assertThat(resources.size()).isEqualTo(1);

    List<VaccinationDTO> updatedVaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, updatedBundle);
    assertThat(updatedVaccinations.size()).isEqualTo(1);
    assertThat(updatedVaccinations.getFirst().getLotNumber()).isEqualTo("AHAVB946A_2");
    assertNotNull(updatedVaccinations.getFirst().getComment());
    assertThat(updatedVaccinations.getFirst().getComment().getText()).isEqualTo("BlaBla");
    assertThat(updatedVaccinations.getFirst().getComment().getAuthor()).contains("Wegmueller");
    assertThat(updatedVaccinations.getFirst().getVerificationStatus().getCode()).contains("59156000");
    assertThat(updatedVaccinations.getFirst().getVerificationStatus().getName()).contains("Confirmed");
    assertThat(updatedVaccinations.getFirst().getVerificationStatus().getSystem()).contains("http://snomed.info/sct");
  }

  @Test
  void test_A_D2() {
    Bundle bundle = createBundle("testfiles/Bundle-A-D2-HCP1-C1.json");

    assertThat(bundle.getId()).isEqualTo("Bundle/A-D2-HCP1-C1");
    assertThat(bundle.getEntry().size()).isEqualTo(7);
    assertThat(bundle.getMeta().getProfile().getFirst().getValue()).isEqualTo(FhirConstants.META_VACCINATION_TYPE_URL);

    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();
    assertThat(vaccinationDTO.getTargetDiseases().size()).isEqualTo(3);
    assertThat(vaccinationDTO.getTargetDiseases().getFirst().getCode()).isEqualTo("397430003");
    assertThat(vaccinationDTO.getTargetDiseases().getFirst().getName())
        .isEqualTo("Diphtheria caused by Corynebacterium diphtheriae (disorder)");
    assertThat(vaccinationDTO.getTargetDiseases().getFirst().getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);
    assertThat(vaccinationDTO.getTargetDiseases().get(1).getCode()).isEqualTo("76902006");
    assertThat(vaccinationDTO.getTargetDiseases().get(1).getName()).isEqualTo("Tetanus (disorder)");
    assertThat(vaccinationDTO.getTargetDiseases().get(1).getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);
    assertThat(vaccinationDTO.getTargetDiseases().get(2).getCode()).isEqualTo("27836007");
    assertThat(vaccinationDTO.getTargetDiseases().get(2).getName()).isEqualTo("Pertussis (disorder)");
    assertThat(vaccinationDTO.getTargetDiseases().get(2).getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);
  }

  @Test
  void test_A_D3() {
    Bundle bundle = createBundle("testfiles/Bundle-A-D3-HCP2-C2.json");

    assertThat(bundle.getId()).isEqualTo("Bundle/A-D3-HCP2-C2");
    assertThat(bundle.getEntry().size()).isEqualTo(9);
    assertThat(bundle.getMeta().getProfile().getFirst().getValue()).isEqualTo(FhirConstants.META_VACCINATION_TYPE_URL);
  }

  @Test
  void test_A_D5_P_C1() {
    Bundle patientBundle = createBundle("testfiles/Bundle-A-D5-P-C1.json");

    assertThat(patientBundle.getId()).isEqualTo("Bundle/A-D5-P-C1");

    List<VaccinationDTO> vaccinationDTOs = fhirAdapter.getDTOs(VaccinationDTO.class, patientBundle);
    assertThat(vaccinationDTOs.size()).isEqualTo(1);

    assertThat(vaccinationDTOs.getFirst().getRecorder().getLastName()).isEqualTo("Meier");
    assertThat(vaccinationDTOs.getFirst().getAuthor().getUser().getLastName()).isEqualTo("Wegmueller");

    Bundle hcpBundle = createBundle("testfiles/Bundle-A-D6-HCP1-C1.json");
    assertThat(hcpBundle.getId()).isEqualTo("Bundle/A-D6-HCP1-C1");

    List<VaccinationDTO> vaccinationDTOs2 = fhirAdapter.getDTOs(VaccinationDTO.class, hcpBundle);
    assertThat(vaccinationDTOs.size()).isEqualTo(1);

    assertThat(vaccinationDTOs2.getFirst().getRecorder().getLastName()).isEqualTo("Meier");
    assertThat(vaccinationDTOs2.getFirst().getAuthor().getUser().getLastName()).isEqualTo("Müller");
  }

  @Test
  void test_AuthorIsHCP() {
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    patientIdentifier.setSpidExtension("12.34.56.78");
    VaccinationDTO vaccinationDTO = createVaccinationDTO();
    vaccinationDTO.getAuthor().setRole("HCP");
    vaccinationDTO.setReason(new ValueDTO("reasonCode", "reasonName", "reasonSystem"));

    // Bundle -> DTO
    FhirContext ctx = FhirContext.forR4();
    Bundle bundle = fhirConverter.createBundle(ctx, patientIdentifier, vaccinationDTO);

    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/Practitioner-0001").getNameFirstRep().getFamily())
        .isEqualTo("Frankenstein");
    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/Practitioner-author").getIdentifierFirstRep().getValue())
        .isEqualTo("gln:11.22.33.44");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getNameFirstRep().getFamily()).isEqualTo("Branagh");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getIdentifierFirstRep().getValue())
        .isEqualTo("12.34.56.78");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getIdentifierFirstRep().getSystem())
        .isEqualTo("urn:oid:2.16.756.5.30.1.127.3.10.3");
    assertThat(FhirUtils.getAuthor(bundle).getUser().getLastName()).isEqualTo("Doe");
    assertThat(FhirUtils.getAuthor(bundle).getRole()).isEqualTo("HCP");
    assertThat(FhirUtils.getConfidentiality(bundle).getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);
    assertThat(bundle.getMeta().getProfile().getFirst().getValue()).isEqualTo(FhirConstants.META_VACCINATION_TYPE_URL);

    String json = fhirAdapter.marshall(bundle);
    assertThat(json).contains("\"system\": \"urn:oid:2.16.756.5.30.1.127.3.10.3\"");
    assertThat(json).contains("\"value\": \"12.34.56.78\"");
    assertThat(json).contains(patientIdentifier.getLocalAssigningAuthority());
    assertThat(json).doesNotContain(patientIdentifier.getLocalExtenstion());

    // DTO -> Bundle
    VaccinationDTO targetDTO = fhirAdapter.getDTOs(VaccinationDTO.class, bundle).getFirst();
    assertThat(targetDTO.getRecorder().getLastName()).isEqualTo("Frankenstein");
    assertThat(targetDTO.getAuthor().getUser().getLastName()).isEqualTo("Doe");
    assertThat(targetDTO.getAuthor().getRole()).isEqualTo("HCP");
    assertThat(targetDTO.isValidated()).isTrue();
    assertThat(targetDTO.getReason().getName()).isEqualTo("reasonName");
    assertThat(targetDTO.getVerificationStatus().getName()).isEqualTo("Confirmed");
    assertThat(targetDTO.getVerificationStatus().getCode()).isEqualTo("59156000");
    assertThat(targetDTO.getVerificationStatus().getSystem()).isEqualTo("http://snomed.info/sct");
  }

  @Test
  void test_AuthorIsOtherPatient() {
    // patient Kenneth Branagh
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    patientIdentifier.setSpidExtension("12.34.56.78");
    // author John Doe
    VaccinationDTO vaccinationDTO = createVaccinationDTO();

    // Bundle -> DTO
    FhirContext ctx = FhirContext.forR4();
    Bundle bundle = fhirConverter.createBundle(ctx, patientIdentifier, vaccinationDTO);

    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/Practitioner-0001").getNameFirstRep().getFamily())
        .isEqualTo("Frankenstein");
    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/Practitioner-0001").getIdentifier().getFirst().getSystem())
        .isEqualTo("urn:oid:2.51.1.3");
    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/Practitioner-0001").getIdentifier().getFirst().getValue())
        .isEqualTo("7601007922000");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getNameFirstRep().getFamily()).isEqualTo("Branagh");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getIdentifierFirstRep().getValue())
        .isEqualTo("12.34.56.78");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getIdentifierFirstRep().getSystem())
        .isEqualTo("urn:oid:2.16.756.5.30.1.127.3.10.3");

    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-author").getNameFirstRep().getFamily()).isEqualTo("Doe");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-author").getIdentifierFirstRep().getValue())
        .isNotEqualTo("12.34.56.78");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-author").getIdentifierFirstRep().getSystem())
        .isEqualTo("urn:oid:2.16.756.5.30.1.127.3.10.3");
    assertThat(FhirUtils.getAuthor(bundle).getUser().getLastName()).isEqualTo("Doe");
    assertThat(FhirUtils.getAuthor(bundle).getRole()).isEqualTo("PAT");
    assertThat(FhirUtils.getOrganization(bundle, "Organization/Organization-0001").getName()).isEqualTo("-");

    // DTO -> Bundle
    VaccinationDTO targetDTO = fhirAdapter.getDTOs(VaccinationDTO.class, bundle).getFirst();
    assertThat(targetDTO.getRecorder().getLastName()).isEqualTo("Frankenstein");
    // TODO assertThat(targetDTO.getRecorder().getRole()).isEqualTo("HCP");
    assertThat(targetDTO.getAuthor().getUser().getLastName()).isEqualTo("Doe");
    assertThat(targetDTO.getAuthor().getRole()).isEqualTo("PAT");
    assertThat(targetDTO.isValidated()).isFalse();
  }

  @Test
  void test_AuthorIsPatient() {
    // patient Kenneth Branagh
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    VaccinationDTO vaccinationDTO = createVaccinationDTO();
    // author Kenneth Branagh
    vaccinationDTO.getAuthor().setUser(patientIdentifier.getPatientInfo());
    vaccinationDTO.getVerificationStatus().setName("Not confirmed");
    vaccinationDTO.getVerificationStatus().setCode("76104008");

    // Bundle -> DTO
    FhirContext ctx = FhirContext.forR4();
    Bundle bundle = fhirConverter.createBundle(ctx, patientIdentifier, vaccinationDTO);

    assertThat(
        FhirUtils.getResource(Immunization.class, bundle, "Immunization-0001").getMeta().getProfile().getFirst().getValue())
            .isEqualTo("http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-immunization");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getNameFirstRep().getFamily()).isEqualTo("Branagh");
    assertThat(FhirUtils.getAuthor(bundle).getUser().getLastName()).isEqualTo("Branagh");
    assertThat(FhirUtils.getAuthor(bundle).getRole()).isEqualTo("PAT");

    // DTO -> Bundle
    VaccinationDTO targetDTO = fhirAdapter.getDTOs(VaccinationDTO.class, bundle).getFirst();
    assertThat(targetDTO.getAuthor().getUser().getLastName()).isEqualTo("Branagh");
    assertThat(targetDTO.getAuthor().getRole()).isEqualTo("PAT");
    assertThat(targetDTO.isValidated()).isFalse();
    assertThat(targetDTO.getVerificationStatus().getCode()).isEqualTo("76104008");
    assertThat(targetDTO.getVerificationStatus().getName()).isEqualTo("Not confirmed");
    assertThat(targetDTO.getVerificationStatus().getSystem()).isEqualTo("http://snomed.info/sct");
  }

  @Test
  void test_B_D1() {
    Bundle bundle = createBundle("testfiles/Bundle-B-D1-HCP1-C1.json");
    assertThat(fhirAdapter.getDTOs(VaccinationDTO.class, bundle).size()).isEqualTo(2);
  }

  @Test
  void test_B_D2() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-B-D2-HCP1-C1.json");

    assertThat(bundle.getId()).isEqualTo("Bundle/B-D2-HCP1-C1");
    assertThat(bundle.getEntry().size()).isEqualTo(6);
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getId())
        .isEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getCode().getCode()).isEqualTo("294659004");
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getCriticality()).isNull();
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getRecorder().getFirstName()).isEqualTo("Peter");
  }

  @Test
  void test_B_D3() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-B-D3-HCP1-C1.json");
    assertThat(bundle.getId()).isEqualTo("Bundle/B-D3-HCP1-C1");

    List<PastIllnessDTO> pastIllnessDTOs = fhirAdapter.getDTOs(PastIllnessDTO.class, bundle);
    assertThat(pastIllnessDTOs.size()).isEqualTo(1);
    assertThat(pastIllnessDTOs.getFirst().getId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(pastIllnessDTOs.getFirst().getClinicalStatus().getCode()).isEqualTo("resolved");
    assertThat(pastIllnessDTOs.getFirst().getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(pastIllnessDTOs.getFirst().getCode().getCode()).isEqualTo("38907003");
    assertThat(pastIllnessDTOs.getFirst().getCode().getName()).isEqualTo("Varicella (disorder)");

    assertThat(pastIllnessDTOs.getFirst().getRecorder().getFirstName()).isEqualTo("Peter");
  }

  @Test
  void test_commentVaccination() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualByComparingTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();

    LocalDateTime now = LocalDateTime.now();
    HumanNameDTO author = new HumanNameDTO();
    author.setLastName("me");
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    CommentDTO comment = new CommentDTO(null, author.getFullName(), "BlaBla");
    vaccinationDTO.setComment(comment);
    vaccinationDTO.setVerificationStatus(new ValueDTO("76104008", "Not confirmed", "http://snomed.info/sct"));

    Bundle updatedBundle = fhirAdapter.update(patientIdentifier, vaccinationDTO, bundle, vaccinationDTO.getId());

    Immunization immunization = FhirUtils.getResource(Immunization.class, updatedBundle);

    assertThat(immunization.getNote().size()).isEqualTo(1);
    assertThat(immunization.getNoteFirstRep().getText()).isEqualTo("BlaBla");
    assertThat(immunization.getNoteFirstRep().getAuthorReference().getReference()).isEqualTo("Patient/Patient-author");
    assertThat(immunization.getNoteFirstRep().getTime()).isAfter(
        Date.from(now.minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant()));
    assertEquals("76104008",
        ((Coding) immunization
            .getExtensionByUrl("http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-verification-status")
            .getValue()).getCode());
    assertEquals("Not confirmed",
        ((Coding) immunization
            .getExtensionByUrl("http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-verification-status")
            .getValue()).getDisplay());
    assertEquals("http://snomed.info/sct",
        ((Coding) immunization
            .getExtensionByUrl("http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-verification-status")
            .getValue()).getSystem());
  }

  @Test
  void test_createAllergy() {
    Bundle originalBundle =
        fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-B-D2-HCP1-C1.json");

    AllergyDTO originalAllergyDTO = getFirstDTO(AllergyDTO.class, originalBundle);
    originalAllergyDTO.setConfidentiality(new ValueDTO("aa", "bb", "cc"));
    originalAllergyDTO.setType(new ValueDTO("intolerance", "", ""));

    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), patientIdentifier, originalAllergyDTO);
    assertThat(FhirUtils.getResource(AllergyIntolerance.class, bundle).getCategory().getFirst().getValue())
        .isEqualTo(AllergyIntoleranceCategory.MEDICATION);
    assertThat(FhirUtils.getResource(AllergyIntolerance.class, bundle).getType())
        .isEqualTo(AllergyIntolerance.AllergyIntoleranceType.INTOLERANCE);
    AllergyDTO allergyDTO = getFirstDTO(AllergyDTO.class, bundle);
    // new object was created so ID must no longer be equal!
    assertThat(allergyDTO.getId()).isNotEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(allergyDTO.getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(allergyDTO.getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(allergyDTO.getCode().getCode()).isEqualTo("294659004");
    assertThat(allergyDTO.getOrganization()).isEqualTo("Gruppenpraxis Müller");
    assertThat(allergyDTO.getCriticality()).isNull();

    isConfidentialityEqualTo(bundle, "aa", "cc", "bb");
  }

  @Test
  void test_createPastillness() {
    Bundle originalBundle =
        fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-B-D3-HCP1-C1.json");
    List<PastIllnessDTO> pastIllnessDTOs = fhirAdapter.getDTOs(PastIllnessDTO.class, originalBundle);
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), patientIdentifier, pastIllnessDTOs.getFirst());
    PastIllnessDTO pastIllnessDTO = getFirstDTO(PastIllnessDTO.class, bundle);
    // new object was created so ID must no longer be equal!
    assertThat(pastIllnessDTO.getId()).isNotEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(pastIllnessDTO.getClinicalStatus().getCode()).isEqualTo("resolved");
    assertThat(pastIllnessDTO.getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(pastIllnessDTO.getCode().getCode()).isEqualTo("38907003");
    assertThat(pastIllnessDTO.getCode().getName()).isEqualTo("Varicella (disorder)");
    assertThat(pastIllnessDTO.getOrganization()).isEqualTo("Gruppenpraxis Müller");
  }

  @Test
  void test_D_D3_HCP1_C1() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-D-D3-HCP1-C1.json");

    List<MedicalProblemDTO> dtos = fhirAdapter.getDTOs(MedicalProblemDTO.class, bundle);
    assertThat(dtos.size()).isEqualTo(1);
    assertThat(dtos.getFirst().getId()).isEqualTo("30327ea1-6893-4c65-896e-c32c394f1ec6");
    assertThat(dtos.getFirst().getCode().getCode()).isEqualTo("402196005");
    assertThat(dtos.getFirst().getCode().getName()).isEqualTo("Atopische Dermatitis im Kindesalter");
    assertThat(dtos.getFirst().getCode().getSystem())
        .isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);

    assertThat(dtos.getFirst().getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(dtos.getFirst().getClinicalStatus().getName()).isEqualTo("Active");
    assertThat(dtos.getFirst().getClinicalStatus().getSystem())
        .isEqualTo("http://terminology.hl7.org/CodeSystem/condition-clinical");

    assertThat(dtos.getFirst().getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(dtos.getFirst().getVerificationStatus().getName()).isEqualTo("Confirmed");
    assertThat(dtos.getFirst().getVerificationStatus().getSystem())
        .isEqualTo("http://terminology.hl7.org/CodeSystem/condition-ver-status");

    List<PastIllnessDTO> pastIllnessDTOs = fhirAdapter.getDTOs(PastIllnessDTO.class, bundle);
    assertThat(pastIllnessDTOs.size()).isEqualTo(1);
    assertThat(pastIllnessDTOs.getFirst().getId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(pastIllnessDTOs.getFirst().getCode().getCode()).isEqualTo("38907003");
    assertThat(pastIllnessDTOs.getFirst().getCode().getName()).isEqualTo("Varicella (disorder)");
    assertThat(pastIllnessDTOs.getFirst().getCode().getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);

    assertThat(pastIllnessDTOs.getFirst().getClinicalStatus().getCode()).isEqualTo("resolved");
    assertThat(pastIllnessDTOs.getFirst().getClinicalStatus().getName()).isEqualTo("Resolved");
    assertThat(pastIllnessDTOs.getFirst().getClinicalStatus().getSystem())
        .isEqualTo("http://terminology.hl7.org/CodeSystem/condition-clinical");

    assertThat(pastIllnessDTOs.getFirst().getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(pastIllnessDTOs.getFirst().getVerificationStatus().getName()).isEqualTo("Confirmed");
    assertThat(pastIllnessDTOs.getFirst().getVerificationStatus().getSystem())
        .isEqualTo("http://terminology.hl7.org/CodeSystem/condition-ver-status");
  }

  @Test
  void test_deleteVaccination() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    Bundle deletedBundle = fhirAdapter.delete(patientIdentifier, vaccinationDTO, bundle,
        vaccinationDTO.getId());
    Immunization immunization = FhirUtils.getResource(Immunization.class, deletedBundle);
    assertThat(immunization).isNotNull();
    assertThat(((Reference)immunization.getExtensionByUrl(
        "http://fhir.ch/ig/ch-core/StructureDefinition/ch-ext-author").getValue())
        .getReference()).isEqualTo("Patient/TC-patient");
    isEqualTo(immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-ext-entry-resource-cross-references")
        .getExtensionByUrl("entry"), "acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    isEqualTo(immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-ext-entry-resource-cross-references")
        .getExtensionByUrl("container"), "urn:uuid:b505b90a-f241-41ca-859a-b55a6103e753");
    assertThat(((CodeType) immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-ext-entry-resource-cross-references")
        .getExtensionByUrl("relationcode").getValue()).getCode()).isEqualTo("replaces");

    Composition composition = FhirUtils.getResource(Composition.class, deletedBundle);
    List<CompositionRelatesToComponent> relatesTo = composition.getRelatesTo();
    assertEquals("replaces", relatesTo.get(0).getCode().toCode());
    assertEquals("urn:uuid:b505b90a-f241-41ca-859a-b55a6103e753", relatesTo.get(0).getTargetReference().getReference());


    VaccinationDTO deletedVaccinationDTO = getFirstDTO(VaccinationDTO.class, deletedBundle);
    assertThat(deletedVaccinationDTO.isDeleted()).isTrue();
    assertThat(deletedVaccinationDTO.getRelatedId())
        .isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
  }

  @Test
  void test_getJsonFilenames() {
    assertThat(fhirAdapter.getJsonFilenames(CONFIG_TESTFILES_PATH).size()).isEqualTo(5);

    for (String jsonFilename : fhirAdapter.getJsonFilenames(CONFIG_TESTFILES_PATH)) {
      Bundle bundle = fhirAdapter.unmarshallFromFile(CONFIG_TESTFILES_PATH + "/" + jsonFilename);
      Patient patient = FhirUtils.getPatient(bundle, "Patient/Patient-0001");
      if (patient != null) {
        log.debug("{} {}", patient.getNameFirstRep().getFamily(),
            patient.getIdentifierFirstRep().getValue());
      }
    }
  }

  @Test
  void test_getSectionResources() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-D-D3-HCP1-C1.json");

    List<DomainResource> resources;
    resources = fhirAdapter.getSectionResources(bundle, SectionType.PAST_ILLNESSES);

    assertThat(resources.size()).isEqualTo(1);
    assertThat(resources.getFirst()).isInstanceOf(Condition.class);
    assertThat(resources.getFirst().getIdPart()).isEqualTo("TCB02-UNDILL1");


    resources = fhirAdapter.getSectionResources(bundle, SectionType.MEDICAL_PROBLEM);

    assertThat(resources.size()).isEqualTo(1);
    assertThat(resources.getFirst()).isInstanceOf(Condition.class);
    assertThat(resources.getFirst().getIdPart()).isEqualTo("TCB03-EXPRISK1");
  }

  @Test
  void test_patientIdentifierNull_genderAndBirthdateGeneratedBySystem() {
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    patientIdentifier.setPatientInfo(null);
    VaccinationDTO vaccinationDTO = createVaccinationDTO();
    vaccinationDTO.getAuthor().setRole("HCP");
    vaccinationDTO.setReason(new ValueDTO("reasonCode", "reasonName", "reasonSystem"));

    FhirContext ctx = FhirContext.forR4();
    Bundle bundle = fhirConverter.createBundle(ctx, patientIdentifier, vaccinationDTO);

    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getGender().getDisplay())
        .isEqualToIgnoringCase("unknown");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getBirthDate())
        .isEqualToIgnoringHours("1900-01-01");
  }

  @Test
  void test_toBundle_with_Patient() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO createdVaccinationDTO = vaccinations.getFirst();
    createdVaccinationDTO.setAuthor(createAuthor());
    FhirContext ctx = FhirContext.forR4();
    PatientIdentifier patientIdentifier = createPatientIdentifier("aaa", "bbb");

    Bundle createdBundle =
        fhirConverter.createBundle(ctx, patientIdentifier, createdVaccinationDTO);
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getFamily())
        .isEqualTo("Branagh");
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getGivenAsSingleString())
        .isEqualTo("Kenneth");
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getPrefixAsSingleString())
        .isEqualTo("Mr.");

    isConfidentialityEqualTo(createdBundle, "17621005", FhirConstants.SNOMED_SYSTEM_URL, "Normal");

    String json = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdBundle);
    log.debug("json:{}", json);

    vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, createdBundle);
    createdVaccinationDTO = vaccinations.getFirst();
    assertThat(createdVaccinationDTO.getDoseNumber()).isEqualTo(1);
    assertThat(createdVaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
    assertThat(createdVaccinationDTO.getCode().getCode()).isEqualTo("558");
    assertThat(createdVaccinationDTO.getId()).isNotNull();
    assertThat(createdVaccinationDTO.getStatus().getCode()).isEqualTo("completed");
    assertThat(createdVaccinationDTO.isValidated()).isFalse();

    assertThat(createdVaccinationDTO.getRecorder().getFirstName()).isEqualTo("Peter");
    assertThat(createdVaccinationDTO.getRecorder().getLastName()).isEqualTo("Müller");
    assertThat(createdVaccinationDTO.getRecorder().getPrefix()).isEqualTo("Dr. med.");
  }

  @Test
  @Deprecated // Patient should be always defined !
  void test_toBundle_without_patient() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();

    FhirContext ctx = FhirContext.forR4();
    PatientIdentifier patientIdentifier = createPatientIdentifier("aaa", "bbb");

    Bundle createdBundle =
        fhirConverter.createBundle(ctx, patientIdentifier, vaccinationDTO);
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getFamily())
        .isEqualTo("Branagh");
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getGivenAsSingleString())
        .isEqualTo("Kenneth");
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getPrefixAsSingleString())
        .isEqualTo("Mr.");

    vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, createdBundle);
    vaccinationDTO = vaccinations.getFirst();
    assertThat(vaccinationDTO.getDoseNumber()).isEqualTo(1);
    assertThat(vaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
    assertThat(vaccinationDTO.getCode().getCode()).isEqualTo("558");
    assertThat(vaccinationDTO.getId()).isNotNull();
    assertThat(vaccinationDTO.getStatus().getCode()).isEqualTo("completed");
    assertThat(vaccinationDTO.isValidated()).isFalse();

    assertThat(vaccinationDTO.getRecorder().getFirstName()).isEqualTo("Peter");
    assertThat(vaccinationDTO.getRecorder().getLastName()).isEqualTo("Müller");
    assertThat(vaccinationDTO.getRecorder().getPrefix()).isEqualTo("Dr. med.");
  }

  @Test
  void test_updateVaccination() {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO vaccinationDTO = vaccinations.getFirst();
    assertThat(vaccinationDTO.getAuthor().getUser().getLastName()).isEqualTo("Wegmueller");
    assertThat(vaccinationDTO.getVerificationStatus().getCode()).isEqualTo("59156000");
    assertThat(vaccinationDTO.getVerificationStatus().getName()).isEqualTo("Confirmed");
    assertThat(vaccinationDTO.getVerificationStatus().getSystem()).isEqualTo("http://snomed.info/sct");
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    Bundle updatedBundle = fhirAdapter.update(patientIdentifier, vaccinationDTO, bundle, vaccinationDTO.getId());
    Condition condition = FhirUtils.getResource(Condition.class, updatedBundle);
    assertThat(condition).isNull();
    Immunization immunization = FhirUtils.getResource(Immunization.class, updatedBundle);

    assertThat(vaccinationDTO.getComment().getText())
        .isEqualTo("Der Patient hat diese Impfung ohne jedwelcher nebenwirkungen gut vertragen.");
    assertThat(FhirUtils.getAuthor(updatedBundle).getUser().getLastName()).isEqualTo("Wegmueller");
    assertThat(((Reference)immunization.getExtensionByUrl(
        "http://fhir.ch/ig/ch-core/StructureDefinition/ch-ext-author").getValue())
        .getReference()).isEqualTo("Patient/TC-patient");
    isEqualTo(immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-ext-entry-resource-cross-references")
        .getExtensionByUrl("entry"), "acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    isEqualTo(immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-ext-entry-resource-cross-references")
        .getExtensionByUrl("container"), "urn:uuid:b505b90a-f241-41ca-859a-b55a6103e753");
    assertThat(((CodeType) immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-ext-entry-resource-cross-references")
        .getExtensionByUrl("relationcode").getValue()).getCode()).isEqualTo("replaces");

    Composition composition = FhirUtils.getResource(Composition.class, updatedBundle);
    List<CompositionRelatesToComponent> relatesTo = composition.getRelatesTo();
    assertEquals("replaces", relatesTo.get(0).getCode().toCode());
    assertEquals("urn:uuid:b505b90a-f241-41ca-859a-b55a6103e753", relatesTo.get(0).getTargetReference().getReference());

    vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, updatedBundle);
    VaccinationDTO updatedVaccinationDTO = vaccinations.getFirst();
    assertThat(updatedVaccinationDTO.isDeleted()).isFalse();
    assertThat(updatedVaccinationDTO.getRelatedId())
        .isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
  }

  @Test
  void unmarshallFromString_emptyBundle_returnNull() {
    assertNull(fhirAdapter.unmarshallFromString(""));
  }

  private AuthorDTO createAuthor() {
    return new AuthorDTO(new HumanNameDTO("John", "Doe", "Mr", null, null),
        null, null, null, "gln:11.22.33.44", null, null);
  }

  private Bundle createBundle(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource(filename).getFile());
    return fhirAdapter.unmarshallFromFile(file);
  }

  private PatientIdentifier createPatientIdentifier(String localAssigningAuthorityOid, String localId) {
    PatientIdentifier identifier = new PatientIdentifier(null, localId, localAssigningAuthorityOid);
    identifier.setPatientInfo(new HumanNameDTO("Kenneth", "Branagh", "Mr.", null, null));
    return identifier;
  }

  private VaccinationDTO createVaccinationDTO() {
    HumanNameDTO performer = new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null); // , null,
    AuthorDTO author = createAuthor();

    ValueDTO vaccineCode = new ValueDTO("123456789", "123456789", "testsystem");
    ValueDTO status = new ValueDTO("completed", "completed", "testsystem");
    ValueDTO verificationStatus = new ValueDTO("59156000", "Confirmed", "http://snomed.info/sct");
    ValueDTO targetDisease = new ValueDTO("38907003", "Varicella", "http://snomed.info/sct");
    VaccinationDTO vaccinationDTO =
        new VaccinationDTO(null, vaccineCode, List.of(targetDisease), null, 3, LocalDate.now(), performer, null,
            "lotNumber", null, status, verificationStatus);
    vaccinationDTO.setAuthor(author);
    return vaccinationDTO;
  }

  private <T extends BaseDTO> T getFirstDTO(Class<T> clazz, Bundle bundle) {
    return fhirAdapter.getDTOs(clazz, bundle).getFirst();
  }

  private VaccinationRecordDTO getVaccinationRecordFromTestfile(PatientIdentifier patientIdentifier) {
    Bundle originalBundle =
        fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-D-D3-HCP1-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, originalBundle);
    List<AllergyDTO> allergies = fhirAdapter.getDTOs(AllergyDTO.class, originalBundle);
    List<MedicalProblemDTO> medicalProblems = fhirAdapter.getDTOs(MedicalProblemDTO.class, originalBundle);
    List<PastIllnessDTO> pastIllnessDTOs = fhirAdapter.getDTOs(PastIllnessDTO.class, originalBundle);
    AuthorDTO author = createAuthor();

    return new VaccinationRecordDTO(author, patientIdentifier.getPatientInfo(),
        allergies, pastIllnessDTOs, vaccinations, medicalProblems);
  }

  private void isConfidentialityEqualTo(Bundle bundle, String code, String system, String name) {
    ValueDTO confidentiality = FhirUtils.getConfidentiality(bundle);

    assertThat(confidentiality.getCode()).isEqualTo(code);
    assertThat(confidentiality.getSystem()).isEqualTo(system);
    assertThat(confidentiality.getName()).isEqualTo(name);
  }

  private void isEqualTo(Extension extension, String value) {
    Reference reference = (Reference) extension.getValue();
    assertThat(reference.getIdentifier().getValue()).isEqualTo(value);
  }
}

