/**
 * No dedicated copyright this class is a copy of CookieCsrfTokenRepository from the spring framework.
 */
package ch.admin.bag.vaccination.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * Needed to adapt the mapToCookie Method to ensure that httpOnly = false is set.
 * By default, using the httponly method did not work.
 *
 * This copied class was reduced class to only the necessary parts.
 */
public final class CookieTokenRepository implements CsrfTokenRepository {
  static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";
  static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";
  static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";
  private static final String CSRF_TOKEN_REMOVED_ATTRIBUTE_NAME =
    org.springframework.security.web.csrf.CookieCsrfTokenRepository.class.getName().concat(".REMOVED");
  private final String parameterName = DEFAULT_CSRF_PARAMETER_NAME;
  private final String headerName = DEFAULT_CSRF_HEADER_NAME;
  private final String cookieName = DEFAULT_CSRF_COOKIE_NAME;
  private final String cookiePath;
  private String cookieDomain;

  public CookieTokenRepository(String frontendDomain) {
    cookiePath = "/";
    if (frontendDomain != null && !frontendDomain.isEmpty()) {
      cookieDomain = frontendDomain;
    }
  }

  public CsrfToken generateToken(HttpServletRequest request) {
    return new DefaultCsrfToken(this.headerName, this.parameterName, this.createNewToken());
  }

  private String getRequestContext(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    return !contextPath.isEmpty() ? contextPath : "/";
  }

  public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
    String tokenValue = token != null ? token.getToken() : "";
    boolean cookieHttpOnly = false;
    int cookieMaxAge = -1;
    ResponseCookie.ResponseCookieBuilder cookieBuilder =
      ResponseCookie.from(this.cookieName, tokenValue)
        .secure(request.isSecure())
        .path(StringUtils.hasLength(this.cookiePath) ? this.cookiePath : this.getRequestContext(request))
        .maxAge(token != null ? (long) cookieMaxAge : 0L)
        .httpOnly(cookieHttpOnly)
        .domain(this.cookieDomain);
    Cookie cookie = this.mapToCookie(cookieBuilder.build());
    response.addCookie(cookie);
    if (!StringUtils.hasLength(tokenValue)) {
      request.setAttribute(CSRF_TOKEN_REMOVED_ATTRIBUTE_NAME, Boolean.TRUE);
    } else {
      request.removeAttribute(CSRF_TOKEN_REMOVED_ATTRIBUTE_NAME);
    }
  }

  public CsrfToken loadToken(HttpServletRequest request) {
    if (Boolean.TRUE.equals(request.getAttribute(CSRF_TOKEN_REMOVED_ATTRIBUTE_NAME))) {
      return null;
    } else {
      Cookie cookie = WebUtils.getCookie(request, this.cookieName);
      if (cookie == null) {
        return null;
      } else {
        String token = cookie.getValue();
        return !StringUtils.hasLength(token) ? null : new DefaultCsrfToken(this.headerName, this.parameterName, token);
      }
    }
  }

  private String createNewToken() {
    return UUID.randomUUID().toString();
  }

  private Cookie mapToCookie(ResponseCookie responseCookie) {
    Cookie cookie = new Cookie(responseCookie.getName(), responseCookie.getValue());
    cookie.setSecure(responseCookie.isSecure());
    cookie.setPath(responseCookie.getPath());
    cookie.setMaxAge((int) responseCookie.getMaxAge().getSeconds());
    cookie.setAttribute("HttpOnly", "false");
    if (StringUtils.hasLength(responseCookie.getDomain())) {
      cookie.setDomain(responseCookie.getDomain());
    }

    if (StringUtils.hasText(responseCookie.getSameSite())) {
      cookie.setAttribute("SameSite", responseCookie.getSameSite());
    }

    return cookie;
  }
}
