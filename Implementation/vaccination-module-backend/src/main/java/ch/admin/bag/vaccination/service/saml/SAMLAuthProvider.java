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

import ch.admin.bag.vaccination.service.saml.config.IdentityProviderConfig;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Authentication provider which validates the saml artifact (saml token) with the IDP.
 */
@Slf4j
@Service
public class SAMLAuthProvider implements AuthenticationProvider {

  @Autowired
  private SAMLService samlService;

  @Autowired
  private HttpServletRequest request;

  /**
   * <p>
   * The authenticate method to authenticate the request. We will get the username from the
   * Authentication object and will use the custom @userDetailsService service to load the given user.
   * </p>
   *
   * @param authentication unauthenticated token
   * @return authenticated token or exception
   * @throws AuthenticationException
   */
  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (authentication instanceof SAMLAuthentication samlAuth) {
      IdentityProviderConfig idpConfig = samlService.getIdpConfig(samlAuth.getIdp());

      Artifact artifact = SAMLUtils.buildArtifactFromRequest(samlAuth.getArtifact());
      ArtifactResolve artifactResolve = samlService.buildArtifactResolve(idpConfig, artifact);
      SAMLUtils.logSAMLObject(artifactResolve);
      ArtifactResponse artifactResponse =
          samlService.sendAndReceiveArtifactResolve(idpConfig, artifactResolve);

      SAMLUtils.validateArtifactResponse(artifactResponse, request);
      Assertion assertion = SAMLUtils.getAssertion(artifactResponse);

      boolean validAuthentication = assertion != null;
      if (validAuthentication) {
        log.debug("Received valid artifact response including assertion.");
        if (log.isTraceEnabled()) {
          SAMLUtils.logSAMLObject(assertion);
        }
        samlService.createAuthenticatedSession(samlAuth.getIdp(), request, assertion);
        return SecurityContextHolder.getContext().getAuthentication();
      }

      String saml2Reponse = SAMLUtils.convertSamlMessageToString(artifactResponse);
      throw new Saml2AuthenticationException(new Saml2Error("400", "Invalid artifact response: \n" + saml2Reponse));
    }

    throw new Saml2AuthenticationException(new Saml2Error("300", "Invalid authentication token."));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.equals(SAMLAuthentication.class);
  }

}
