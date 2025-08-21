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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import ch.admin.bag.vaccination.service.SignatureService;
import ch.admin.bag.vaccination.service.VaccinationService;
import ch.admin.bag.vaccination.utils.MockSessionHelper;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.projecthusky.xua.saml2.impl.AssertionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
class BaseControllerTest extends MockSessionHelper {
  @LocalServerPort
  private int port;
  @MockitoBean
  private VaccinationService vaccinationService;
  @MockitoBean
  private SignatureService signatureService;
  @Autowired
  private TestRestTemplate restTemplate;

  @BeforeAll
  public void setUp() {
    restTemplate = new TestRestTemplate(HttpClientOption.ENABLE_COOKIES);
    enhanceSessionWithMockedData(restTemplate, signatureService, port, false);
  }

  @Test
  public void testCreate() {
    HttpEntity<VaccinationDTO> request = new HttpEntity<>(new VaccinationDTO());
    ResponseEntity<VaccinationDTO> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/vaccination/communityIdentifier/GAZELLE", request, VaccinationDTO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(vaccinationService).create(eq("GAZELLE"), eq("1.3.6.1.4.1.12559.11.20.1"), eq("CHPAM204"),
        any(VaccinationDTO.class), any(AssertionImpl.class), eq(false));
  }

  @Test
  public void testDelete() {
    ValueDTO confidentiality = new ValueDTO("1141000195107", "Secret", "url");
    HttpEntity<ValueDTO> request = new HttpEntity<>(confidentiality);
    ResponseEntity<VaccinationDTO> response = restTemplate.exchange("http://localhost:" + port
        + "/vaccination/communityIdentifier/EPDPLAYGROUND/uuid/acc1f090-5e0c-45ae-b283-521d57c3aa2f",
        HttpMethod.DELETE, request, VaccinationDTO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(vaccinationService).delete(eq("EPDPLAYGROUND"), eq("1.3.6.1.4.1.12559.11.20.1"), eq("CHPAM204"),
        eq("acc1f090-5e0c-45ae-b283-521d57c3aa2f"), eq(confidentiality), any(AssertionImpl.class));
  }

  @Test
  public void testGetAll() {
    ResponseEntity<Object> response = restTemplate.getForEntity(
        "http://localhost:" + port
            + "/vaccination/communityIdentifier/GAZELLE",
        Object.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(vaccinationService).getAll(eq("GAZELLE"), eq("1.3.6.1.4.1.12559.11.20.1"), eq("CHPAM204"),
        any(AssertionImpl.class),
        eq(false));
  }

  @Test
  public void testUpdate() {
    HttpEntity<VaccinationDTO> request = new HttpEntity<>(new VaccinationDTO());

    ResponseEntity<VaccinationDTO> response =
        restTemplate.postForEntity("http://localhost:" + port + "/vaccination/communityIdentifier/EPDPLAYGROUND",
        request, VaccinationDTO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(vaccinationService).create(eq("EPDPLAYGROUND"), eq("1.3.6.1.4.1.12559.11.20.1"), eq("CHPAM204"),
        any(VaccinationDTO.class), any(AssertionImpl.class), eq(false));
  }
}
