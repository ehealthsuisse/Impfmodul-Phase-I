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
package ch.fhir.epr.adapter.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TechnicalExceptionTest {

  private static final String EXCEPTION_MESSAGE = "This is an exception";

  @Test
  void constructor_messageAndCauseInput_gettersReturn() {
    Throwable ex = new IllegalArgumentException(EXCEPTION_MESSAGE);
    TechnicalException technicalExceptionSetMessageAndSetCause =
        new TechnicalException(EXCEPTION_MESSAGE, ex);

    assertEquals(EXCEPTION_MESSAGE, technicalExceptionSetMessageAndSetCause.getMessage());
    assertTrue(
        technicalExceptionSetMessageAndSetCause.getCause() instanceof IllegalArgumentException);
  }

  @Test
  void constructor_messageInput_gettersReturn() {
    TechnicalException technicalExceptionSetMessage =
        new TechnicalException(EXCEPTION_MESSAGE);

    assertEquals(EXCEPTION_MESSAGE, technicalExceptionSetMessage.getMessage());
  }

  @Test
  void constructor_noInput_gettersReturn() {
    TechnicalException technicalException = new TechnicalException();

    assertTrue(technicalException instanceof Throwable);
  }

}
