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
package ch.admin.bag.vaccination.service.saml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import javax.net.ssl.SSLContext;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.utils.HttpSessionUtils;
import ch.admin.bag.vaccination.service.saml.config.IdentityProviderConfig;
import ch.admin.bag.vaccination.service.saml.config.IdpProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootTest
public class SAMLServiceTest {
  private static final String IDP_SP = "idpSP";
  private static final String SP = "sp";
  private static final String PRINCIPAL = "principal";

  @Autowired
  private SAMLServiceIfc samlService;

  @Autowired
  private ProfileConfig profileConfig;

  @SuppressWarnings("unused")
  @Autowired
  private XMLObjectProviderRegistry registry;

  @Test
  void buildArtifactResolve_explicitProviderEntityIdSet_returnExplicitId() {
    IdentityProviderConfig config = mock(IdentityProviderConfig.class);
    when(config.getEntityId()).thenReturn(IDP_SP);
    Artifact artifact = mock(Artifact.class);
    ReflectionTestUtils.setField(samlService, "spEntityId", SP);

    ArtifactResolve result = samlService.buildArtifactResolve(config, artifact);
    assertEquals(IDP_SP, result.getIssuer().getValue());
  }

  @Test
  void buildArtifactResolve_noProviderEntityIdSet_returnDefaultEntity() {
    IdentityProviderConfig config = mock(IdentityProviderConfig.class);
    Artifact artifact = mock(Artifact.class);
    ReflectionTestUtils.setField(samlService, "spEntityId", SP);

    ArtifactResolve result = samlService.buildArtifactResolve(config, artifact);
    assertEquals(SP, result.getIssuer().getValue());
  }

  @Test
  void checkAndUpdateSessionInformation_differentSession_sameName_returnTrue() {
    HttpSession session = createAuthenticatedSession();
    HttpSession newSession = new MockHttpSession();
    newSession.setAttribute(HttpSessionUtils.AUTHENTICATED_SESSION_ATTRIBUTE, true);
    newSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));

    assertTrue(samlService.checkAndUpdateSessionInformation(session));
  }

  @Test
  void checkAndUpdateSessionInformation_loggedOutSession_returnFalse_sessionIsAlreadyInvalidated() {
    HttpSession session = createAuthenticatedSession();
    assertTrue(samlService.checkAndUpdateSessionInformation(session));

    samlService.logout(PRINCIPAL);
    assertFalse(samlService.checkAndUpdateSessionInformation(session));
  }

  @Test
  void checkAndUpdateSessionInformation_sameSession_returnTrue() {
    HttpSession session = createAuthenticatedSession();

    assertTrue(samlService.checkAndUpdateSessionInformation(session));
  }

  @Test
  void createAuthenticatedSession_validCredentials_securityContextHolderContains() {
    createAuthenticatedSession();

    SecurityContext securityContext = SecurityContextHolder.getContext();
    assertNotNull(securityContext);
    assertNotNull(securityContext.getAuthentication());
    assertEquals(PRINCIPAL, securityContext.getAuthentication().getName());
    assertNotNull(((SAMLAuthentication) securityContext.getAuthentication()).getAssertion());
  }

  @Test
  void createDummyAuthentication_anyInput_dummyAuthenticationIsPutInSecurityContext() {
    profileConfig.setSamlAuthenticationActive(false);
    HttpServletRequest request = createMockRequest("/husky", createMockSession());

    samlService.createDummyAuthentication(request);

    SecurityContext securityContext = SecurityContextHolder.getContext();
    assertNotNull(securityContext);
    assertNotNull(securityContext.getAuthentication());
    assertEquals("Dummy", securityContext.getAuthentication().getName());
    assertNotNull(((SAMLAuthentication) securityContext.getAuthentication()).getAssertion());
  }

  @Test
  void getterSetter_samlAuthentication_returnSameValue() {
    assertFalse(profileConfig.isSamlAuthenticationActive());

    profileConfig.setSamlAuthenticationActive(true);

    assertTrue(profileConfig.isSamlAuthenticationActive());
  }

  @Test
  void logout() {
    MockHttpServletRequest request1 = new MockHttpServletRequest();
    request1.setSession(new MockHttpSession(null, "session1"));

    assertEquals(samlService.getNumberOfSessions(), 0);
    samlService.createAuthenticatedSession("dummyIdp", request1, createAssertion("name1"));
    assertEquals(samlService.getNumberOfSessions(), 1);

    LogoutRequest logoutRequest = mock(LogoutRequest.class);
    NameID nameID = mock(NameID.class);
    when(nameID.getValue()).thenReturn("name2");
    when(logoutRequest.getNameID()).thenReturn(nameID);
    samlService.logout("name1");
    assertEquals(samlService.getNumberOfSessions(), 0);
  }

  @Test
  void redirectToIdp_validResponse_noException() {
    HttpServletResponse mock = new MockHttpServletResponse();
    samlService.redirectToIdp(IdpProvider.GAZELLE_IDP_IDENTIFIER, mock);
  }

  @Test
  void sendLogoutToNextNode_noAttemptedIndexes_sendsToFirstConfiguredNode() throws Exception {
    // Arrange
    String firstLogoutURL = "http://node1.example.com/logout";
    String nextLogoutURL = "http://node2.example.com/logout";
    List<String> logoutURLs = List.of(firstLogoutURL, nextLogoutURL);
    String logoutRequestBody = "<logoutRequest>test</logoutRequest>";
    HttpResponse<String> mockResponse = mockStringHttpResponse();
    when(mockResponse.statusCode()).thenReturn(200);

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpClient.Builder mockHttpClientBuilder = mock(HttpClient.Builder.class);
    when(mockHttpClientBuilder.sslContext(any(SSLContext.class))).thenReturn(mockHttpClientBuilder);
    when(mockHttpClientBuilder.build()).thenReturn(mockHttpClient);
    when(mockHttpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
      .thenReturn(mockResponse);

    try (MockedStatic<HttpClient> mockedHttpClient = Mockito.mockStatic(HttpClient.class)) {
      mockedHttpClient.when(HttpClient::newBuilder).thenReturn(mockHttpClientBuilder);
      SAMLService samlService = new SAMLService();

      // Act
      samlService.sendLogoutToNextNode(logoutURLs, null, logoutRequestBody);

      // Assert
      ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
      verify(mockHttpClient).send(requestCaptor.capture(),
          ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
      HttpRequest capturedRequest = requestCaptor.getValue();

      assertEquals(firstLogoutURL, capturedRequest.uri().toString());
      assertEquals("application/soap+xml", capturedRequest.headers().firstValue("Content-Type").orElse(null));
      assertEquals("0", capturedRequest.headers()
          .firstValue(SAMLService.ATTEMPTED_LOGOUT_INDEXES_HEADER).orElse(null));
      assertTrue(capturedRequest.bodyPublisher().isPresent());
    }
  }

  @Test
  void sendLogoutToNextNode_firstIndexAlreadyAttempted_sendsToSecondConfiguredNode() throws Exception {
    // Arrange
    String firstLogoutURL = "http://node1.example.com/logout";
    String secondLogoutURL = "http://node2.example.com/logout";
    List<String> logoutURLs = List.of(firstLogoutURL, secondLogoutURL);
    String logoutRequestBody = "<logoutRequest>test</logoutRequest>";
    HttpResponse<String> mockResponse = mockStringHttpResponse();
    when(mockResponse.statusCode()).thenReturn(200);

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpClient.Builder mockHttpClientBuilder = mock(HttpClient.Builder.class);
    when(mockHttpClientBuilder.sslContext(any(SSLContext.class))).thenReturn(mockHttpClientBuilder);
    when(mockHttpClientBuilder.build()).thenReturn(mockHttpClient);
    when(mockHttpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
      .thenReturn(mockResponse);

    try (MockedStatic<HttpClient> mockedHttpClient = Mockito.mockStatic(HttpClient.class)) {
      mockedHttpClient.when(HttpClient::newBuilder).thenReturn(mockHttpClientBuilder);
      SAMLService samlService = new SAMLService();

      // Act
      samlService.sendLogoutToNextNode(logoutURLs, "0", logoutRequestBody);

      // Assert
      ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
      verify(mockHttpClient).send(requestCaptor.capture(),
          ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
      HttpRequest capturedRequest = requestCaptor.getValue();

      assertEquals(secondLogoutURL, capturedRequest.uri().toString());
      assertEquals("0,1", capturedRequest.headers()
          .firstValue(SAMLService.ATTEMPTED_LOGOUT_INDEXES_HEADER).orElse(null));
    }
  }

  @Test
  void sendLogoutToNextNode_unreachableNode_sendsToFollowingNode() throws Exception {
    // Arrange
    String unavailableLogoutURL = "http://node1.example.com/logout";
    String followingLogoutURL = "http://node2.example.com/logout";
    List<String> logoutURLs = List.of(unavailableLogoutURL, followingLogoutURL);
    String logoutRequestBody = "<logoutRequest>test</logoutRequest>";
    HttpResponse<String> mockResponse = mockStringHttpResponse();
    when(mockResponse.statusCode()).thenReturn(200);

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpClient.Builder mockHttpClientBuilder = mock(HttpClient.Builder.class);
    when(mockHttpClientBuilder.sslContext(any(SSLContext.class))).thenReturn(mockHttpClientBuilder);
    when(mockHttpClientBuilder.build()).thenReturn(mockHttpClient);
    when(mockHttpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
      .thenThrow(new RuntimeException("unreachable"))
      .thenReturn(mockResponse);

    try (MockedStatic<HttpClient> mockedHttpClient = Mockito.mockStatic(HttpClient.class)) {
      mockedHttpClient.when(HttpClient::newBuilder).thenReturn(mockHttpClientBuilder);
      SAMLService samlService = new SAMLService();

      // Act
      samlService.sendLogoutToNextNode(logoutURLs, null, logoutRequestBody);

      // Assert
      ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
      verify(mockHttpClient, Mockito.times(2)).send(requestCaptor.capture(),
          ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
      List<HttpRequest> capturedRequests = requestCaptor.getAllValues();

      assertEquals(unavailableLogoutURL, capturedRequests.get(0).uri().toString());
      assertEquals("0", capturedRequests.get(0).headers()
          .firstValue(SAMLService.ATTEMPTED_LOGOUT_INDEXES_HEADER).orElse(null));
      assertEquals(followingLogoutURL, capturedRequests.get(1).uri().toString());
      assertEquals("0,1", capturedRequests.get(1).headers()
          .firstValue(SAMLService.ATTEMPTED_LOGOUT_INDEXES_HEADER).orElse(null));
    }
  }

  @Test
  void sendLogoutToNextNode_allNodesAlreadyAttempted_doesNotSendRequest() throws Exception {
    // Arrange
    String currentLogoutURL = "http://node1.example.com/logout";
    List<String> logoutURLs = List.of(currentLogoutURL, "http://node2.example.com/logout");
    String logoutRequestBody = "<logoutRequest>test</logoutRequest>";
    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpClient.Builder mockHttpClientBuilder = mock(HttpClient.Builder.class);
    when(mockHttpClientBuilder.sslContext(any(SSLContext.class))).thenReturn(mockHttpClientBuilder);
    when(mockHttpClientBuilder.build()).thenReturn(mockHttpClient);
    SAMLService samlService = new SAMLService();

    try (MockedStatic<HttpClient> mockedHttpClient = Mockito.mockStatic(HttpClient.class)) {
      mockedHttpClient.when(HttpClient::newBuilder).thenReturn(mockHttpClientBuilder);

      // Act
      samlService.sendLogoutToNextNode(logoutURLs, "0,1", logoutRequestBody);

      // Assert
      verify(mockHttpClient, never()).send(any(HttpRequest.class),
          ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }
  }

  @SuppressWarnings("unchecked")
  private HttpResponse<String> mockStringHttpResponse() {
    return mock(HttpResponse.class);
  }

  private Assertion createAssertion() {
    return createAssertion(PRINCIPAL);
  }

  private Assertion createAssertion(String name) {
    NameID nameId = mock(NameID.class);
    when(nameId.getValue()).thenReturn(name);
    Subject subject = mock(Subject.class);
    when(subject.getNameID()).thenReturn(nameId);
    Assertion assertion = mock(Assertion.class);
    when(assertion.getSubject()).thenReturn(subject);
    return assertion;
  }

  private HttpSession createAuthenticatedSession() {
    Assertion assertion = createAssertion();
    HttpServletRequest request = createMockRequest("/husky", createMockSession());

    samlService.createAuthenticatedSession("dummyIdp", request, assertion);

    return request.getSession();
  }

  private HttpServletRequest createMockRequest(String uri, HttpSession session) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(uri);
    when(request.getRequestURL()).thenReturn(new StringBuffer(uri));
    when(request.getSession()).thenReturn(session);
    when(request.getSession(true)).thenReturn(session);
    when(request.getSession(false)).thenReturn(session);

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    return request;
  }

  private HttpSession createMockSession() {
    return new MockHttpSession();
  }
}
