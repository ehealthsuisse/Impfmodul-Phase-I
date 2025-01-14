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
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Abstract class to provide common security functionality.
 */
public class AbsSecurityConfiguration {
  protected CsrfTokenRepository createCsrfTokenRepository(String frontendDomain) {
    // use customized CookieTokenRepository due to error in Spring Security class
    return new CookieTokenRepository(frontendDomain);
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

  /**
   * Customized single-page application request handler used for resolving the plain csrf token value sent
   * by the FE in a request header
   */
  static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
      this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
      // If the request contains a request header, use CsrfTokenRequestAttributeHandler to resolve the CsrfToken.
      if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
        return super.resolveCsrfTokenValue(request, csrfToken);
      }
      /*
       * In all other cases (e.g. if the request contains a request parameter),
       * use XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken.
       */
      return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
  }

  static final class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
      // Render the token value to a cookie by causing the deferred token to be loaded
      csrfToken.getToken();

      filterChain.doFilter(request, response);
    }
  }
}
