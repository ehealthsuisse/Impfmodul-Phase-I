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

import ch.admin.bag.vaccination.service.saml.config.IdentityProviderConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.security.core.context.SecurityContext;

/**
 * SAML functionalities definition.
 */
public interface SAMLServiceIfc {

  /**
   * Resolves the Artifact from the IDP.
   *
   * @param idpConfig {@link IdentityProviderConfig}
   * @param artifact SAML {@link Artifact}
   * @return {@link ArtifactResolve}
   */
  ArtifactResolve buildArtifactResolve(IdentityProviderConfig idpConfig, Artifact artifact);

  /**
   * Creates an valid authenticated session based on a saml resposne.
   *
   * @param request {@link HttpServletRequest}
   * @param saml2Reponse saml response as string
   * @param assertion idp assertion
   */
  void createAuthenticatedSession(HttpServletRequest request, String saml2Reponse, Assertion assertion);

  /**
   * Craetes a dummy session used for development purposes if no saml context is given
   *
   * @param request {@link HttpServletRequest}
   */
  void createDummyAuthentication(HttpServletRequest request);

  /**
   * Gets the IDP config based on the identifiers specific in the idp-config.yml
   *
   * @param idpIdentifier name of the config
   * @return {@link IdentityProviderConfig}
   */
  IdentityProviderConfig getIdpConfig(String idpIdentifier);

  /**
   * Returns number of current sessions.
   *
   * @return number
   */
  int getNumberOfSessions();

  /**
   * Logs out a user by principal name given by the saml artifact response.
   *
   * @param principalName name of the user
   */
  void logout(String principalName);

  /**
   * Prepares the redirect to the IDP by doing the SAML authentication flow using HTTP Post.
   *
   * @param idpIdentifier Identifier of the idp config
   * @param httpServletResponse {@link HttpServletResponse}
   */
  void redirectToIdp(String idpIdentifier, HttpServletResponse httpServletResponse);

  /**
   * As the name indicates sends and receives the artifact resolve item.
   *
   * @param idpIdentifier Identifier of the idp config
   * @param artifactResolve {@link ArtifactResolve}
   * @return {@link ArtifactResponse}
   */
  ArtifactResponse sendAndReceiveArtifactResolve(IdentityProviderConfig idpConfig, ArtifactResolve artifactResolve);

  /**
   * Validates the Response, e.g. checking lifetimes and received endpoints.
   *
   * @param artifactResponse {@link ArtifactResponse}
   * @param request {@link HttpServletRequest}
   */
  void validateArtifactResponse(ArtifactResponse artifactResponse, HttpServletRequest request);

  /**
   * Validates the spring security context stored in the http session.
   *
   * @param httpSession {@link HttpSession}
   * @param authenticatedSecurityContext {@link SecurityContext}
   */
  void validateSecurityContext(HttpSession httpSession, SecurityContext authenticatedSecurityContext);
}
