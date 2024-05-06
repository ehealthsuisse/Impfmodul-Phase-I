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
package ch.admin.bag.vaccination.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.fhir.epr.adapter.exception.TechnicalException;
import ch.fhir.epr.adapter.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private static final String ERROR_MESSAGE = "Error message";

  @Test
  void accessDeniedExceptionHandler_ValidationException_getHttpStatusCodeGetMessage() {
    AccessDeniedException vex = new AccessDeniedException(ERROR_MESSAGE, new Throwable(ERROR_MESSAGE));
    GlobalExceptionHandler geh = new GlobalExceptionHandler();

    ResponseEntity<String> resultGetMessageAndStatusCode = geh.accessDeniedExceptionHandler(vex);

    assertEquals(ERROR_MESSAGE, resultGetMessageAndStatusCode.getBody());
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resultGetMessageAndStatusCode.getStatusCode());
  }

  @Test
  void businessExceptionHandler_BusinessException_getHttpStatusCodeGetMessage() {
    BusinessException bexGetMessageAndStatusCode =
        new BusinessException(ERROR_MESSAGE, new Throwable(ERROR_MESSAGE));
    GlobalExceptionHandler geh = new GlobalExceptionHandler();

    ResponseEntity<String> resultGetMessageAndStatusCode =
        geh.businessExceptionHandler(bexGetMessageAndStatusCode);

    assertEquals(ERROR_MESSAGE, resultGetMessageAndStatusCode.getBody());
    assertEquals(HttpStatus.BAD_REQUEST, resultGetMessageAndStatusCode.getStatusCode());
  }

  @Test
  void technicalExceptionHandler_TechnicalException_getHttpStatusCodeAndMessageTemplate() {
    TechnicalException tex = new TechnicalException(ERROR_MESSAGE, new Throwable(ERROR_MESSAGE));
    GlobalExceptionHandler geh = new GlobalExceptionHandler();

    ResponseEntity<String> resultGetMessageAndStatusCode = geh.technicalExceptionHandler(tex);

    assertThat(resultGetMessageAndStatusCode.getBody()).contains(GlobalExceptionHandler.TECHNICAL_ERROR_MESSAGE);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resultGetMessageAndStatusCode.getStatusCode());
  }

  @Test
  void validationExceptionHandler_ValidationException_getHttpStatusCodeGetMessage() {
    ValidationException vex = new ValidationException(ERROR_MESSAGE, new Throwable(ERROR_MESSAGE));
    GlobalExceptionHandler geh = new GlobalExceptionHandler();

    ResponseEntity<String> resultGetMessageAndStatusCode = geh.validationExceptionHandler(vex);

    assertEquals(ERROR_MESSAGE, resultGetMessageAndStatusCode.getBody());
    assertEquals(HttpStatus.EXPECTATION_FAILED, resultGetMessageAndStatusCode.getStatusCode());
  }
}
