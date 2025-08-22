/**
 * Copyright (c) 2024 eHealth Suisse
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
package ch.fhir.epr.adapter.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ValidationUtilsTest {
  private static final boolean IS_READ = true;

  @Test
  void testAllergyDTOValidity_invalidCases_ShouldThrowException() {
    ValueDTO allergy = new ValueDTO("213020009", "Allergy to egg protein", "http://snomed.info/sct");

    // Test with missing occurrenceDate
    AllergyDTO missingOccurrenceDate = new AllergyDTO();
    missingOccurrenceDate.setCode(allergy);
    missingOccurrenceDate.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingOccurrenceDate));

    // Test with a future occurrence date
    AllergyDTO futureOccurrenceDate = new AllergyDTO();
    futureOccurrenceDate.setOccurrenceDate(LocalDate.now().plusDays(1));
    futureOccurrenceDate.setCode(allergy);
    futureOccurrenceDate.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(futureOccurrenceDate));

    // Test with empty allergy name
    AllergyDTO emptyAllergyName = new AllergyDTO();
    emptyAllergyName.setOccurrenceDate(LocalDate.now());
    emptyAllergyName.setCode(new ValueDTO("213020009", "", "http://snomed.info/sct"));
    emptyAllergyName.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(emptyAllergyName));

    // Test with missing allergy code
    AllergyDTO missingAllergyCode = new AllergyDTO();
    missingAllergyCode.setOccurrenceDate(LocalDate.now());
    missingAllergyCode.setCode(new ValueDTO(null, "Allergy to egg protein", "http://snomed.info/sct"));
    missingAllergyCode.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingAllergyCode));

    // Test with missing recorder last name
    AllergyDTO missingRecFirstName = new AllergyDTO();
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setLastName("John");
    missingRecFirstName.setRecorder(humanNameDTO);
    missingRecFirstName.setOccurrenceDate(LocalDate.now());
    missingRecFirstName.setCode(allergy);
    missingRecFirstName.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingRecFirstName));
  }

  @Test
  void testMedicalProblemDTOValidity_invalidCases_ShouldThrowException() {
    ValueDTO medicalProblem = new ValueDTO("51244008", "Disorder of spleen (disorder)", "http://snomed.info/sct");

    // Test with missing recordedDate
    MedicalProblemDTO missingRecordedDate = new MedicalProblemDTO();
    missingRecordedDate.setCode(medicalProblem);
    missingRecordedDate.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingRecordedDate));

    // Test with a future recordedDate
    MedicalProblemDTO futureRecordedDate = new MedicalProblemDTO();
    futureRecordedDate.setRecordedDate(LocalDate.now().plusDays(2));
    futureRecordedDate.setBegin(LocalDate.now());
    futureRecordedDate.setCode(medicalProblem);
    futureRecordedDate.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(futureRecordedDate));

    // Test with a missing recorder last name
    MedicalProblemDTO missingRecFirstName = new MedicalProblemDTO();
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setFirstName("John");
    missingRecFirstName.setRecordedDate(LocalDate.now());
    missingRecFirstName.setRecorder(humanNameDTO);
    missingRecFirstName.setBegin(LocalDate.now());
    missingRecFirstName.setCode(medicalProblem);
    missingRecFirstName.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingRecFirstName));
  }

  @Test
  void testPastIllnessDTOValidity_invalidCases_ShouldThrowException() {
    ValueDTO pastIllness = new ValueDTO("52947006", "Japanese encephalitis", "http://snomed.info/sct");

    // Test with missing recordedDate
    PastIllnessDTO missingRecordedDate = new PastIllnessDTO();
    missingRecordedDate.setCode(pastIllness);
    missingRecordedDate.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingRecordedDate));

    // Test with begin date after recordedDate (in the future)
    PastIllnessDTO futureBeginDate = new PastIllnessDTO();
    futureBeginDate.setRecordedDate(LocalDate.now());
    futureBeginDate.setBegin(LocalDate.now().plusDays(3));
    futureBeginDate.setCode(pastIllness);
    futureBeginDate.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(futureBeginDate));

    // Test with a future recordedDate
    PastIllnessDTO futureRecordedDate = new PastIllnessDTO();
    futureRecordedDate.setRecordedDate(LocalDate.now().plusDays(3));
    futureRecordedDate.setBegin(LocalDate.now());
    futureRecordedDate.setCode(pastIllness);
    futureRecordedDate.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(futureRecordedDate));

    // Test with missing organization
    PastIllnessDTO missingOrganization = new PastIllnessDTO();
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setFirstName("John");
    humanNameDTO.setLastName("Doe");
    missingOrganization.setRecorder(humanNameDTO);
    missingOrganization.setRecordedDate(LocalDate.now());
    missingOrganization.setBegin(LocalDate.now());
    missingOrganization.setCode(pastIllness);
    assertDoesNotThrow(() -> ValidationUtils.isValid(missingOrganization));
  }

  @Test
  void testVaccinationDTOValidity_invalidCases_ShouldThrowException() {
    ValueDTO targetDisease = new ValueDTO("38907003", "Varicella", "http://snomed.info/sct");

    // Test with empty list of target diseases
    VaccinationDTO emptyTargetDiseases = new VaccinationDTO();
    emptyTargetDiseases.setTargetDiseases(new ArrayList<>());
    emptyTargetDiseases.setCode(targetDisease);
    emptyTargetDiseases.setDoseNumber(1);
    emptyTargetDiseases.setOccurrenceDate(LocalDate.now());
    emptyTargetDiseases.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(emptyTargetDiseases));

    // Test with null list of target diseases
    VaccinationDTO missingTargetDiseases = new VaccinationDTO();
    missingTargetDiseases.setCode(targetDisease);
    missingTargetDiseases.setDoseNumber(1);
    missingTargetDiseases.setOccurrenceDate(LocalDate.now());
    missingTargetDiseases.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingTargetDiseases));

    // Test with missing dose number
    VaccinationDTO missingDoseNumber = new VaccinationDTO();
    missingDoseNumber.setTargetDiseases(List.of(targetDisease));
    missingDoseNumber.setOccurrenceDate(LocalDate.now());
    missingDoseNumber.setCode(targetDisease);
    missingDoseNumber.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingDoseNumber));

    // Test with missing occurrence date
    VaccinationDTO missingOccurrenceDate = new VaccinationDTO();
    missingOccurrenceDate.setTargetDiseases(List.of(targetDisease));
    missingOccurrenceDate.setDoseNumber(1);
    missingOccurrenceDate.setCode(targetDisease);
    missingOccurrenceDate.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingOccurrenceDate));

    // Test with empty organization
    VaccinationDTO missingOrganization = new VaccinationDTO();
    missingOrganization.setTargetDiseases(List.of(targetDisease));
    missingOrganization.setOccurrenceDate(LocalDate.now());
    missingOrganization.setCode(targetDisease);
    missingOrganization.setOrganization("");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingOrganization));
  }

  // Test holds for all entities
  @Test
  void testValidity_read_missingFirstAndLastName_throwException() {
    MedicalProblemDTO missingAllName = createMedicalProblem();
    missingAllName.getRecorder().setFirstName(null);
    missingAllName.getRecorder().setLastName(null);
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(missingAllName));
  }

  // Test holds for all entities
  @Test
  void testValidity_validCaseRead_missingFirstOrLastName_allValid() {
    MedicalProblemDTO missingRecFirstName = createMedicalProblem();
    missingRecFirstName.getRecorder().setFirstName(null);
    assertDoesNotThrow(() -> ValidationUtils.isValid(missingRecFirstName, IS_READ));

    MedicalProblemDTO missingRecLastNameOnly = missingRecFirstName;
    missingRecFirstName.getRecorder().setFirstName("John");
    missingRecFirstName.getRecorder().setLastName(null);
    assertDoesNotThrow(() -> ValidationUtils.isValid(missingRecLastNameOnly, IS_READ));
  }

  @Test
  void testShouldRecordBeValidated_equalObjectsAndPATAuthor_onlyCommentChanged_shouldReturnTrue() {
    VaccinationDTO oldVaccination = createVaccinationDTO();
    oldVaccination.setValidated(true);
    oldVaccination.setAuthor(new AuthorDTO(new HumanNameDTO("Mike", "McDonald", "Dr.", null, null), "HCP", "GLN"));

    VaccinationDTO updatedVaccination = createVaccinationDTO();
    CommentDTO commentDTO = createCommentDTO();
    commentDTO.setText("updated comment");
    updatedVaccination.setComment(commentDTO);
    // verificationStatus should not be taken into account when checking equality
    updatedVaccination.setVerificationStatus(createValueDTO("59156000", "Confirmed", "http://snomed.info/sct"));
    assertTrue(ValidationUtils.shouldRecordBeValidated(oldVaccination, updatedVaccination));
  }

  @Test
  void testShouldRecordBeValidated_notEqualObjectsAndPATAuthor_commentAndDoseChanged_shouldReturnFalse() {
    VaccinationDTO oldVaccination = createVaccinationDTO();
    oldVaccination.setValidated(true);
    oldVaccination.setAuthor(new AuthorDTO(new HumanNameDTO("Mike", "McDonald", "Dr.", null, null), "HCP", "GLN"));

    VaccinationDTO updatedVaccination = createVaccinationDTO();
    updatedVaccination.setDoseNumber(2);
    CommentDTO commentDTO = createCommentDTO();
    commentDTO.setText("updated comment");
    updatedVaccination.setComment(commentDTO);
    assertFalse(ValidationUtils.shouldRecordBeValidated(oldVaccination, updatedVaccination));
  }

  @Test
  void testShouldRecordBeValidated_notEqualObjectsAndHCPAuthor_shouldReturnTrue() {
    AllergyDTO oldAllergy = createAllergyDTO();
    oldAllergy.setValidated(false);

    AllergyDTO updatedAllergy = createAllergyDTO();
    updatedAllergy.setCode(createValueDTO("219082005", "Cholera vaccine adverse reaction", "http://snomed.info/sct"));
    updatedAllergy.setVerificationStatus(createValueDTO("59156000", "Confirmed", "http://snomed.info/sct"));
    updatedAllergy.setAuthor(new AuthorDTO(new HumanNameDTO("Mike", "McDonald", "Dr.", null, null), "HCP", "GLN"));
    assertTrue(ValidationUtils.shouldRecordBeValidated(oldAllergy, updatedAllergy));
  }

  @Test
  void testShouldRecordBeValidated_equalObjectsOnlyLetterCaseDiffers_shouldReturnTrue() {
    AllergyDTO oldAllergy = createAllergyDTO();
    oldAllergy.setAuthor(new AuthorDTO(new HumanNameDTO("Mike", "McDonald", "Dr.", null, null), "HCP", "GLN"));
    oldAllergy.setValidated(true);

    AllergyDTO updatedAllergy = createAllergyDTO();
    updatedAllergy.setType(createValueDTO("allergy", "allergy", "http://hl7.org/fhir/allergy-intolerance-type"));
    updatedAllergy.setCode(createValueDTO("39579001", "anaphylactic reaction", "HTTP://snomed.info/sct"));
    updatedAllergy.setCriticality(createValueDTO("Unable-To-assess", "unable To assess risk", "HTTP://hl7.org/fhir/allergy-INTOLERANCE-criticality"));
    CommentDTO commentDTO = createCommentDTO();
    commentDTO.setText("updated comment");
    updatedAllergy.setComment(commentDTO);
    assertTrue(ValidationUtils.shouldRecordBeValidated(oldAllergy, updatedAllergy));
  }

  private AllergyDTO createAllergyDTO() {
    AllergyDTO allergyDTO = new AllergyDTO();
    allergyDTO.setAuthor(new AuthorDTO(new HumanNameDTO("John", "Doe", "Dr.", null, null),
        "PAT", "GLN"));
    allergyDTO.setCode(createValueDTO("39579001", "Anaphylactic reaction", "http://snomed.info/sct"));
    allergyDTO.setCriticality(createValueDTO("unable-to-assess", "Unable to Assess Risk",
        "http://hl7.org/fhir/allergy-intolerance-criticality"));
    allergyDTO.setVerificationStatus(createValueDTO("76104008", "Not confirmed", "http://snomed.info/sct"));
    allergyDTO.setType(createValueDTO("allergy", "Allergy", "http://hl7.org/fhir/allergy-intolerance-type"));
    return allergyDTO;
  }

  private CommentDTO createCommentDTO() {
    return new CommentDTO(null, "John Doe", "original comment");
  }

  // Test holds for all entities
  private MedicalProblemDTO createMedicalProblem() {
    ValueDTO medicalProblemCode = new ValueDTO("51244008", "Disorder of spleen (disorder)", "http://snomed.info/sct");
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setFirstName("John");
    humanNameDTO.setLastName("Doe");

    MedicalProblemDTO valideMedicalProblem = new MedicalProblemDTO();
    valideMedicalProblem.setRecordedDate(LocalDate.now());
    valideMedicalProblem.setBegin(LocalDate.now());
    valideMedicalProblem.setCode(medicalProblemCode);
    valideMedicalProblem.setOrganization("Gruppenpraxis CH");
    valideMedicalProblem.setRecorder(humanNameDTO);

    return valideMedicalProblem;
  }

  private VaccinationDTO createVaccinationDTO() {
    VaccinationDTO vaccinationDTO = new VaccinationDTO();
    vaccinationDTO.setAuthor(
        new AuthorDTO(new HumanNameDTO("John", "Doe", "Dr.", null, null), "PAT", "GLN"));
    vaccinationDTO.setDoseNumber(1);
    vaccinationDTO.setVerificationStatus(createValueDTO("76104008", "Not confirmed", "http://snomed.info/sct"));
    vaccinationDTO.setComment(createCommentDTO());
    vaccinationDTO.setCode(createValueDTO("38907003", "Varicella", "http://snomed.info/sct"));
    return vaccinationDTO;
  }

  private ValueDTO createValueDTO(String code, String name, String system) {
    return new ValueDTO(code, name, system);
  }
}