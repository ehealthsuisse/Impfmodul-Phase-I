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
package ch.admin.bag.vaccination.config;

import ch.admin.bag.vaccination.service.saml.SAMLAuthFilter;
import ch.admin.bag.vaccination.service.saml.SAMLFilter;
import ch.admin.bag.vaccination.service.saml.SAMLService;
import ch.admin.bag.vaccination.service.saml.SAMLServiceIfc;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * Abstract class to provide common security functionality.
 */
public class AbsSecurityConfiguration {
  protected CsrfTokenRepository createCsrfTokenRepository(String frontendDomain) {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookiePath("/");
    if (frontendDomain != null && !frontendDomain.isEmpty()) {
      repository.setCookieDomain(frontendDomain);
    }

    return repository;
  }

  /**
   * Ensure that logout is handled correctly only sending 200 response code and logging out the
   * session. Without it, Spring would by default forward to a login url which is not desired.
   *
   * @param samlService {@link SAMLService}
   * @return logout configuration
   */
  protected Customizer<LogoutConfigurer<HttpSecurity>> createLogoutConfig(SAMLServiceIfc samlService) {
    return logout -> logout.logoutUrl("/logout")
        .invalidateHttpSession(true)
        .logoutSuccessHandler((request, response, authentication) -> {
          if (samlService != null) {
            samlService.removeSession(request.getRequestedSessionId());
          }

          response.setStatus(HttpServletResponse.SC_OK);
        });
  }

  protected Filter createSAMLAuthFilter(AuthenticationManager authManager,
      HttpSessionSecurityContextRepository securityContextRepository) {
    return new SAMLAuthFilter(authManager, securityContextRepository);
  }

  protected Filter createSAMLFilter(SAMLServiceIfc samlService, ProfileConfig profileConfig) {
    return new SAMLFilter(samlService, profileConfig);
  }
}
