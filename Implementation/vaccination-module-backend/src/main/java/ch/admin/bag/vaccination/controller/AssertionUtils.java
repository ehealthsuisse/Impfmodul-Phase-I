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

import ch.admin.bag.vaccination.service.saml.SAMLService;
import javax.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.projecthusky.xua.saml2.Assertion;
import org.projecthusky.xua.saml2.impl.AssertionBuilderImpl;
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
   * Get the assertion from session and transforms it to be compatible with the husky framework.
   *
   * @return {@link Assertion}
   */
  public static org.projecthusky.xua.saml2.Assertion getAssertionFromSession() {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    org.opensaml.saml.saml2.core.Assertion assertion =
        (org.opensaml.saml.saml2.core.Assertion) request.getSession()
            .getAttribute(SAMLService.IDP_ASSERTION);

    if (assertion == null) {
      // could be occuring during initial call but should not.
      log.warn("No idp assertion available, might indicate a misbehaviour.");
      return null;
    }

    org.projecthusky.xua.saml2.Assertion huskyIdpAssertion = new AssertionBuilderImpl().create(assertion);
    return huskyIdpAssertion;
  }
}
