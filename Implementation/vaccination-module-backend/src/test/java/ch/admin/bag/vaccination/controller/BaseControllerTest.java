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

import ch.admin.bag.vaccination.service.VaccinationService;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import org.junit.jupiter.api.Test;
import org.projecthusky.xua.saml2.impl.AssertionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BaseControllerTest {
  @LocalServerPort
  private int port;
  @Autowired
  private TestRestTemplate restTemplate;
  @MockBean
  private VaccinationService vaccinationService;

  @Test
  public void testCreate() {
    HttpEntity<VaccinationDTO> request = new HttpEntity<>(new VaccinationDTO());

    ResponseEntity<VaccinationDTO> response = restTemplate.postForEntity(
        "http://localhost:" + port
            + "/vaccination/communityIdentifier/GAZELLE/oid/1.3.6.1.4.1.21367.13.20.3000/localId/CHPAM4489",
        request, VaccinationDTO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(vaccinationService).create(eq("GAZELLE"), eq("1.3.6.1.4.1.21367.13.20.3000"), eq("CHPAM4489"),
        any(VaccinationDTO.class), any(AssertionImpl.class));
  }

  @Test
  public void testDelete() {
    ValueDTO confidentiality = new ValueDTO("1141000195107", "Secret", "url");
    HttpEntity<ValueDTO> request = new HttpEntity<>(confidentiality);
    ResponseEntity<VaccinationDTO> response = restTemplate.exchange("http://localhost:" + port
        + "/vaccination/communityIdentifier/EPDPLAYGROUND/oid/1.2.3.4.123456.1/localId/waldspital-Id-1234/uuid/acc1f090-5e0c-45ae-b283-521d57c3aa2f",
        HttpMethod.DELETE, request, VaccinationDTO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(vaccinationService).delete(eq("EPDPLAYGROUND"), eq("1.2.3.4.123456.1"), eq("waldspital-Id-1234"),
        eq("acc1f090-5e0c-45ae-b283-521d57c3aa2f"), eq(confidentiality), any(AssertionImpl.class));
  }

  @Test
  public void testGetAll() {
    ResponseEntity<Object> response = restTemplate.getForEntity(
        "http://localhost:" + port
            + "/vaccination/communityIdentifier/GAZELLE/oid/1.3.6.1.4.1.21367.13.20.3000/localId/CHPAM4489",
        Object.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(vaccinationService).getAll(eq("GAZELLE"), eq("1.3.6.1.4.1.21367.13.20.3000"), eq("CHPAM4489"),
        any(AssertionImpl.class),
        eq(false));
  }

  @Test
  public void testUpdate() {
    HttpEntity<VaccinationDTO> request = new HttpEntity<>(new VaccinationDTO());

    ResponseEntity<VaccinationDTO> response = restTemplate.postForEntity(
        "http://localhost:" + port
            + "/vaccination/communityIdentifier/EPDPLAYGROUND/oid/1.2.3.4.123456.1/localId/waldspital-Id-1234",
        request, VaccinationDTO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(vaccinationService).create(eq("EPDPLAYGROUND"), eq("1.2.3.4.123456.1"), eq("waldspital-Id-1234"),
        any(VaccinationDTO.class), any(AssertionImpl.class));
  }

}
