/**
 * Copyright (c) 2023 eHealth Suisse
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.service.saml.config.IdentityProviderConfig;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class SAMLAuthProviderTest {

  @Autowired
  private SAMLAuthProvider provider;

  @MockitoBean
  private SAMLService samlService;

  @Test
  void authentication_noSAMLAuthentication_throwException() {
    assertThrows(Saml2AuthenticationException.class, () -> provider.authenticate(mock(Authentication.class)));
  }

  //Attention: does fail is debug level is DEBUG due to marshalling a mock object
  @Test
  void authentication_validAuth_returnAuthentication() {
    IdentityProviderConfig config = mock(IdentityProviderConfig.class);
    ArtifactResolve artResolve = mock(ArtifactResolve.class);
    ArtifactResponse artResponse = createArtifactResponse(true);
    when(samlService.getIdpConfig(anyString())).thenReturn(config);
    when(samlService.buildArtifactResolve(eq(config), any())).thenReturn(artResolve);;
    when(samlService.sendAndReceiveArtifactResolve(eq(config), eq(artResolve))).thenReturn(artResponse);
    SAMLAuthentication unAuthenticatedAuth = mock(SAMLAuthentication.class);
    when(unAuthenticatedAuth.getIdp()).thenReturn("Test");
    Authentication authenticatedAuth = mock(SAMLAuthentication.class);

    doAnswer(invocation -> {
      SecurityContextHolder.getContext().setAuthentication(authenticatedAuth);
      return null;
    }).when(samlService).createAuthenticatedSession(any(), any(), any());

    assertEquals(authenticatedAuth, provider.authenticate(unAuthenticatedAuth));
  }

  // Attention: does fail is debug level is DEBUG due to marshalling a mock object
  @Test
  void authentication_validAuthButNoAssertion_throwException() {
    IdentityProviderConfig config = mock(IdentityProviderConfig.class);
    ArtifactResolve artResolve = mock(ArtifactResolve.class);
    ArtifactResponse artResponse = createArtifactResponse(false);
    when(samlService.getIdpConfig(anyString())).thenReturn(config);
    when(samlService.buildArtifactResolve(eq(config), any())).thenReturn(artResolve);;
    when(samlService.sendAndReceiveArtifactResolve(eq(config), eq(artResolve))).thenReturn(artResponse);
    SAMLAuthentication unAuthenticatedAuth = mock(SAMLAuthentication.class);
    when(unAuthenticatedAuth.getIdp()).thenReturn("Test");

    assertThrows(TechnicalException.class, () -> provider.authenticate(unAuthenticatedAuth));
  }

  @Test
  void support_correctClass_returnTrue() {
    assertTrue(provider.supports(SAMLAuthentication.class));
  }

  private ArtifactResponse createArtifactResponse(boolean hasAssertion) {
    Assertion assertion = mock(Assertion.class);
    ArtifactResponse artResponse = mock(ArtifactResponse.class);
    when(artResponse.getIssueInstant()).thenReturn(Instant.now());

    if (hasAssertion) {
      Response response = mock(Response.class);
      when(artResponse.getMessage()).thenReturn(response);
      when(response.getAssertions()).thenReturn(List.of(assertion));
    }

    return artResponse;
  }

}
