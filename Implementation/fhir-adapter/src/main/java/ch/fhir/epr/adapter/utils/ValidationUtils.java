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

import ch.fhir.epr.adapter.exception.ValidationException;
import java.time.LocalDate;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * Class containing all validation utils
 */
@NoArgsConstructor
public class ValidationUtils {

  public static int isPositiveNumber(String field, int value) {
    if (value < 1) {
      throw new ValidationException("The field " + field + " should be positive and greater than 0");
    }
    return value;
  }

  public static LocalDate isDateNotNull(String field, LocalDate date) {
    if (Objects.isNull(date)) {
      throw new ValidationException("The field " + field + " should not be null");
    }
    return date;
  }
}
