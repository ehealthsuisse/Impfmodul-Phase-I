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

import ch.fhir.epr.adapter.data.dto.AllergyDTO;
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

    // Test with a missing recorder first name
    MedicalProblemDTO missingRecFirstName = new MedicalProblemDTO();
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setFirstName("John");
    missingRecFirstName.setRecordedDate(LocalDate.now().plusDays(2));
    missingRecFirstName.setRecorder(humanNameDTO);
    missingRecFirstName.setBegin(LocalDate.now());
    missingRecFirstName.setCode(medicalProblem);
    missingRecFirstName.setOrganization("Gruppenpraxis CH");
    assertThrows(ValidationException.class, () -> ValidationUtils.isValid(futureRecordedDate));
  }
}
