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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.saml.IdPAdapter;
import ch.admin.bag.vaccination.service.saml.SAMLService;
import ch.admin.bag.vaccination.service.saml.SAMLUtils;
import ch.admin.bag.vaccination.service.saml.SAMLUtilsTest;
import ch.admin.bag.vaccination.service.saml.SAMLXmlTestUtils;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 *
 * Test the {@link SAMLController}
 *
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SAMLControllerTest {
  private static final String SAML_ART = "1234567890";

  @Autowired
  private SAMLService samlService;

  @MockitoBean
  private ProfileConfig profileConfig;
  @MockitoBean
  private IdPAdapter idPAdapter;
  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void sendEmptySoapSamlLogoutRequest_expectException() {
    assertThrows(AssertionFailedError.class, () -> performLogout("saml/samlEmptySoapLogoutRequest.xml"));
  }

  @Test
  void sendSamlLogoutRequest_expectValidResponse() {
    performLogout("saml/samlLogoutRequest.xml");
  }

  @Test
  void sendSoapSamlLogoutRequest_expectValidResponse() {
    performLogout("saml/samlSoapLogoutRequest.xml");
  }

  @Test
  void sendSoapSamlLogoutRequest_forwardToken_expectValidResponse() {
    performLogout("saml/samlSoapLogoutRequest_forwarded.xml");
  }

  @Test
  void sendSoapSamlLogoutRequest_swissId_expectValidResponse() {
    performLogout("saml/samlLogoutRequestSwissId.xml");
  }

  @BeforeEach
  void setUp() {
    when(profileConfig.isSamlAuthenticationActive()).thenReturn(true);
  }

  @Test
  void ssoArtifactAndLogout() throws Exception {
    String xml = SAMLXmlTestUtils.xml("saml/samlArtifactResponse.xml");
    ArtifactResponse artifactResponse = (ArtifactResponse) SAMLUtils.unmarshall(xml);
    artifactResponse.setIssueInstant(Instant.now());
    when(idPAdapter.sendAndReceiveArtifactResolve(any(), any())).thenReturn(artifactResponse);

    assertThat(samlService.getNumberOfSessions()).isEqualTo(0);
    sendSamlArtifact();
    assertThat(samlService.getNumberOfSessions()).isEqualTo(1);
    performLogout("saml/samlLogoutRequest.xml");
    assertThat(samlService.getNumberOfSessions()).isEqualTo(0);
    performLogout("saml/samlLogoutRequest.xml");
    assertThat(samlService.getNumberOfSessions()).isEqualTo(0);
  }

  @Test
  void ssoLoginPost() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Origin", null);
    headers.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
    headers.add("Content-Type", "application/x-www-form-urlencoded");
    RequestEntity<String> entity =
        new RequestEntity<>("RelayState=GAZELLE&SAMLart=samlArtifact", headers, HttpMethod.POST,
            new URI("http://localhost:" + port + SAMLController.SSO_ENDPOINT));
    ResponseEntity<String> response = restTemplate.exchange(entity, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
  }

  private void performLogout(String requestFile) {
    String xml = SAMLXmlTestUtils.xml(requestFile);
    xml = SAMLUtilsTest.replaceInstantByNow(xml);
    HttpEntity<String> request = new HttpEntity<>(xml);
    ResponseEntity<String> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/saml/logout", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).contains(
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">");
  }

  private void sendSamlArtifact() throws Exception {
    RequestEntity<Void> entity = new RequestEntity<>(null, HttpMethod.GET,
        new URI("http://localhost:" + port + SAMLController.SSO_ENDPOINT_NEW + "?RelayState=GAZELLE&SAMLart="
            + SAML_ART));
    ResponseEntity<Void> response = restTemplate.exchange(entity, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
