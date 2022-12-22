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
package ch.admin.bag.vaccination.service.saml.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import ch.fhir.epr.adapter.exception.TechnicalException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class IdpProviderTest {

  @Autowired
  private IdpProvider idpProvider;

  @Test
  void idpProvider_values() {
    assertThat(idpProvider).isNotNull();
    assertThat(idpProvider.getSupportedProvider()).isNotNull();

    assertThrows(TechnicalException.class,
        () -> idpProvider.getProviderConfig(IdentityProviders.UNKNOWN.name()));

    assertThat(idpProvider.getProviderConfig(IdentityProviders.DUMMY.name())).isNotNull();
    assertThat(idpProvider.getProviderConfig(IdentityProviders.GAZELLE.name())).isNotNull();
    assertThat(idpProvider.getProviderConfig(IdentityProviders.GAZELLE.name()).getIdentifier())
        .isEqualTo(IdentityProviders.GAZELLE.name());
    assertThat(idpProvider.getProviderConfig(IdentityProviders.GAZELLE.name())
        .getAuthnrequestURL()).contains("ehealthsuisse");
    assertThat(idpProvider.getProviderConfig(IdentityProviders.GAZELLE.name())
        .getArtifactResolutionServiceURL()).contains("ehealthsuisse");
  }

}
