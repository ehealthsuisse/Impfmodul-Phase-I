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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.HttpSessionUtils;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootTest
public class SAMLFilterTest {

  private static final String IDP_IDENTIFIER = "IDP-Identifier";
  private SAMLFilter samlFilter;

  @Autowired
  private ProfileConfig profileConfig;

  @MockBean
  private SAMLService samlService;

  @MockBean
  private FilterChain filterChain;

  @MockBean
  HttpServletResponse response;

  @BeforeEach
  public void setUp() {
    samlFilter = new SAMLFilter(samlService, profileConfig);
  }

  @Test
  void doFilter_nonSecuredURL_continueFilterChain() throws Exception {
    HttpServletRequest request = createMockRequest("/swagger");
    samlFilter.doFilter(request, response, filterChain);
    verify(filterChain).doFilter(request, response);

    request = createMockRequest("/actuator");
    samlFilter.doFilter(request, response, filterChain);
    verify(filterChain).doFilter(request, response);

    request = createMockRequest("/v3/api-docs");
    samlFilter.doFilter(request, response, filterChain);
    verify(filterChain).doFilter(request, response);

    request = createMockRequest("/signature");
    samlFilter.doFilter(request, response, filterChain);
    verify(filterChain).doFilter(request, response);

    request = createMockRequest("/saml/sso");
    samlFilter.doFilter(request, response, filterChain);
    verify(filterChain).doFilter(request, response);

    request = createMockRequest("/saml/isAuthenticated");
    samlFilter.doFilter(request, response, filterChain);
    verify(filterChain).doFilter(request, response);

    request = createMockRequest("/saml/logout");
    samlFilter.doFilter(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_securedURL_authenticationDeactivated_continueFilterChain() throws Exception {
    HttpServletRequest request = createMockRequest("/husky");
    deactivateAuthenticationAndSetDummyAuthentication(request);

    samlFilter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_securedURL_noAuthentication_noInitialCall_ignoreCall() throws Exception {
    HttpServletRequest request = createMockRequest("/husky");
    samlFilter.doFilter(request, response, filterChain);

    verify(samlService, never()).redirectToIdp(anyString(), eq(response));
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void doFilter_securedURL_noAuthentication_validInitalCall_forwardToIdP() throws Exception {
    HttpServletRequest request = createMockRequest("/husky");
    when(request.getSession(false).getAttribute(HttpSessionUtils.INITIAL_CALL_VALID)).thenReturn(true);

    samlFilter.doFilter(request, response, filterChain);

    verify(samlService).redirectToIdp(anyString(), eq(response));
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void doFilter_securedURL_withAuthentication_noContext_validInitialCall_forwardToIDP() throws Exception {
    HttpServletRequest request = createMockRequest("/husky", createAuthenticatedSession(null));
    when(request.getSession(false).getAttribute(HttpSessionUtils.INITIAL_CALL_VALID)).thenReturn(true);

    samlFilter.doFilter(request, response, filterChain);
    verify(samlService).redirectToIdp(anyString(), eq(response));
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void doFilter_securedURL_withAuthentication_withContext_callValidateOnService() throws Exception {
    SecurityContext context = mock(SecurityContext.class);
    HttpServletRequest request = createMockRequest("/husky", createAuthenticatedSession(context));

    samlFilter.doFilter(request, response, filterChain);

    verify(samlService).checkAndUpdateSessionInformation(request.getSession());
    verify(filterChain).doFilter(request, response);
  }

  private HttpSession createAuthenticatedSession(SecurityContext context) {
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(HttpSessionUtils.AUTHENTICATED_SESSION_ATTRIBUTE)).thenReturn(true);
    when(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
        .thenReturn(context);
    when(samlService.checkAndUpdateSessionInformation(eq(session))).thenReturn(true);

    return session;
  }

  private HttpServletRequest createMockRequest(String uri) {
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("sessionId");
    when(session.getAttribute(eq(HttpSessionUtils.IDP))).thenReturn(IDP_IDENTIFIER);
    return createMockRequest(uri, session);
  }

  private HttpServletRequest createMockRequest(String uri, HttpSession session) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(uri);
    when(request.getRequestURL()).thenReturn(new StringBuffer(uri));
    when(request.getSession()).thenReturn(session);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(eq(HttpSessionUtils.IDP))).thenReturn(IDP_IDENTIFIER);

    // include request in default servlet context
    ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(requestAttributes);
    return request;
  }

  private void deactivateAuthenticationAndSetDummyAuthentication(HttpServletRequest request)
      throws NoSuchFieldException {
    profileConfig.setSamlAuthenticationActive(false);
    Field field = SAMLService.class.getDeclaredField("sessionIdToSecurityContext");
    field.setAccessible(true);
    ReflectionUtils.setField(field,
        samlService,
        new ConcurrentHashMap<>());

    HttpSession session = request.getSession();
    SecurityContext context = mock(SecurityContext.class);
    when(session.getAttribute(HttpSessionUtils.AUTHENTICATED_SESSION_ATTRIBUTE)).thenReturn(true);
    when(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
        .thenReturn(context);
    when(samlService.checkAndUpdateSessionInformation(eq(session))).thenReturn(true);
  }

}
