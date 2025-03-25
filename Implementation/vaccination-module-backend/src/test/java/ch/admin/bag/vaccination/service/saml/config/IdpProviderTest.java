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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ch.fhir.epr.adapter.exception.TechnicalException;

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

  @Test
  void idpProvider_initBean_hasDefaultSamlKeystoreValuesSet() {
    assertThat(idpProvider.getSamlKeystorePath()).isNotNull();
    assertThat(idpProvider.getSamlKeystorePath()).isEqualTo("config/keystore-idp.p12");
    assertThat(idpProvider.getSamlKeystoreType()).isNotNull();
    assertThat(idpProvider.getSamlKeystoreType()).isEqualTo("PKCS12");
    assertThat(idpProvider.getSamlKeystorePassword()).isNotNull();
    assertThat(idpProvider.getSamlSpAlias()).isNotNull();
    assertThat(idpProvider.getSamlSpAlias()).isEqualTo("spkey");
  }

  @Test
  void idpProvider_initBean_hasDefaultTlsKeystoreValuesSet() {
    assertThat(idpProvider.getTlsKeystorePath()).isNotNull();
    assertThat(idpProvider.getTlsKeystorePath()).isEqualTo("config/GazelleKeystore.p12");
    assertThat(idpProvider.getTlsKeystoreType()).isNotNull();
    assertThat(idpProvider.getTlsKeystoreType()).isEqualTo("PKCS12");
    assertThat(idpProvider.getTlsKeystorePassword()).isNotNull();
  }
}
