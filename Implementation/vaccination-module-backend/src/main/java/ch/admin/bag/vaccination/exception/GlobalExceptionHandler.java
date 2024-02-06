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

import ch.fhir.epr.adapter.exception.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles all exceptions going to the frontend.
 *
 * Currently, 3 different kinds are supported.
 * <ul>
 * <li>BusinessException: Business constraint was broken regarding the EPD
 * <li>TechnicalException: Unexpected technical error, usually caused by wrong configuration or some
 * environment issues.
 * <li>ValidationException: The input did not match the expectation.
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  static final String TECHNICAL_ERROR_MESSAGE = "A technical error occured. Please check internal logs for details at ";

  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<String> businessExceptionHandler(BusinessException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  /**
   * This handler will take care of the technical exceptions and any other upcoming runtime exceptions
   */
  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<String> technicalExceptionHandler(RuntimeException ex) {
    log.error(ex.getMessage());
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    String errorMessage = TECHNICAL_ERROR_MESSAGE + now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".";
    return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ValidationException.class)
  @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
  public ResponseEntity<String> validationExceptionHandler(ValidationException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.EXPECTATION_FAILED);
  }

}
