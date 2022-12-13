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
package ch.admin.bag.vaccination.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Objects;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@AutoConfigureMockMvc
class WebConfigurationTest {

  private static final String ORIGIN = "http://localhost:9000";
  private static final String TEST_CORS = "/api/test-cors";

  private MockEnvironment mockEnv;
  private MockMvc mockMvc;
  private MockServletContext servletContext;
  private WebConfiguration webConfiguration;

  @BeforeEach
  void before() {
    servletContext = spy(new MockServletContext());
    doReturn(mock(FilterRegistration.Dynamic.class)).when(servletContext).addFilter(anyString(),
        any(Filter.class));
    doReturn(mock(ServletRegistration.Dynamic.class)).when(servletContext).addServlet(anyString(),
        any(Servlet.class));
    mockEnv = new MockEnvironment();

    webConfiguration = new WebConfiguration(mockEnv);
    ReflectionTestUtils.setField(webConfiguration, "frontendUrl", ORIGIN);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new TestCorsRestController())
        .addFilters(Objects.requireNonNull(webConfiguration.corsFilter()))
        .build();
  }

  @Test
  void delete_methodIsAllowed() throws Exception {
    mockMvc.perform(delete(TEST_CORS)
        .header(HttpHeaders.ORIGIN, ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
        .andExpect(status().isOk());
  }

  @Test
  void get_methodIsAllowed() throws Exception {
    mockMvc.perform(get(TEST_CORS)
        .header(HttpHeaders.ORIGIN, ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
        .andExpect(status().isOk());
  }

  @Test
  void post_methodIsAllowed() throws Exception {
    mockMvc.perform(post(TEST_CORS)
        .header(HttpHeaders.ORIGIN, ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
        .andExpect(status().isOk());
  }

  @Test
  void put_methodIsAllowed() throws Exception {
    mockMvc.perform(put(TEST_CORS)
        .header(HttpHeaders.ORIGIN, ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
        .andExpect(status().isOk());
  }

  @Test
  void startup_runWithProdProfile_noExceptionOccures() throws ServletException {
    mockEnv.setActiveProfiles("prod");
    assertThatCode(() -> webConfiguration.onStartup(servletContext)).doesNotThrowAnyException();
  }

}
