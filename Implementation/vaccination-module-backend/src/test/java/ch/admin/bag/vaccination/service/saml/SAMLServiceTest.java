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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.SignatureService;
import ch.admin.bag.vaccination.service.saml.config.IdpProvider;
import ch.fhir.epr.adapter.exception.TechnicalException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
public class SAMLServiceTest {
  private static final String PRINCIPAL = "principal";
  private static final String SAML2_RESPONSE = "SAML2Response";

  @Autowired
  private SAMLServiceIfc samlService;

  @Autowired
  private ProfileConfig profileConfig;

  @SuppressWarnings("unused")
  @Autowired
  private XMLObjectProviderRegistry registry;

  @MockBean
  private SignatureService signatureService;

  @Test
  void createAuthenticatedSession_validCredentials_securityContextHolderContains() {
    createAuthenticatedSession();

    SecurityContext securityContext = SecurityContextHolder.getContext();
    assertNotNull(securityContext);
    assertNotNull(securityContext.getAuthentication());
    assertEquals(PRINCIPAL, securityContext.getAuthentication().getName());
    assertEquals(SAML2_RESPONSE,
        ((SAMLAuthentication) securityContext.getAuthentication()).getSaml2Response());
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
    assertEquals("Dummy",
        ((SAMLAuthentication) securityContext.getAuthentication()).getSaml2Response());
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
    samlService.createAuthenticatedSession(request1, "saml2Reponse1", createAssertion("name1"));
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
  void validateSecurityContext_givenSecurityContextIsKnown_putContextInSecurityContextHolder() {
    createAuthenticatedSession();
    SecurityContext knownContext = SecurityContextHolder.getContext();

    // remove context from holder to be able to check that it was set by the function under test
    SecurityContextHolder.clearContext();

    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("1234");

    samlService.validateSecurityContext(session, knownContext);
    assertEquals(knownContext, SecurityContextHolder.getContext());
  }

  @Test
  void validateSecurityContext_noSecurityContextKnown_throwTechnicalException() {
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("1234");
    SecurityContext context = mock(SecurityContext.class);

    assertThrows(TechnicalException.class,
        () -> samlService.validateSecurityContext(session, context));
  }

  @Test
  void validateSecurityContext_noSecurityContextProvided_throwTechnicalException() {
    HttpSession session = createAuthenticatedSession();

    assertThrows(TechnicalException.class,
        () -> samlService.validateSecurityContext(session, null));
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

    samlService.createAuthenticatedSession(request, SAML2_RESPONSE, assertion);

    return request.getSession();
  }

  private HttpServletRequest createMockRequest(String uri, HttpSession session) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(uri);
    when(request.getRequestURL()).thenReturn(new StringBuffer(uri));
    when(request.getSession()).thenReturn(session);

    return request;
  }

  private HttpSession createMockSession() {
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("1234");

    return session;
  }

}
