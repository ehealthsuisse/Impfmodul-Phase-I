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

import ch.admin.bag.vaccination.controller.SAMLController;
import ch.admin.bag.vaccination.service.HttpSessionUtils;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.ForwardAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * Specific filter which triggers the saml authentication.
 *
 * It ignores utility or saml endpoints, all other endpoints are secured.
 */
@Slf4j
public class SAMLAuthFilter extends AbstractAuthenticationProcessingFilter {

  public SAMLAuthFilter(AuthenticationManager authManager,
      HttpSessionSecurityContextRepository securityContextRepository) {
    super(SAMLController.SSO_ENDPOINT_NEW, authManager);

    // ensure that valid context is stored in the session
    setSecurityContextRepository(securityContextRepository);
    setAllowSessionCreation(true);
    setSessionAuthenticationStrategy(new SessionFixationProtectionStrategy());
    setAuthenticationSuccessHandler(new ForwardAuthenticationSuccessHandler("/saml/login"));
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException, IOException, ServletException {
    String idpIdentifier = HttpSessionUtils.getIdpFromSession();
    if (idpIdentifier == null || idpIdentifier.isBlank()) {
      idpIdentifier = request.getParameter("RelayState");
    }
    String samlArtifact = request.getParameter("SAMLart");
    log.debug("Artifact {} received for idp {}", samlArtifact, idpIdentifier);

    SAMLAuthentication authRequest = new SAMLAuthentication(idpIdentifier, samlArtifact);
    return this.getAuthenticationManager().authenticate(authRequest);
  }

}
