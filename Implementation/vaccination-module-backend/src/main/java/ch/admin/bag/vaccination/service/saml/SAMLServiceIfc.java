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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;

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
   * Validates that the correct sessions is associated with the user.
   *
   * @param httpSession {@link HttpSession}
   * @return <code>true</code> if IDP session is still valid, false otherwise
   */
  boolean checkAndUpdateSessionInformation(HttpSession httpSession);

  /**
   * Creates an valid authenticated session based on a saml resposne.
   *
   * @param idp identifier of the IDP
   * @param request {@link HttpServletRequest}
   * @param assertion idp assertion
   */
  void createAuthenticatedSession(String idp, HttpServletRequest request, Assertion assertion);

  /**
   * Craetes a dummy session used for development purposes if no saml context is given
   *
   * @param request {@link HttpServletRequest}
   */
  void createDummyAuthentication(HttpServletRequest request);

  /**
   * Creates a logout response based on a given logout request
   *
   * @param idp idp information needed to retrieve logout url
   * @param logoutRequest {@link LogoutRequest}
   * @param request {@link HttpServletRequest}
   * @return {@link LogoutResponse}
   */
  LogoutResponse createLogoutResponse(String idp, LogoutRequest logoutRequest, HttpServletRequest request);

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
   * @return returns IDP string needed to process the logout response
   */
  String logout(String principalName);

  /**
   * Prepares the redirect to the IDP by doing the SAML authentication flow using HTTP Post.
   *
   * @param idpIdentifier Identifier of the idp config
   * @param httpServletResponse {@link HttpServletResponse}
   */
  void redirectToIdp(String idpIdentifier, HttpServletResponse httpServletResponse);

  /**
   * Logs out a user by sessionId.
   *
   * @param sessionId HTTP sessionId of the user
   * @return returns IDP string if available
   */
  String removeSession(String sessionId);

  /**
   * As the name indicates sends and receives the artifact resolve item.
   *
   * @param idpConfig Identifier of the idp config
   * @param artifactResolve {@link ArtifactResolve}
   * @return {@link ArtifactResponse}
   */
  ArtifactResponse sendAndReceiveArtifactResolve(IdentityProviderConfig idpConfig, ArtifactResolve artifactResolve);

  /**
   * Sends a logout request to the other node in the cluster if configured.
   *
   * @param otherNodeLogoutURL URL of the other node to send the logout request to
   * @param logoutRequestBody body of the logout request containing the SAML logout request
   */
  void sendLogoutToOtherNode(String otherNodeLogoutURL, String logoutRequestBody);
}
