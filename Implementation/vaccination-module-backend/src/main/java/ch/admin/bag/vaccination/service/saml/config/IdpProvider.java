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

import ch.fhir.epr.adapter.exception.ValidationException;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "idp")
@Getter
@Setter
public class IdpProvider {

  public static final String GAZELLE_IDP_IDENTIFIER = "GAZELLE";

  @Value("${sp.samlKeystore.keystore-type}")
  private String samlKeystoreType;

  @Value("${sp.samlKeystore.keystore-path}")
  private String samlKeystorePath;

  @Value("${sp.samlKeystore.keystore-password}")
  private String samlKeystorePassword;

  @Value("${sp.samlKeystore.sp-alias}")
  private String samlSpAlias;

  @Value("${sp.tlsKeystore.keystore-type}")
  private String tlsKeystoreType;

  @Value("${sp.tlsKeystore.keystore-path}")
  private String tlsKeystorePath;

  @Value("${sp.tlsKeystore.keystore-password}")
  private String tlsKeystorePassword;

  /** entityId which is registered at the IDP */
  private String knownEntityId;
  private List<IdentityProviderConfig> supportedProvider;

  @PostConstruct
  public void init() {
    if (supportedProvider != null) {
      supportedProvider.forEach(this::applyKeystoreDefaults);
    }
  }

  public IdentityProviderConfig getProviderConfig(String providerIdentifier) {
    return supportedProvider.stream()
             .filter(config -> config.getIdentifier().equalsIgnoreCase(providerIdentifier))
             .findFirst()
             .orElseThrow(() -> new ValidationException("Configuration for provider with identifier: "
                                                          + providerIdentifier + " not found."));
  }

  private void applyKeystoreDefaults(IdentityProviderConfig config) {
    config.setSamlKeystore(config.getSamlKeystore() != null ? config.getSamlKeystore() :
                             createDefaultKeystore(samlKeystoreType, samlKeystorePath, samlKeystorePassword, samlSpAlias));
    config.setTlsKeystore(config.getTlsKeystore() != null ? config.getTlsKeystore() :
                            createDefaultKeystore(tlsKeystoreType, tlsKeystorePath, tlsKeystorePassword, null));
  }

  private KeystoreProperties createDefaultKeystore(String keystoreType, String keystorePath, String keystorePassword,
                                                   String spAlias) {
    return new KeystoreProperties(keystoreType, keystorePath, keystorePassword, spAlias);
  }
}
