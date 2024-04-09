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

import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.ValidationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Class containing all validation utils
 */
@NoArgsConstructor
public class ValidationUtils {

  public static void isValid(BaseDTO dto) {
    ValidationUtils.validateValueDTO(dto.getCode());
    if (Objects.nonNull(dto.getRecorder())) {
      ValidationUtils.validateRecorder(dto.getRecorder());
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

  private static void validateVaccination(VaccinationDTO vaccination) {
    ValidationUtils.isListNotNullOrEmpty("targetDiseases", vaccination.getTargetDiseases());
    vaccination.getTargetDiseases().forEach(ValidationUtils::validateValueDTO);
    ValidationUtils.isPositiveNumber("doseNumber", vaccination.getDoseNumber());
    ValidationUtils.isDateNotNull("occurrenceDate", vaccination.getOccurrenceDate());
  }

  private static void validateAllergy(AllergyDTO allergy) {
    ValidationUtils.isDateNotNull("occurrenceDate", allergy.getOccurrenceDate());
    ValidationUtils.isDateInTheFuture("occurrenceDate", allergy.getOccurrenceDate());
  }

  private static void validatePastIllness(PastIllnessDTO pastIllness) {
    ValidationUtils.isDateNotNull("recordedDate", pastIllness.getRecordedDate());
    ValidationUtils.isDateNotNull("begin", pastIllness.getRecordedDate());
    ValidationUtils.isDateInTheFuture("recordedDate", pastIllness.getRecordedDate());
    ValidationUtils.isDateInTheFuture("begin", pastIllness.getBegin());
  }

  private static void validateMedicalProblem(MedicalProblemDTO medicalProblem) {
    ValidationUtils.isDateNotNull("recordedDate", medicalProblem.getRecordedDate());
    ValidationUtils.isDateNotNull("begin", medicalProblem.getBegin());
    ValidationUtils.isDateInTheFuture("recordedDate", medicalProblem.getRecordedDate());
  }

  private static void isPositiveNumber(String field, Integer value) {
    if (Objects.isNull(value)) {
      throw new ValidationException("The field " + field + " should not be null");
    }
    if (value < 1) {
      throw new ValidationException("The field " + field + " should be positive and greater than 0");
    }
  }

  private static void isDateNotNull(String field, LocalDate date) {
    if (Objects.isNull(date)) {
      throw new ValidationException("The field " + field + " should not be null");
    }
  }

  private static void isDateInTheFuture(String field, LocalDate date) {
    if (date.isAfter(LocalDate.now())) {
      throw new ValidationException("The field " + field + " should not be in the future");
    }
  }

  private static void isStringNotNullOrEmpty(String field, String text) {
    if (Objects.isNull(text)) {
      throw new ValidationException("The field " + field + " should not be null");
    }
    if (StringUtils.isBlank(text)) {
      throw new ValidationException("The filed " + field + " should not be empty");
    }
  }

  private static <T> void isListNotNullOrEmpty(String field, List<T> list) {
    if (Objects.isNull(list)) {
      throw new ValidationException("The field " + field + " should not be null");
    }
    if (list.isEmpty()) {
      throw new ValidationException("The field " + field + " should not be empty");
    }
  }

  private static void validateValueDTO(ValueDTO dto) {
    ValidationUtils.isStringNotNullOrEmpty("code", dto.getCode());
    ValidationUtils.isStringNotNullOrEmpty("name", dto.getName());
    ValidationUtils.isStringNotNullOrEmpty("system", dto.getSystem());
  }

  private static void validateRecorder(HumanNameDTO recorder) {
    ValidationUtils.isStringNotNullOrEmpty("firstName", recorder.getFirstName());
    ValidationUtils.isStringNotNullOrEmpty("lastName", recorder.getLastName());
  }
}
