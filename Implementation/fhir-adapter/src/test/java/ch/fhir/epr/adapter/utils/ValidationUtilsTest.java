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
package ch.fhir.epr.adapter.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.fhir.epr.adapter.exception.ValidationException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class ValidationUtilsTest {
  @Test
  void testDoseNumberNegativeValue_shouldThrowException() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> ValidationUtils.isPositiveNumber("doseNumber", -1));

    assertTrue(exception.getMessage().contains("The field doseNumber should be positive and greater than 0"));
  }

  @Test
  void testDoseNumberZero_shouldThrowException() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> ValidationUtils.isPositiveNumber("doseNumber", 0));

    assertTrue(exception.getMessage().contains("The field doseNumber should be positive and greater than 0"));
  }

  @Test
  void testDoseNumberPositiveAndGreaterThanZero_shouldReturnTheNumber() {
    int doseNumber = ValidationUtils.isPositiveNumber("doseNumber", 11);
    assertEquals(11, doseNumber);
  }

  @Test
  void testRecordedDate_nullValue_shouldThrowException() {
    ValidationException exception = assertThrows(ValidationException.class,
        () -> ValidationUtils.isDateNotNull("recordedDate", null));

    assertTrue(exception.getMessage().contains("The field recordedDate should not be null"));
  }

  @Test
  void testRecordedDate_validValue_shouldReturnValue() {
    LocalDate recordedDate = ValidationUtils.isDateNotNull("recordedDate", LocalDate.of(2022, 10, 10));
    assertEquals(LocalDate.of(2022, 10, 10), recordedDate);
  }
}
