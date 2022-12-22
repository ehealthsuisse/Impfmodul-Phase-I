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
 * {@link SAMLAuthentication#getSaml2Response()}
 *
 * Adapted class Saml2Authentication from spring dependency: spring-security-saml2-service-provider
 */
public class SAMLAuthentication extends AbstractAuthenticationToken {

  private final AuthenticatedPrincipal principal;
  private final String saml2Response;

  /**
   * Construct a {@link SAMLAuthentication} using the provided parameters. An authenticated user has
   * all right, i.e. we have no explicit grantedAuthorities
   *
   * @param principal the logged in user
   * @param saml2Response the SAML 2.0 response used to authenticate the user
   */
  public SAMLAuthentication(AuthenticatedPrincipal principal, String saml2Response) {
    super(AuthorityUtils.createAuthorityList("ROLE_USER"));
    Assert.notNull(principal, "principal cannot be null");
    Assert.hasText(saml2Response, "saml2Response cannot be null");
    this.principal = principal;
    this.saml2Response = saml2Response;
    setAuthenticated(true);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || (obj instanceof SAMLAuthentication
            && Objects.equal(((SAMLAuthentication) obj).principal, principal)
            && Objects.equal(((SAMLAuthentication) obj).saml2Response, saml2Response));
  }

  @Override
  public Object getCredentials() {
    return getSaml2Response();
  }

  @Override
  public Object getPrincipal() {
    return this.principal;
  }

  /**
   * Returns the SAML response object, as decoded XML. May contain encrypted elements
   *
   * @return string representation of the SAML Response XML object
   */
  public String getSaml2Response() {
    return this.saml2Response;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(principal, saml2Response);
  }
}
