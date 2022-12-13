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
import static org.mockito.Mockito.when;
import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.saml.IdPAdapter;
import ch.admin.bag.vaccination.service.saml.SAMLService;
import ch.admin.bag.vaccination.service.saml.SAMLUtils;
import ch.admin.bag.vaccination.service.saml.SAMLUtilsTest;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * 
 * Test the {@link SAMLController}
 *
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SAMLControllerTest {

  private static final String IDP_COOKIE = "idpIdentifier=GAZELLE";
  private static final String SAML_ART = "1234567890";

  @Autowired
  private SAMLService samlService;
  @MockBean
  private ProfileConfig profileConfig;
  @MockBean
  private IdPAdapter idPAdapter;
  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void ssoArtifactAndLogout() throws Exception {
    String xml = SAMLUtils.xml("saml/samlArtifactResponse.xml");
    ArtifactResponse artifactResponse = (ArtifactResponse) SAMLUtils.unmarshall(xml);
    artifactResponse.setIssueInstant(Instant.now());
    when(idPAdapter.sendAndReceiveArtifactResolve(any(), any())).thenReturn(artifactResponse);

    assertThat(samlService.getSessionNumber()).isEqualTo(0);
    sendSamlArtifact(SAML_ART);
    assertThat(samlService.getSessionNumber()).isEqualTo(1);
    sendSamlLogoutRequest();
    assertThat(samlService.getSessionNumber()).isEqualTo(0);
    sendSamlLogoutRequest();
    assertThat(samlService.getSessionNumber()).isEqualTo(0);
  }

  private HttpHeaders createCookieHeader() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, IDP_COOKIE);

    return headers;
  }

  private void sendSamlArtifact(String samlArtifact) throws Exception {
    RequestEntity<Void> entity = new RequestEntity<>(createCookieHeader(), HttpMethod.GET,
        new URI("http://localhost:" + port + SAMLController.SSO_ENDPOINT + "?SAMLart=" + samlArtifact));
    ResponseEntity<Void> response = restTemplate.exchange(entity, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
  }

  private void sendSamlLogoutRequest() {
    String xml = SAMLUtils.xml("saml/samlLogoutRequest.xml");
    xml = SAMLUtilsTest.replaceInstantByNow(xml);
    HttpEntity<String> request = new HttpEntity<>(xml);
    ResponseEntity<Void> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/saml/logout", request, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
