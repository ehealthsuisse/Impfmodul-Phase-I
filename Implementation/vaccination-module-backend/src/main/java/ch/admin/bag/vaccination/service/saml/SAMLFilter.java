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
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.IOException;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Specific filter which triggers the saml authentication.
 *
 * It ignores utility or saml endpoints, all other endpoints are secured.
 */
public class SAMLFilter extends GenericFilterBean {

  private SAMLServiceIfc samlService;
  private ProfileConfig profileConfig;

  public SAMLFilter(SAMLServiceIfc samlService, ProfileConfig profileConfig) {
    this.samlService = samlService;
    this.profileConfig = profileConfig;
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;

    checkSAMLAuthenticationState(httpServletRequest);
    boolean isToBeAuthenticated = isToBeAuthenticated(httpServletRequest);
    boolean isUserAuthenticated = httpServletRequest.getSession()
        .getAttribute(SAMLService.AUTHENTICATED_SESSION_ATTRIBUTE) != null;

    if (isToBeAuthenticated && isUserAuthenticated) {
      SecurityContext authenticatedSecurityContext =
          (SecurityContext) httpServletRequest.getSession()
              .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

      if (authenticatedSecurityContext == null) {
        throw new TechnicalException(
            "An authenticated user must include a spring security context.");
      }

      samlService.validateSecurityContext(httpServletRequest.getSession(),
          authenticatedSecurityContext);
    } else if (isToBeAuthenticated) {
      String idpIdentifier = getIdpIdentifierFromCookie(httpServletRequest);
      setGotoURLOnSession(httpServletRequest);
      redirectUserForAuthentication(idpIdentifier, httpServletResponse);
      return; // stop security processing
    }

    chain.doFilter(request, response);
  }

  private void checkSAMLAuthenticationState(HttpServletRequest request) {
    if (!profileConfig.isSamlAuthenticationActive()) {
      samlService.createDummyAuthentication(request, "DUMMY"); // TODO Param or PractionerName
    }
  }

  private String getIdpIdentifierFromCookie(HttpServletRequest request) {
    return List.of(request.getCookies()).stream()
        .filter(cookie -> cookie.getName().equals(SAMLService.IDP_IDENTIFIER_ATTRIBUTE))
        .findAny()
        .orElseThrow(() -> new TechnicalException("Cookie containing Idp identifier not found."))
        .getValue();
  }

  private boolean isToBeAuthenticated(HttpServletRequest httpServletRequest) {
    String uri = httpServletRequest.getRequestURI();

    return !(uri.startsWith("/swagger")
        || uri.startsWith("/v3/api-docs")
        || uri.startsWith("/actuator")
        || uri.startsWith("/saml/"));
  }

  private void redirectUserForAuthentication(String idpIdentifier,
      HttpServletResponse httpServletResponse) {
    samlService.redirectToIdp(idpIdentifier, httpServletResponse);
  }

  private void setGotoURLOnSession(HttpServletRequest request) {
    request.getSession().setAttribute("gotoURL", request.getRequestURL().toString());
  }

}
