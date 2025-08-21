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
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class IdentityProviderConfigTest {

  private static final String IDENTIFIER = "identifier";
  private static final String AUTHNREQUEST_URL = "authnrequest url";
  private static final String ARTIFACT_RESOLUTION_URL = "artifact resolution url";
  private static final String ENTITY_ID = "entityID";

  @Test
  void setter_validValues_getterReturnSameValue() {
    IdentityProviderConfig config = new IdentityProviderConfig();
    config.setIdentifier(IDENTIFIER);
    config.setAuthnrequestURL(AUTHNREQUEST_URL);
    config.setArtifactResolutionServiceURL(ARTIFACT_RESOLUTION_URL);
    config.setEntityId(ENTITY_ID);

    assertThat(config).isNotNull();
    assertEquals(IDENTIFIER, config.getIdentifier());
    assertEquals(AUTHNREQUEST_URL, config.getAuthnrequestURL());
    assertEquals(ARTIFACT_RESOLUTION_URL, config.getArtifactResolutionServiceURL());
    assertEquals(ENTITY_ID, config.getEntityId());
  }

  @Test
  void getSamlCredentials_noInput_noExceptionOccures_validCertificate() {
    IdentityProviderConfig idpConfig = createIdpConfig();
    assertThat(idpConfig.getSamlCredential(true)).isNotNull();
  }

  private IdentityProviderConfig createIdpConfig() {
    KeystoreProperties samlKeystoreProps =
        new KeystoreProperties("PKCS12", "config/keystore-idp.p12", "8vX6_q73K.T1u2", "spkey");

    IdentityProviderConfig identityProviderConfig = new IdentityProviderConfig();
    identityProviderConfig.setSamlKeystore(samlKeystoreProps);
    return identityProviderConfig;
  }
}
