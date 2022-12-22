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
import ch.admin.bag.vaccination.service.saml.config.IdpProvider;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Response;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SAMLControllerMockTest {

  private static final String IDP_COOKIE = "idpIdentifier=GAZELLE";
  private static final String SAML_ART = "1234567890";

  @MockBean
  private SAMLService samlService;
  @MockBean
  private ProfileConfig profileConfig;

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void samlAuthenticationActive_anyInput_forwardToSamlService() throws Exception {
    ResponseEntity<String> response = restTemplate.getForEntity(
        SAMLController.SAML_AUTHENTICATION_ACTIVE.replace("{active}", "true"),
        String.class);

    assertNotNull(response);

    verify(profileConfig).setSamlAuthenticationActive(true);
  }

  @Test
  void ssoLogin_mockService_itJustWorks() throws Exception {
    createArtifactResolveMock();
    ArtifactResponse artifactResponse = createArtifactResponseMock();


    RequestEntity<Void> entity = new RequestEntity<>(createCookieHeader(), HttpMethod.GET,
        new URI("http://localhost:" + port + SAMLController.SSO_ENDPOINT + "?SAMLart=" + SAML_ART));
    ResponseEntity<Void> response = restTemplate.exchange(entity, Void.class);

    assertNotNull(response);

    ArgumentCaptor<Artifact> capturer = ArgumentCaptor.forClass(Artifact.class);
    verify(samlService).buildArtifactResolve(any(), capturer.capture());
    verify(samlService).validateArtifactResponse(eq(artifactResponse), any());

    Artifact artifact = capturer.getValue();
    assertEquals(SAML_ART, artifact.getValue());
  }

  @Test
  void ssoLogin_validInput_cookieWasCreated() {
    ResponseEntity<Void> response = restTemplate.getForEntity(
        "http://localhost:" + port
            + SAMLController.SSO_LOGIN_ENDPOINT.replace("{idpIdentifier}",
                IdpProvider.GAZELLE_IDP_IDENTIFIER),
        Void.class);

    assertNotNull(response);
    assertNotNull(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
        .contains("idpIdentifier=" + IdpProvider.GAZELLE_IDP_IDENTIFIER);
  }

  @Test
  void ssoLogout() {

    HttpEntity<String> request = new HttpEntity<>(SAMLUtils.xml("saml/samlLogoutRequest.xml"));

    ResponseEntity<Void> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/saml/logout", request, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(samlService).logout(eq("remery"));
  }

  private void createArtifactResolveMock() throws Exception {
    ArtifactResolve artifactResolveMock = mock(ArtifactResolve.class);
    when(artifactResolveMock.isSigned()).thenReturn(true);
    Element artifactResolveElement = createXMLElementFromFile("saml/samlArtifactResolve.xml");
    when(artifactResolveMock.getDOM()).thenReturn(artifactResolveElement);
    when(artifactResolveMock.getSchemaType()).thenReturn(ArtifactResolve.TYPE_NAME);
    when(samlService.buildArtifactResolve(any(), any())).thenReturn(artifactResolveMock);
  }

  private ArtifactResponse createArtifactResponseMock() throws Exception {
    ArtifactResponse artifactResponseMock = mock(ArtifactResponse.class);
    when(artifactResponseMock.isSigned()).thenReturn(true);
    Element artifactResponseElement = createXMLElementFromFile("saml/samlArtifactResponse.xml");
    when(artifactResponseMock.getDOM()).thenReturn(artifactResponseElement);
    when(artifactResponseMock.getSchemaType()).thenReturn(ArtifactResponse.TYPE_NAME);
    when(samlService.sendAndReceiveArtifactResolve(any(), any()))
        .thenReturn(artifactResponseMock);

    Assertion assertion = createAssertion();
    Response responseContainigAssertion = mock(Response.class);
    when(responseContainigAssertion.getAssertions()).thenReturn(List.of(assertion));
    when(artifactResponseMock.getMessage()).thenReturn(responseContainigAssertion);

    return artifactResponseMock;
  }

  private Assertion createAssertion() throws Exception {
    Attribute attribute = mock(Attribute.class);
    AttributeStatement attrStatement = mock(AttributeStatement.class);
    when(attrStatement.getAttributes()).thenReturn(List.of(attribute));

    AuthnContextClassRef authncontextClassRef = mock(AuthnContextClassRef.class);
    when(authncontextClassRef.getURI()).thenReturn("URI");

    AuthnContext authNContext = mock(AuthnContext.class);
    when(authNContext.getAuthnContextClassRef()).thenReturn(authncontextClassRef);

    AuthnStatement authStatement = mock(AuthnStatement.class);
    when(authStatement.getAuthnContext()).thenReturn(authNContext);

    Element assertionElement = createXMLElementFromFile("saml/samlAssertion.xml");
    Assertion assertion = mock(Assertion.class);
    when(assertion.getAttributeStatements()).thenReturn(List.of(attrStatement));
    when(assertion.getAuthnStatements()).thenReturn(List.of(authStatement));
    when(assertion.getSchemaType()).thenReturn(Assertion.TYPE_NAME);
    when(assertion.getDOM()).thenReturn(assertionElement);

    return assertion;
  }

  private HttpHeaders createCookieHeader() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, IDP_COOKIE);

    return headers;
  }

  private Element createXMLElementFromFile(String path) throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource(path).getFile());
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document document = dBuilder.parse(new InputSource(new FileReader(file)));

    return document.getDocumentElement();
  }

}
