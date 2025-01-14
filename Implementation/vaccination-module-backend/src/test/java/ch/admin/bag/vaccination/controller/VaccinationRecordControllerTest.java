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
package ch.admin.bag.vaccination.controller;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.SignatureService;
import ch.admin.bag.vaccination.utils.MockSessionHelper;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public class VaccinationRecordControllerTest extends MockSessionHelper {
  @LocalServerPort
  private int port;
  @MockBean
  private SignatureService signatureService;
  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private ProfileConfig profileConfig;
  private List<String> cookies;

  @BeforeAll
  void setUp() {
    profileConfig.setLocalMode(true);
    profileConfig.setHuskyLocalMode(null);
    restTemplate = new TestRestTemplate(HttpClientOption.ENABLE_COOKIES);
    cookies = enhanceSessionWithMockedData(restTemplate, signatureService, port, true);
  }

  @Test
  void testCreateVaccinationRecordPdf_noExceptionOccurs() throws Exception {
    ValueDTO tuberkulose = new ValueDTO("56717001", "Tuberkulose", "testsystem");
    ValueDTO herpes = new ValueDTO("4740000", "Herpes zoster", "testsystem");
    ValueDTO masern = new ValueDTO("14189004", "Masern", "testsystem");

    // Call VaccinationRecordController's /name endpoint to enhance the Session with the patient info
    ResponseEntity<String> entity =
        restTemplate.getForEntity("http://localhost:" + port + "/vaccinationRecord/communityIdentifier/GAZELLE/name",
            String.class);
    assertThat(entity.getBody()).isEqualTo("Max Mustermann");

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.put(HttpHeaders.COOKIE, cookies);

    HttpEntity<List<ValueDTO>> httpEntity = new HttpEntity<>(List.of(tuberkulose, herpes, masern), httpHeaders);
    // Call the /exportToPDF endpoint using the updated Session object from previous requests
    ResponseEntity<Resource> response =
        restTemplate.postForEntity("http://localhost:" + port + "/vaccinationRecord/exportToPDF/en", httpEntity,
            Resource.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    Resource resource = response.getBody();
    InputStream stream = resource.getInputStream();

    File pdfFile = new File("exportToPDF.pdf");

    Files.copy(stream, pdfFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

    IOUtils.closeQuietly(stream);
  }

  @Test
  public void testGetAll() {
    ResponseEntity<Object> response =
        restTemplate.getForEntity("http://localhost:" + port + "/vaccinationRecord/communityIdentifier/GAZELLE",
            Object.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
