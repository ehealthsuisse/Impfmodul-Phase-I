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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.saml.SAMLService;
import ch.admin.bag.vaccination.service.saml.SAMLUtils;
import ch.admin.bag.vaccination.service.saml.SAMLXmlTestUtils;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.w3c.dom.Element;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SAMLControllerMockTest {

  private static final String SAML_ART = "1234567890";

  @MockitoBean
  private SAMLService samlService;
  @MockitoBean
  private ProfileConfig profileConfig;

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void ssoLogin_mockService_itJustWorks() throws Exception {
    createArtifactResolveMock();
    createArtifactResponseMock();

    RequestEntity<Void> entity = new RequestEntity<>(HttpMethod.GET,
        new URI("http://localhost:" + port + SAMLController.SSO_ENDPOINT_NEW + "?SAMLart=" + SAML_ART));
    ResponseEntity<Void> response = restTemplate.exchange(entity, Void.class);
    assertNotNull(response);

    ArgumentCaptor<Artifact> capturer = ArgumentCaptor.forClass(Artifact.class);
    verify(samlService).buildArtifactResolve(any(), capturer.capture());

    Artifact artifact = capturer.getValue();
    assertEquals(SAML_ART, artifact.getValue());
  }

  @Test
  void ssoLogout() throws Exception {
    HttpEntity<String> request = new HttpEntity<>(SAMLXmlTestUtils.xml("saml/samlLogoutRequest.xml"));
    mockLogoutResponse();

    ResponseEntity<String> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/saml/logout", request, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(samlService).logout(eq("remery"));
  }

  private void createArtifactResolveMock() throws Exception {
    ArtifactResolve artifactResolveMock = mock(ArtifactResolve.class);
    when(artifactResolveMock.isSigned()).thenReturn(true);
    Element artifactResolveElement = SAMLXmlTestUtils.createXMLElementFromFile("saml/samlArtifactResolve.xml");
    when(artifactResolveMock.getDOM()).thenReturn(artifactResolveElement);
    when(artifactResolveMock.getSchemaType()).thenReturn(ArtifactResolve.TYPE_NAME);
    when(samlService.buildArtifactResolve(any(), any())).thenReturn(artifactResolveMock);
  }

  private ArtifactResponse createArtifactResponseMock() throws Exception {
    ArtifactResponse artifactResponseMock = mock(ArtifactResponse.class);
    when(artifactResponseMock.isSigned()).thenReturn(true);
    Element artifactResponseElement = SAMLXmlTestUtils.createXMLElementFromFile("saml/samlArtifactResponse.xml");
    when(artifactResponseMock.getDOM()).thenReturn(artifactResponseElement);
    when(artifactResponseMock.getSchemaType()).thenReturn(ArtifactResponse.TYPE_NAME);
    when(samlService.sendAndReceiveArtifactResolve(any(), any())).thenReturn(artifactResponseMock);

    Assertion assertion = SAMLXmlTestUtils.createAssertion("saml/samlAssertion.xml");
    Response responseContainigAssertion = mock(Response.class);
    when(responseContainigAssertion.getAssertions()).thenReturn(List.of(assertion));
    when(artifactResponseMock.getMessage()).thenReturn(responseContainigAssertion);

    return artifactResponseMock;
  }

  private void mockLogoutResponse() throws Exception {
    String logoutResponseString = SAMLXmlTestUtils.xml("saml/samlLogoutResponse.xml");
    XMLObject logoutResponse = SAMLUtils.unmarshall(logoutResponseString);

    when(samlService.createLogoutResponse(any(), any(), any())).thenReturn((LogoutResponse) logoutResponse);
  }

}
