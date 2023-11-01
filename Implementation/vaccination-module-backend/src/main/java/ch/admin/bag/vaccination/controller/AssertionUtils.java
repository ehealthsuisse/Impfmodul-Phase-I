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
package ch.admin.bag.vaccination.controller;

import ch.admin.bag.vaccination.service.saml.SAMLAuthentication;
import java.lang.reflect.Field;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.projecthusky.xua.saml2.Assertion;
import org.projecthusky.xua.saml2.impl.AssertionBuilderImpl;
import org.projecthusky.xua.saml2.impl.AssertionImpl;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility class to handle Assertion
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class AssertionUtils {

  /**
   * Important - do NOT use new AssertionBuilderImpl().create(assertion) as it will break the DOM of
   * the assertion object, e.g. the SignatureValue is removed! Therefor, we use reflection to avoid
   * this problem.
   */
  public static org.projecthusky.xua.saml2.Assertion convertSamlToHuskyAssertion(
      org.opensaml.saml.saml2.core.Assertion assertion) {
    org.projecthusky.xua.saml2.Assertion huskyIdpAssertion = new AssertionBuilderImpl().create();
    setAssertion(huskyIdpAssertion, assertion);
    return huskyIdpAssertion;
  }


  /**
   * Get the assertion from session and transforms it to be compatible with the husky framework.
   *
   * @return {@link Assertion}
   */
  public static org.projecthusky.xua.saml2.Assertion getAssertionFromSession() {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    HttpSession session = request.getSession(false);
    org.opensaml.saml.saml2.core.Assertion assertion =
        session != null ? ((SAMLAuthentication) ((SecurityContext) session
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).getAuthentication())
                .getAssertion()
            : null;

    if (assertion == null) {
      // could be occuring during initial call but should not.
      log.warn("No idp assertion available, can be ignored in local mode.");
      return null;
    }

    return convertSamlToHuskyAssertion(assertion);
  }


  private static void setAssertion(Assertion huskyIdpAssertion, org.opensaml.saml.saml2.core.Assertion assertion) {
    try {
      Field field = AssertionImpl.class.getDeclaredField("assertion");
      field.setAccessible(true);
      field.set(huskyIdpAssertion, assertion);
    } catch (Exception ex) {
      log.error("Converting opensaml assertion to husky assertion failed.", ex);
      throw new RuntimeException(ex);
    }
  }

}
