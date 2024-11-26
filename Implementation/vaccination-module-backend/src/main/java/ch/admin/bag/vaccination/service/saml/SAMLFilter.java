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

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.exception.AccessDeniedException;
import ch.admin.bag.vaccination.service.HttpSessionUtils;
import java.io.IOException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Specific filter which triggers the saml authentication.
 *
 * It ignores utility or saml endpoints, all other endpoints are secured.
 */
@Slf4j
public class SAMLFilter extends GenericFilterBean {

  private SAMLServiceIfc samlService;
  private ProfileConfig profileConfig;

  public SAMLFilter(SAMLServiceIfc samlService, ProfileConfig profileConfig) {
    this.samlService = samlService;
    this.profileConfig = profileConfig;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    log.debug("Request url: {}", httpServletRequest.getRequestURL());

    checkSAMLAuthenticationState(httpServletRequest);

    HttpSession httpSession = httpServletRequest.getSession(false);
    boolean isToBeAuthenticated = isToBeAuthenticated(httpServletRequest);
    boolean isLoginCall = httpServletRequest.getRequestURL().toString().contains("/saml/login")
        && DispatcherType.REQUEST.equals(httpServletRequest.getDispatcherType());
    boolean isUserAuthenticated = HttpSessionUtils.getIsAuthenticatedFromSession();

    if (!isLoginCall && isToBeAuthenticated && isUserAuthenticated) {
      SecurityContext authenticatedSecurityContext =
          (SecurityContext) httpSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

      if (authenticatedSecurityContext == null) {
        log.error("An authenticated user must include a spring security context, please check session {}.");
        throw new AccessDeniedException("GLOBAL.DISCONNECT");
      }

      if (!samlService.checkAndUpdateSessionInformation(httpSession)) {
        log.info("User was logged out while his current http session was still active, relogin is initialized.");
        throw new AccessDeniedException("GLOBAL.DISCONNECT");
      }
    } else if (isToBeAuthenticated) {
      forwardToIdp(httpServletResponse);
      return;
    }

    chain.doFilter(request, response);
  }

  // development purpose only - needs adaption to local security to be used
  private void checkSAMLAuthenticationState(HttpServletRequest request) {
    boolean isAlreadyAuthenticated = HttpSessionUtils.getIsAuthenticatedFromSession();
    if (!isAlreadyAuthenticated && !profileConfig.isSamlAuthenticationActive()) {
      samlService.createDummyAuthentication(request);
    }
  }

  private void forwardToIdp(HttpServletResponse httpServletResponse) {
    String idpIdentifier = HttpSessionUtils.getIdpFromSession();
    boolean validInitialCall = HttpSessionUtils.getIsInitialCallValidFromSession();
    if (validInitialCall && idpIdentifier != null) {
      log.info("User needs to be authenticated. Forwarding to IDP {}.", idpIdentifier);
      samlService.redirectToIdp(idpIdentifier, httpServletResponse);
    } else if (!validInitialCall) {
      log.info("User did not have valid initial webcall, forward to IDP refused.");
      throw new AccessDeniedException("GLOBAL.DISCONNECT");
    }
  }

  private boolean isToBeAuthenticated(HttpServletRequest httpServletRequest) {
    String uri = httpServletRequest.getRequestURI();

    return !(uri.contains("/swagger")
        || uri.contains("/v3/api-docs")
        || uri.contains("/actuator")
        || uri.contains("/signature")
        || uri.contains("/saml/sso")
        || uri.contains("/saml/isAuthenticated")
        || uri.contains("/saml/logout")
        || uri.contains("/utility"));
  }

}
