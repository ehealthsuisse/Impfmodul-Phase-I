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
 *
 */
public interface SAMLServiceIfc {

  public ArtifactResolve buildArtifactResolve(IdentityProviderConfig idpConfig, Artifact artifact);

  public void createAuthenticatedSession(HttpServletRequest request, String saml2Reponse, Assertion assertion);

  public void createDummyAuthentication(HttpServletRequest request, String dummyName);

  public IdentityProviderConfig getIdpConfig(String idpIdentifier);

  public int getSessionNumber();

  public void logout(String name);

  public void redirectToIdp(String idpIdentifier, HttpServletResponse httpServletResponse);

  public ArtifactResponse sendAndReceiveArtifactResolve(IdentityProviderConfig idpConfig,
      ArtifactResolve artifactResolve);

  public void validateArtifactResponse(ArtifactResponse artifactResponse, HttpServletRequest request);

  public void validateSecurityContext(HttpSession httpSession, SecurityContext authenticatedSecurityContext);
}
