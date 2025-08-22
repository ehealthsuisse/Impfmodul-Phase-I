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

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.util.Assert;

/**
 * An implementation of an {@link AbstractAuthenticationToken} that represents an authenticated SAML
 * 2.0 {@link Authentication}.
 * <p>
 * The {@link Authentication} associates valid SAML assertion data with a Spring Security
 * authentication object The complete assertion is contained in the object in String format,
 * {@link SAMLAuthentication#getAssertion()}
 *
 * Adapted class Saml2Authentication from spring dependency: spring-security-saml2-service-provider
 */
public class SAMLAuthentication extends AbstractAuthenticationToken {

  @Getter
  private String idp;
  @Getter
  private String artifact;

  private AuthenticatedPrincipal principal;

  @Setter
  @Getter
  private Assertion assertion;


  /**
   * Construct a {@link SAMLAuthentication} using the provided parameters. An authenticated user has
   * all right, i.e. we have no explicit grantedAuthorities
   *
   * @param principal the logged in user
   * @param saml2Response the SAML 2.0 response used to authenticate the user
   */
  public SAMLAuthentication(AuthenticatedPrincipal principal, String idp, Assertion assertion) {
    super(AuthorityUtils.createAuthorityList("ROLE_USER"));
    Assert.notNull(idp, "idp cannot be null");
    Assert.notNull(principal, "principal cannot be null");
    Assert.notNull(assertion, "assertion cannot be null");
    this.idp = idp;
    this.principal = principal;
    this.assertion = assertion;
    setAuthenticated(true);
  }

  /**
   * Constructor of an unauthenticated session. Nothing is known apart from the registration id
   *
   * @param idp string of the configured idp, must match idp-config
   * @param artifact saml artifact which is a one-time-token for the saml authentication
   */
  public SAMLAuthentication(String idp, String artifact) {
    super(AuthorityUtils.NO_AUTHORITIES);
    this.idp = idp;
    this.artifact = artifact;
    setAuthenticated(false);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || (obj instanceof SAMLAuthentication samla
            && Objects.equal(samla.principal, principal)
            && Objects.equal(samla.assertion, assertion));
  }

  @Override
  public Object getCredentials() {
    return getAssertion();
  }

  @Override
  public Object getPrincipal() {
    return this.principal;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(principal, assertion);
  }
}
