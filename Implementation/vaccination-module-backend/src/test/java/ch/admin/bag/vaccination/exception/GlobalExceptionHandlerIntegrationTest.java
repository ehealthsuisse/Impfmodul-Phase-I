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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {

  private static final String BUSINESS_EXCEPTION_URL = "/api/test/globalexception/business";
  private static final String TECHNICAL_EXCEPTION_URL = "/api/test/globalexception/technical";
  private static final String VALIDATION_EXCEPTION_URL = "/api/test/globalexception/validation";
  private static MockMvc mockMvc;

  @BeforeAll
  static void before() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new GlobalExceptionHandlerRestTestController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void throwBusinessException_receiveBadRequestHttpStatus() throws Exception {
    MockHttpServletResponse response = mockMvc.perform(
        get(BUSINESS_EXCEPTION_URL)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void throwTechnicalException_receiveInternalServerErrorHttpStatus() throws Exception {
    MockHttpServletResponse response = mockMvc.perform(
        get(TECHNICAL_EXCEPTION_URL)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  @Test
  void throwValidationException_receiveExpectationFailedHttpStatus() throws Exception {
    MockHttpServletResponse response = mockMvc.perform(
        get(VALIDATION_EXCEPTION_URL)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.EXPECTATION_FAILED.value());
  }

}
