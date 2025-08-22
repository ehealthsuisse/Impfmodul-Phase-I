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

import ch.fhir.epr.adapter.FhirConstants;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.ValidationException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Class containing all validation utils
 */
@NoArgsConstructor
public class ValidationUtils {

  /**
   * Fully validates the dto, as if in writing mode
   *
   * @param dto {@link BaseDTO}T
   */
  public static void isValid(BaseDTO dto) {
    isValid(dto, false);
  }

  /**
   * Differenciate validation for read and write mode, especially for the recorder. In read mode, we
   * allow either first or last name to be empty.
   *
   * @param dto {@link BaseDTO}
   * @param isReadOrDelete true for read or delete operations
   */
  public static void isValid(BaseDTO dto, boolean isReadOrDelete) {
    ValidationUtils.validateValueDTO(dto.getCode());
    if (Objects.nonNull(dto.getRecorder())) {
      ValidationUtils.validateRecorder(dto.getRecorder(), isReadOrDelete);
    } else {
      ValidationUtils.isStringNotNullOrEmpty("organization", dto.getOrganization());
    }

    if (dto instanceof VaccinationDTO vaccination) {
      validateVaccination(vaccination);
    } else if (dto instanceof AllergyDTO allergy) {
      validateAllergy(allergy);
    } else if (dto instanceof PastIllnessDTO pastIllness) {
      validatePastIllness(pastIllness);
    } else if (dto instanceof MedicalProblemDTO medicalProblem) {
      validateMedicalProblem(medicalProblem);
    }
  }

  /**
   * Determines whether a record should be validated after an update.
   * <p>
   * A record should be validated if the updated object is otherwise equal to the old object
   * (i.e., all relevant fields excluding the comment are the same),
   * but the comment has changed and the original record was already validated by a HCP.
   * <p>
   * Covers also the case when a record previously not validated, will be validated by a HCP (when clicking Validate button)
   *
   * @param oldObject     the original {@code BaseDTO} instance before the update
   * @param updatedObject the new {@code BaseDTO} instance after the update
   * @return {@code true} if the updated record should be re-validated; {@code false} otherwise
   */
  public static Boolean shouldRecordBeValidated(BaseDTO oldObject, BaseDTO updatedObject) {
    if (isTrustedAuthor(updatedObject)) {
      return true;
    }

    if (!Objects.equals(oldObject, updatedObject)) {
      return false;
    }

    return hasCommentsChanged(oldObject, updatedObject) && oldObject.isValidated();
  }

  private static boolean areAllStringNullOrEmpty(List<String> values) {
    return values.stream().allMatch(value -> Objects.isNull(value) || StringUtils.isBlank(value));
  }

  private static boolean hasCommentsChanged(BaseDTO oldObject, BaseDTO updatedObject) {
    return !Objects.equals(oldObject.getComment(), updatedObject.getComment());
  }

  private static void isDateInTheFuture(String field, LocalDate date) {
    if (date.isAfter(LocalDate.now())) {
      throw new ValidationException("The field " + field + " should not be in the future.");
    }
  }

  private static void isDateNotNull(String field, LocalDate date) {
    if (Objects.isNull(date)) {
      throw new ValidationException("The field " + field + " should not be null.");
    }
  }

  private static <T> void isListNotNullOrEmpty(String field, List<T> list) {
    if (Objects.isNull(list)) {
      throw new ValidationException("The field " + field + " should not be null.");
    }
    if (list.isEmpty()) {
      throw new ValidationException("The field " + field + " should not be empty.");
    }
  }

  private static void isPositiveNumber(String field, Integer value) {
    if (Objects.isNull(value)) {
      throw new ValidationException("The field " + field + " should not be null.");
    }
    if (value < 1) {
      throw new ValidationException("The field " + field + " should be positive and greater than 0.");
    }
  }

  private static void isStringNotNullOrEmpty(String field, String text) {
    if (Objects.isNull(text)) {
      throw new ValidationException("The field " + field + " should not be null.");
    }
    if (StringUtils.isBlank(text)) {
      throw new ValidationException("The field " + field + " should not be empty.");
    }
  }

  private static boolean isTrustedAuthor(BaseDTO updatedObject) {
    return updatedObject.getAuthor() != null && Arrays.asList(FhirConstants.HCP, FhirConstants.ASS, FhirConstants.TCU)
        .contains(updatedObject.getAuthor().getRole());
  }

  private static void validateAllergy(AllergyDTO allergy) {
    ValidationUtils.isDateNotNull("occurrenceDate", allergy.getOccurrenceDate());
    ValidationUtils.isDateInTheFuture("occurrenceDate", allergy.getOccurrenceDate());
  }

  private static void validateMedicalProblem(MedicalProblemDTO medicalProblem) {
    ValidationUtils.isDateNotNull("recordedDate", medicalProblem.getRecordedDate());
    ValidationUtils.isDateNotNull("begin", medicalProblem.getBegin());
    ValidationUtils.isDateInTheFuture("recordedDate", medicalProblem.getRecordedDate());
  }

  private static void validatePastIllness(PastIllnessDTO pastIllness) {
    ValidationUtils.isDateNotNull("recordedDate", pastIllness.getRecordedDate());
    ValidationUtils.isDateNotNull("begin", pastIllness.getRecordedDate());
    ValidationUtils.isDateInTheFuture("recordedDate", pastIllness.getRecordedDate());
    ValidationUtils.isDateInTheFuture("begin", pastIllness.getBegin());
  }

  private static void validateRecorder(HumanNameDTO recorder, boolean isRead) {
    if (!isRead) {
      ValidationUtils.isStringNotNullOrEmpty("firstName", recorder.getFirstName());
    }

    if (!isRead) {
      ValidationUtils.isStringNotNullOrEmpty("lastName", recorder.getLastName());
    }

    if (isRead && areAllStringNullOrEmpty(Arrays.asList(recorder.getFirstName(), recorder.getLastName()))) {
      throw new ValidationException("There needs to be either first or last name set for a valid person.");
    }
  }

  private static void validateVaccination(VaccinationDTO vaccination) {
    ValidationUtils.isListNotNullOrEmpty("targetDiseases", vaccination.getTargetDiseases());
    vaccination.getTargetDiseases().forEach(ValidationUtils::validateValueDTO);
    ValidationUtils.isPositiveNumber("doseNumber", vaccination.getDoseNumber());
    ValidationUtils.isDateNotNull("occurrenceDate", vaccination.getOccurrenceDate());
  }

  private static void validateValueDTO(ValueDTO dto) {
    ValidationUtils.isStringNotNullOrEmpty("code", dto.getCode());
    ValidationUtils.isStringNotNullOrEmpty("name", dto.getName());
    ValidationUtils.isStringNotNullOrEmpty("system", dto.getSystem());
  }
}
