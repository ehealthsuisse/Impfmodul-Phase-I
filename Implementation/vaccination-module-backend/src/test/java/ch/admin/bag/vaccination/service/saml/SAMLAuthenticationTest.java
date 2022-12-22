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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.authority.AuthorityUtils;

@SpringBootTest
public class SAMLAuthenticationTest {

  private static final String SAML_RESPONSE = "samlResponse";

  @Test
  void constructor_validAttribute_gettersReturnSameData() {
    AuthenticatedPrincipal principal = mock(AuthenticatedPrincipal.class);
    SAMLAuthentication authentication = new SAMLAuthentication(principal, SAML_RESPONSE);

    assertThat(authentication.getPrincipal()).isEqualTo(principal);
    assertThat(authentication.getSaml2Response()).isEqualTo(SAML_RESPONSE);
    assertTrue(authentication.isAuthenticated());
    assertThat(authentication.getAuthorities())
        .isEqualTo(AuthorityUtils.createAuthorityList("ROLE_USER"));
  }

  @Test
  void equalsHashCode_differentClass_notEqual() {
    AuthenticatedPrincipal principal = mock(AuthenticatedPrincipal.class);
    SAMLAuthentication authentication = new SAMLAuthentication(principal, SAML_RESPONSE);

    int differentClassOrValue = -1;
    assertNotEquals(authentication, differentClassOrValue);
    assertNotEquals(authentication.hashCode(), differentClassOrValue);
  }

  @Test
  void equalsHashCode_differentPrincipal_notEqual() {
    AuthenticatedPrincipal principal = mock(AuthenticatedPrincipal.class);
    AuthenticatedPrincipal principal2 = mock(AuthenticatedPrincipal.class);
    SAMLAuthentication authentication = new SAMLAuthentication(principal, SAML_RESPONSE);
    SAMLAuthentication authentication2 = new SAMLAuthentication(principal2, SAML_RESPONSE);

    assertNotEquals(authentication, authentication2);
    assertNotEquals(authentication.hashCode(), authentication2.hashCode());
  }

  @Test
  void equalsHashCode_differentResponse_notEqual() {
    AuthenticatedPrincipal principal = mock(AuthenticatedPrincipal.class);
    SAMLAuthentication authentication = new SAMLAuthentication(principal, SAML_RESPONSE);
    SAMLAuthentication authentication2 = new SAMLAuthentication(principal, "Different Response");

    assertNotEquals(authentication, authentication2);
    assertNotEquals(authentication.hashCode(), authentication2.hashCode());
  }

  @Test
  void equalsHashCode_sameEntity_equal() {
    AuthenticatedPrincipal principal = mock(AuthenticatedPrincipal.class);
    SAMLAuthentication authentication = new SAMLAuthentication(principal, SAML_RESPONSE);

    assertEquals(authentication, authentication);
    assertEquals(authentication.hashCode(), authentication.hashCode());
  }

  @Test
  void equalsHashCode_similarEntity_equal() {
    AuthenticatedPrincipal principal = mock(AuthenticatedPrincipal.class);
    SAMLAuthentication authentication = new SAMLAuthentication(principal, SAML_RESPONSE);
    SAMLAuthentication authentication2 = new SAMLAuthentication(principal, SAML_RESPONSE);

    assertEquals(authentication, authentication2);
    assertEquals(authentication.hashCode(), authentication2.hashCode());
  }
}
