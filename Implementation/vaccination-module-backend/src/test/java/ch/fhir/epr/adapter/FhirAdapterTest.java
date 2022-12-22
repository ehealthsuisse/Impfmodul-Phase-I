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

import ca.uhn.fhir.context.FhirContext;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class FhirAdapterTest {
  private static final String CONFIG_TESTFILES_PATH = "config/testfiles";
  @Autowired
  private FhirAdapter fhirAdapter;
  @Autowired
  private FhirConverterIfc fhirConverter;

  @Test
  public void test_A_D1() throws Exception {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");

    assertThat(bundle.getId()).isEqualTo("Bundle/A-D1-P-C1");
    assertThat(bundle.getEntry().size()).isEqualTo(8);
    assertThat(fhirAdapter.getImmunization(bundle)).isNotNull();
    assertThat(fhirAdapter.getImmunization(bundle).getPerformerFirstRep().getActor().getReference())
        .isEqualTo("PractitionerRole/TC-HCP1-ORG1-ROLE-performer");

    assertThat(
        FhirUtils.getPractitionerRole(bundle, "PractitionerRole/TC-HCP1-ORG1-ROLE-performer")
            .getPractitioner().getReference()).isEqualTo("Practitioner/TC-HCP1-C1");

    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/TC-HCP1-C1").getNameFirstRep()
        .getFamily()).isEqualTo("Mueller");
    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/TC-HCP1-C1").getNameFirstRep()
        .getGivenAsSingleString()).isEqualTo("Peter");
    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/TC-HCP1-C1").getNameFirstRep()
        .getPrefixAsSingleString()).isEqualTo("Dr. med.");
    assertThat(FhirUtils.getAuthor(bundle).getLastName()).isEqualTo("Wegmueller");
    assertThat(FhirUtils.getAuthor(bundle).getRole()).isEqualTo("PAT");

    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.get(0);
    assertThat(vaccinationDTO.getDoseNumber()).isEqualTo(1);
    assertThat(vaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
    assertThat(vaccinationDTO.getVaccineCode().getCode()).isEqualTo("558");
    assertThat(vaccinationDTO.getId()).isNotNull();
    assertThat(vaccinationDTO.getStatus().getCode()).isEqualTo("completed");
    assertThat(vaccinationDTO.isValidated()).isFalse();

    assertThat(vaccinationDTO.getRecorder().getFirstName()).isEqualTo("Peter");
    assertThat(vaccinationDTO.getRecorder().getLastName()).isEqualTo("Mueller");
    assertThat(vaccinationDTO.getRecorder().getPrefix()).isEqualTo("Dr. med.");

    assertThat(vaccinationDTO.getAuthor().getLastName()).isEqualTo("Wegmueller");
    assertThat(vaccinationDTO.getAuthor().getRole()).isEqualTo("PAT");

    assertThat(vaccinationDTO.getComments().get(0).getText())
        .isEqualTo("Der Patient hat diese Impfung ohne jedwelcher nebenwirkungen gut vertragen.");
    assertThat(vaccinationDTO.getComments().get(0).getAuthor().getLastName()).isEqualTo("Mueller");
  }

  @Test
  public void test_A_D1_xNotes() throws Exception {
    Bundle bundle = createBundle("testfiles/Bundle-A-D1-P-C1-xNotes.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.get(0);
    assertThat(vaccinationDTO.getDoseNumber()).isEqualTo(1);
    assertThat(vaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
    assertThat(vaccinationDTO.getVaccineCode().getCode()).isEqualTo("558");
    assertThat(vaccinationDTO.getId()).isNotNull();
    assertThat(vaccinationDTO.getStatus().getCode()).isEqualTo("completed");
    assertThat(vaccinationDTO.isValidated()).isFalse();

    assertThat(vaccinationDTO.getRecorder().getFirstName()).isEqualTo("Peter");
    assertThat(vaccinationDTO.getRecorder().getLastName()).isEqualTo("Mueller");
    assertThat(vaccinationDTO.getRecorder().getPrefix()).isEqualTo("Dr. med.");

    assertThat(vaccinationDTO.getAuthor().getLastName()).isEqualTo("Wegmueller");
    assertThat(vaccinationDTO.getAuthor().getRole()).isEqualTo("PAT");

    assertThat(vaccinationDTO.getComments().size()).isEqualTo(4);
    assertThat(vaccinationDTO.getComments().get(3).getText()).isEqualTo("AAAA");
    assertThat(vaccinationDTO.getComments().get(2).getText()).isEqualTo("BBBB");
    assertThat(vaccinationDTO.getComments().get(1).getText()).isEqualTo("CCCC");
    assertThat(vaccinationDTO.getComments().get(0).getText()).isEqualTo("DDDD");
    assertThat(vaccinationDTO.getComments().get(3).getAuthor().getLastName()).isEqualTo("Mueller");
    assertThat(vaccinationDTO.getComments().get(2).getAuthor().getLastName()).isEqualTo("Wegmueller");
    assertThat(vaccinationDTO.getComments().get(1).getAuthor().getLastName()).isEqualTo("Mueller");
    assertThat(vaccinationDTO.getComments().get(0).getAuthor().getFirstName()).isEqualTo("Someone");

    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    vaccinationDTO.setLotNumber("AHAVB946A_2");
    vaccinationDTO.getAuthor().setRole("HCP");
    CommentDTO comment = new CommentDTO(null, null, "BlaBla");
    vaccinationDTO.setComments(List.of(comment));
    Bundle updatedBundle = fhirAdapter.update(patientIdentifier, vaccinationDTO, bundle);

    List<VaccinationDTO> updatedVaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, updatedBundle);
    assertThat(updatedVaccinations.get(0).getLotNumber()).isEqualTo("AHAVB946A_2");
    assertThat(updatedVaccinations.get(0).getComments().size()).isEqualTo(5);
    assertThat(updatedVaccinations.get(0).getComments().get(0).getText()).isEqualTo("BlaBla");
    assertThat(updatedVaccinations.get(0).getComments().get(0).getAuthor().getLastName()).isEqualTo("Wegmueller");
  }

  @Test
  public void test_A_D2() throws Exception {
    Bundle bundle = createBundle("testfiles/Bundle-A-D2-HCP1-C1.json");

    assertThat(bundle.getId()).isEqualTo("Bundle/A-D2-HCP1-C1");
    assertThat(bundle.getEntry().size()).isEqualTo(7);
    assertThat(fhirAdapter.getImmunization(bundle)).isNotNull();

    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.get(0);
    assertThat(vaccinationDTO.getTargetDiseases().size()).isEqualTo(3);
    assertThat(vaccinationDTO.getTargetDiseases().get(0).getCode()).isEqualTo("397430003");
    assertThat(vaccinationDTO.getTargetDiseases().get(0).getName())
        .isEqualTo("Diphtheria caused by Corynebacterium diphtheriae (disorder)");
    assertThat(vaccinationDTO.getTargetDiseases().get(0).getSystem()).isEqualTo("http://snomed.info/sct");
    assertThat(vaccinationDTO.getTargetDiseases().get(1).getCode()).isEqualTo("76902006");
    assertThat(vaccinationDTO.getTargetDiseases().get(1).getName()).isEqualTo("Tetanus (disorder)");
    assertThat(vaccinationDTO.getTargetDiseases().get(1).getSystem()).isEqualTo("http://snomed.info/sct");
    assertThat(vaccinationDTO.getTargetDiseases().get(2).getCode()).isEqualTo("27836007");
    assertThat(vaccinationDTO.getTargetDiseases().get(2).getName()).isEqualTo("Pertussis (disorder)");
    assertThat(vaccinationDTO.getTargetDiseases().get(2).getSystem()).isEqualTo("http://snomed.info/sct");
  }

  @Test
  public void test_A_D3() throws Exception {
    Bundle bundle = createBundle("testfiles/Bundle-A-D3-HCP2-C2.json");

    assertThat(bundle.getId()).isEqualTo("Bundle/A-D3-HCP2-C2");
    assertThat(bundle.getEntry().size()).isEqualTo(9);
    assertThat(fhirAdapter.getImmunization(bundle)).isNotNull();
  }

  @Test
  public void test_A_D5_P_C1() throws Exception {
    Bundle patientBundle = createBundle("testfiles/Bundle-A-D5-P-C1.json");

    assertThat(patientBundle.getId()).isEqualTo("Bundle/A-D5-P-C1");

    List<VaccinationDTO> vaccinationDTOs = fhirAdapter.getDTOs(VaccinationDTO.class, patientBundle);
    assertThat(vaccinationDTOs.size()).isEqualTo(1);

    assertThat(vaccinationDTOs.get(0).getRecorder().getLastName()).isEqualTo("Meier");
    assertThat(vaccinationDTOs.get(0).getAuthor().getLastName()).isEqualTo("Wegmueller");

    Bundle hcpBundle = createBundle("testfiles/Bundle-A-D6-HCP1-C1.json");
    assertThat(hcpBundle.getId()).isEqualTo("Bundle/A-D6-HCP1-C1");

    List<VaccinationDTO> vaccinationDTOs2 = fhirAdapter.getDTOs(VaccinationDTO.class, hcpBundle);
    assertThat(vaccinationDTOs.size()).isEqualTo(1);

    assertThat(vaccinationDTOs2.get(0).getRecorder().getLastName()).isEqualTo("Meier");
    assertThat(vaccinationDTOs2.get(0).getAuthor().getLastName()).isEqualTo("Mueller");
  }

  @Test
  public void test_AuthorIsHCP() throws Exception {
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
    assertThat(FhirUtils.getPractitioner(bundle, "Practitioner/Practitioner-0001").getIdentifierFirstRep().getValue())
        .isEqualTo("gln:11.22.33.44");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getNameFirstRep().getFamily()).isEqualTo("Branagh");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getIdentifierFirstRep().getValue())
        .isEqualTo("12.34.56.78");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getIdentifierFirstRep().getSystem())
        .isEqualTo("2.16.756.5.30.1.127.3.10.3");
    assertThat(FhirUtils.getAuthor(bundle).getLastName()).isEqualTo("Doe");
    assertThat(FhirUtils.getAuthor(bundle).getRole()).isEqualTo("HCP");
    assertThat(FhirUtils.getAuthor(bundle).getRole()).isEqualTo("HCP");

    String json = fhirAdapter.marshall(bundle);
    assertThat(json).contains("\"system\": \"2.16.756.5.30.1.127.3.10.3\"");
    assertThat(json).contains("\"value\": \"12.34.56.78\"");
    assertThat(json).doesNotContain(patientIdentifier.getLocalExtenstion());
    assertThat(json).doesNotContain(patientIdentifier.getLocalAssigningAuthority());

    // DTO -> Bundle
    VaccinationDTO targetDTO = fhirAdapter.getDTOs(VaccinationDTO.class, bundle).get(0);
    assertThat(targetDTO.getRecorder().getLastName()).isEqualTo("Frankenstein");
    assertThat(targetDTO.getAuthor().getLastName()).isEqualTo("Doe");
    assertThat(targetDTO.getAuthor().getRole()).isEqualTo("HCP");
    assertThat(targetDTO.isValidated()).isTrue();
    assertThat(targetDTO.getReason().getName()).isEqualTo("reasonName");
  }

  @Test
  public void test_AuthorIsOtherPatient() throws Exception {
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
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getNameFirstRep().getFamily()).isEqualTo("Branagh");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getIdentifierFirstRep().getValue())
        .isEqualTo("12.34.56.78");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getIdentifierFirstRep().getSystem())
        .isEqualTo("2.16.756.5.30.1.127.3.10.3");

    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-author").getNameFirstRep().getFamily()).isEqualTo("Doe");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-author").getIdentifierFirstRep().getValue())
        .isNotEqualTo("12.34.56.78");
    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-author").getIdentifierFirstRep().getSystem())
        .isEqualTo("urn:ietf:rfc:3986");
    assertThat(FhirUtils.getAuthor(bundle).getLastName()).isEqualTo("Doe");
    assertThat(FhirUtils.getAuthor(bundle).getRole()).isEqualTo("PAT");

    // DTO -> Bundle
    VaccinationDTO targetDTO = fhirAdapter.getDTOs(VaccinationDTO.class, bundle).get(0);
    assertThat(targetDTO.getRecorder().getLastName()).isEqualTo("Frankenstein");
    assertThat(targetDTO.getRecorder().getRole()).isEqualTo("HCP");
    assertThat(targetDTO.getAuthor().getLastName()).isEqualTo("Doe");
    assertThat(targetDTO.getAuthor().getRole()).isEqualTo("PAT");
    assertThat(targetDTO.isValidated()).isFalse();
  }

  @Test
  public void test_AuthorIsPatient() throws Exception {
    // patient Kenneth Branagh
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    VaccinationDTO vaccinationDTO = createVaccinationDTO();
    // author Kenneth Branagh
    vaccinationDTO.setAuthor(patientIdentifier.getPatientInfo());

    // Bundle -> DTO
    FhirContext ctx = FhirContext.forR4();
    Bundle bundle = fhirConverter.createBundle(ctx, patientIdentifier, vaccinationDTO);

    assertThat(FhirUtils.getPatient(bundle, "Patient/Patient-0001").getNameFirstRep().getFamily()).isEqualTo("Branagh");
    assertThat(FhirUtils.getAuthor(bundle).getLastName()).isEqualTo("Branagh");
    assertThat(FhirUtils.getAuthor(bundle).getRole()).isEqualTo("PAT");

    // DTO -> Bundle
    VaccinationDTO targetDTO = fhirAdapter.getDTOs(VaccinationDTO.class, bundle).get(0);
    assertThat(targetDTO.getAuthor().getLastName()).isEqualTo("Branagh");
    assertThat(targetDTO.getAuthor().getRole()).isEqualTo("PAT");
    assertThat(targetDTO.isValidated()).isFalse();
  }

  @Test
  public void test_B_D1() throws Exception {
    Bundle bundle = createBundle("testfiles/Bundle-B-D1-HCP1-C1.json");
    assertThat(fhirAdapter.getImmunization(bundle)).isNotNull();
    assertThat(fhirAdapter.getDTOs(VaccinationDTO.class, bundle).size()).isEqualTo(2);
  }

  @Test
  public void test_B_D2() throws Exception {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-B-D2-HCP1-C1.json");

    assertThat(bundle.getId()).isEqualTo("Bundle/B-D2-HCP1-C1");
    assertThat(bundle.getEntry().size()).isEqualTo(6);
    assertThat(fhirAdapter.getImmunization(bundle)).isNull();
    assertThat(fhirAdapter.getAllergyIntolerance(bundle)).isNotNull();

    assertThat(getFirstDTO(AllergyDTO.class, bundle).getId())
        .isEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getAllergyCode().getCode()).isEqualTo("294659004");
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getCriticality()).isNull();
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(getFirstDTO(AllergyDTO.class, bundle).getRecorder().getFirstName()).isEqualTo("Peter");
  }

  @Test
  public void test_B_D3() throws Exception {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-B-D3-HCP1-C1.json");
    assertThat(bundle.getId()).isEqualTo("Bundle/B-D3-HCP1-C1");

    List<PastIllnessDTO> pastIllnessDTOs = fhirAdapter.getDTOs(PastIllnessDTO.class, bundle);
    assertThat(pastIllnessDTOs.size()).isEqualTo(1);
    assertThat(pastIllnessDTOs.get(0).getId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(pastIllnessDTOs.get(0).getClinicalStatus().getCode()).isEqualTo("resolved");
    assertThat(pastIllnessDTOs.get(0).getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(pastIllnessDTOs.get(0).getIllnessCode().getCode()).isEqualTo("38907003");
    assertThat(pastIllnessDTOs.get(0).getIllnessCode().getName()).isEqualTo("Varicella (disorder)");

    assertThat(pastIllnessDTOs.get(0).getRecorder().getFirstName()).isEqualTo("Peter");
  }

  @Test
  public void test_commentVaccination() throws Exception {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    assertThat(vaccinations.size()).isEqualByComparingTo(1);
    VaccinationDTO vaccinationDTO = vaccinations.get(0);

    LocalDateTime now = LocalDateTime.now();
    HumanNameDTO author = new HumanNameDTO();
    author.setLastName("me");
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    CommentDTO comment = new CommentDTO(null, author, "BlaBla");
    vaccinationDTO.setComments(List.of(comment));

    Bundle updatedBundle = fhirAdapter.update(patientIdentifier, vaccinationDTO, bundle);
    Immunization immunization = FhirUtils.getResource(Immunization.class, updatedBundle);

    assertThat(immunization.getNote().size()).isEqualTo(2);
    assertThat(immunization.getNoteFirstRep().getText()).isEqualTo("BlaBla");
    assertThat(immunization.getNoteFirstRep().getAuthorReference().getReference()).isEqualTo("Patient/Patient-author");
    assertThat(immunization.getNoteFirstRep().getTime()).isAfter(
        Date.from(now.minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  public void test_createAllergy() throws Exception {
    Bundle originalBbundle =
        fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-B-D2-HCP1-C1.json");

    AllergyDTO originalAllergyDTO = getFirstDTO(AllergyDTO.class, originalBbundle);
    originalAllergyDTO.setConfidentiality(new ValueDTO("aa", "bb", "cc"));
    originalAllergyDTO.setType(new ValueDTO("intolerance", "", ""));

    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), patientIdentifier, originalAllergyDTO);;
    assertThat(FhirUtils.getResource(AllergyIntolerance.class, bundle).getCategory().get(0).getValue())
        .isEqualTo(AllergyIntoleranceCategory.MEDICATION);
    assertThat(FhirUtils.getResource(AllergyIntolerance.class, bundle).getType())
        .isEqualTo(AllergyIntolerance.AllergyIntoleranceType.INTOLERANCE);
    AllergyDTO allergyDTO = getFirstDTO(AllergyDTO.class, bundle);
    // new object was created so ID must no longer be equal!
    assertThat(allergyDTO.getId()).isNotEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(allergyDTO.getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(allergyDTO.getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(allergyDTO.getAllergyCode().getCode()).isEqualTo("294659004");
    assertThat(allergyDTO.getOrganization()).isEqualTo("Gruppenpraxis Mueller");
    assertThat(allergyDTO.getCriticality()).isNull();

    isConfidentialityEqualTo(bundle, "aa", "cc", "bb");
  }

  @Test
  public void test_createPastillness() throws Exception {
    Bundle originalBbundle =
        fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-B-D3-HCP1-C1.json");
    List<PastIllnessDTO> pastIllnessDTOs = fhirAdapter.getDTOs(PastIllnessDTO.class, originalBbundle);
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), patientIdentifier, pastIllnessDTOs.get(0));
    PastIllnessDTO pastIllnessDTO = getFirstDTO(PastIllnessDTO.class, bundle);
    // new object was created so ID must no longer be equal!
    assertThat(pastIllnessDTO.getId()).isNotEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(pastIllnessDTO.getClinicalStatus().getCode()).isEqualTo("resolved");
    assertThat(pastIllnessDTO.getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(pastIllnessDTO.getIllnessCode().getCode()).isEqualTo("38907003");
    assertThat(pastIllnessDTO.getIllnessCode().getName()).isEqualTo("Varicella (disorder)");
    assertThat(pastIllnessDTO.getOrganization()).isEqualTo("Gruppenpraxis Mueller");
  }

  @Test
  public void test_deleteVaccination() throws Exception {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO vaccinationDTO = vaccinations.get(0);
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    Bundle deletedBundle = fhirAdapter.delete(patientIdentifier, vaccinationDTO, bundle);
    Immunization immunization = FhirUtils.getResource(Immunization.class, deletedBundle);
    assertThat(immunization).isNotNull();
    isEqualTo(immunization.getExtensionByUrl(
        "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-immunization-recorder-reference"),
        "Patient/TC-patient");
    isEqualTo(immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference")
        .getExtensionByUrl("entry"), "acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    isEqualTo(immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference")
        .getExtensionByUrl("document"), "urn:uuid:b505b90a-f241-41ca-859a-b55a6103e753");
    assertThat(((CodeType) immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference")
        .getExtensionByUrl("relationcode").getValue()).getCode()).isEqualTo("replaces");

    Composition composition = FhirUtils.getResource(Composition.class, deletedBundle);
    assertThat(composition).isNotNull();
    assertThat(composition.getRelatesToFirstRep().getTargetReference().getReference())
        .isEqualTo("urn:uuid:b505b90a-f241-41ca-859a-b55a6103e753");

    VaccinationDTO deletedVaccinationDTO = getFirstDTO(VaccinationDTO.class, deletedBundle);
    assertThat(deletedVaccinationDTO.isDeleted()).isTrue();
    assertThat(deletedVaccinationDTO.getRelatedId())
        .isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
  }

  @Test
  public void test_getJsonFilenames() throws Exception {
    assertThat(fhirAdapter.getJsonFilenames(CONFIG_TESTFILES_PATH).size()).isEqualTo(3);

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
  public void test_toBundle_with_Patient() throws Exception {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO createdVaccinationDTO = vaccinations.get(0);
    createdVaccinationDTO.setAuthor(createAuthor());
    FhirContext ctx = FhirContext.forR4();
    PatientIdentifier patientIdentifier = createPatientIdentifier("aaa", "bbb");

    Bundle createdBundle = fhirConverter.createBundle(ctx, patientIdentifier, createdVaccinationDTO);
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getFamily())
        .isEqualTo("Branagh");
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getGivenAsSingleString())
        .isEqualTo("Kenneth");
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getPrefixAsSingleString())
        .isEqualTo("Mr.");

    isConfidentialityEqualTo(createdBundle, "17621005", FhirUtils.CONFIDENTIALITY_CODE_URL, "Normal");

    String json = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdBundle);
    log.debug("json:{}", json);

    vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, createdBundle);
    createdVaccinationDTO = vaccinations.get(0);
    assertThat(createdVaccinationDTO.getDoseNumber()).isEqualTo(1);
    assertThat(createdVaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
    assertThat(createdVaccinationDTO.getVaccineCode().getCode()).isEqualTo("558");
    assertThat(createdVaccinationDTO.getId()).isNotNull();
    assertThat(createdVaccinationDTO.getStatus().getCode()).isEqualTo("completed");
    assertThat(createdVaccinationDTO.isValidated()).isFalse();

    assertThat(createdVaccinationDTO.getRecorder().getFirstName()).isEqualTo("Peter");
    assertThat(createdVaccinationDTO.getRecorder().getLastName()).isEqualTo("Mueller");
    assertThat(createdVaccinationDTO.getRecorder().getPrefix()).isEqualTo("Dr. med.");
  }

  @Test
  @Deprecated // Patient should be always defined !
  public void test_toBundle_without_patient() throws Exception {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO vaccinationDTO = vaccinations.get(0);

    FhirContext ctx = FhirContext.forR4();
    PatientIdentifier patientIdentifier = createPatientIdentifier("aaa", "bbb");

    Bundle createdBundle = fhirConverter.createBundle(ctx, patientIdentifier, vaccinationDTO);
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getFamily())
        .isEqualTo("Branagh");
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getGivenAsSingleString())
        .isEqualTo("Kenneth");
    assertThat(FhirUtils.getPatient(createdBundle, "Patient/Patient-0001").getNameFirstRep().getPrefixAsSingleString())
        .isEqualTo("Mr.");

    vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, createdBundle);
    vaccinationDTO = vaccinations.get(0);
    assertThat(vaccinationDTO.getDoseNumber()).isEqualTo(1);
    assertThat(vaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
    assertThat(vaccinationDTO.getVaccineCode().getCode()).isEqualTo("558");
    assertThat(vaccinationDTO.getId()).isNotNull();
    assertThat(vaccinationDTO.getStatus().getCode()).isEqualTo("completed");
    assertThat(vaccinationDTO.isValidated()).isFalse();

    assertThat(vaccinationDTO.getRecorder().getFirstName()).isEqualTo("Peter");
    assertThat(vaccinationDTO.getRecorder().getLastName()).isEqualTo("Mueller");
    assertThat(vaccinationDTO.getRecorder().getPrefix()).isEqualTo("Dr. med.");
  }

  @Test
  public void test_updateVaccination() throws Exception {
    Bundle bundle = fhirAdapter.unmarshallFromFile("config/testfiles/Bundle-A-D1-P-C1.json");
    List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
    VaccinationDTO vaccinationDTO = vaccinations.get(0);
    assertThat(vaccinationDTO.getAuthor().getLastName()).isEqualTo("Wegmueller");
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");

    Bundle updatedBundle = fhirAdapter.update(patientIdentifier, vaccinationDTO, bundle);
    Condition condition = FhirUtils.getResource(Condition.class, updatedBundle);
    assertThat(condition).isNull();
    Immunization immunization = FhirUtils.getResource(Immunization.class, updatedBundle);

    assertThat(vaccinationDTO.getComments().get(0).getText())
        .isEqualTo("Der Patient hat diese Impfung ohne jedwelcher nebenwirkungen gut vertragen.");
    assertThat(FhirUtils.getAuthor(updatedBundle).getLastName()).isEqualTo("Wegmueller");
    isEqualTo(immunization.getExtensionByUrl(
        "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-immunization-recorder-reference"),
        "Patient/TC-patient");
    isEqualTo(immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference")
        .getExtensionByUrl("entry"), "acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    isEqualTo(immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference")
        .getExtensionByUrl("document"), "urn:uuid:b505b90a-f241-41ca-859a-b55a6103e753");
    assertThat(((CodeType) immunization
        .getExtensionByUrl(
            "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference")
        .getExtensionByUrl("relationcode").getValue()).getCode()).isEqualTo("replaces");

    Composition composition = FhirUtils.getResource(Composition.class, updatedBundle);
    assertThat(composition).isNotNull();
    assertThat(composition.getRelatesToFirstRep().getTargetReference().getReference())
        .isEqualTo("urn:uuid:b505b90a-f241-41ca-859a-b55a6103e753");

    vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, updatedBundle);
    VaccinationDTO updatedVaccinationDTO = vaccinations.get(0);
    assertThat(updatedVaccinationDTO.isDeleted()).isFalse();
    assertThat(updatedVaccinationDTO.getRelatedId())
        .isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
  }


  private HumanNameDTO createAuthor() {
    return new HumanNameDTO("Boris", "Karloff", "Herr", LocalDate.now(), "MALE");
  }

  private Bundle createBundle(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource(filename).getFile());
    Bundle bundle = fhirAdapter.unmarshallFromFile(file);
    return bundle;
  }

  private PatientIdentifier createPatientIdentifier(String localAssigningAuthorityOid, String localId) {
    PatientIdentifier identifier = new PatientIdentifier(null, localId, localAssigningAuthorityOid);
    identifier.setPatientInfo(new HumanNameDTO("Kenneth", "Branagh", "Mr.", null, null));
    return identifier;
  }

  private VaccinationDTO createVaccinationDTO() {
    HumanNameDTO performer = new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null, null, "gln:11.22.33.44");
    HumanNameDTO author = new HumanNameDTO("John", "Doe", "Mr", null, null);

    ValueDTO vaccineCode = new ValueDTO("123456789", "123456789", "testsystem");
    ValueDTO status = new ValueDTO("completed", "completed", "testsystem");
    VaccinationDTO vaccinationDTO =
        new VaccinationDTO(null, vaccineCode, null, null, 3, LocalDate.now(), performer, null, "lotNumber", null,
            status, true);
    vaccinationDTO.setAuthor(author);
    return vaccinationDTO;
  }

  private <T extends BaseDTO> T getFirstDTO(Class<T> clazz, Bundle bundle) {
    return fhirAdapter.getDTOs(clazz, bundle).get(0);
  }

  private void isConfidentialityEqualTo(Bundle bundle, String code, String system, String name) {
    Extension confidentiality =
        FhirUtils.getResource(Composition.class, bundle).getConfidentialityElement().getExtensionFirstRep();
    assertThat(confidentiality.getUrl())
        .isEqualTo("http://fhir.ch/ig/ch-core/StructureDefinition/ch-ext-epr-confidentialitycode");
    assertThat(confidentiality.getUrl())
        .isEqualTo("http://fhir.ch/ig/ch-core/StructureDefinition/ch-ext-epr-confidentialitycode");
    Coding coding = ((CodeableConcept) confidentiality.getValue()).getCodingFirstRep();

    assertThat(coding.getCode()).isEqualTo(code);
    assertThat(coding.getSystem()).isEqualTo(system);
    assertThat(coding.getDisplay()).isEqualTo(name);
  }

  private void isEqualTo(Extension extension, String value) {
    Reference reference = (Reference) extension.getValue();
    assertThat(reference.getReference()).isEqualTo(value);
  }
}

