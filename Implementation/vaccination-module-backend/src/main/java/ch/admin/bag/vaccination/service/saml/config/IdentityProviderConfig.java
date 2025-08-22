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

import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.BasicX509Credential;

@Slf4j
@Getter
@Setter
public class IdentityProviderConfig {
  /** identifier used in the idp-config */
  private String identifier;

  /** Endpoint URL for HTTP-Redirect Binding URL */
  private String authnrequestURL;

  /** Endpoint URL to resolve the SAML artifact */
  private String artifactResolutionServiceURL;

  /** Endpoint URL for token renewal */
  private String securityTokenServiceURL;

  /** Endpoint URL for logout confirmation */
  private String logoutURL;

  /**
   * Customized entityId which overrides the default entity id. Necessary work around for HIN-IDP.
   */
  private String entityId;

  /**  Configuration properties for keystores used in SAML signing */
  private KeystoreProperties samlKeystore;

  /**  Configuration properties for keystores used for mutual TLS */
  private KeystoreProperties tlsKeystore;

  public Credential getSamlCredential(boolean isPrivateKey) {
    KeystoreProperties samlKeystoreProps = this.samlKeystore;
    try {
      KeyStore idpKeyStore = initKeyStore(samlKeystoreProps);
      KeyStore.Entry key = idpKeyStore.getEntry(samlKeystoreProps.getSpAlias(),
          isPrivateKey ? new PasswordProtection(samlKeystoreProps.getKeystorePassword().toCharArray())
              : null);
      BasicCredential credential = null;

      if (isPrivateKey) {
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) key;
        credential = new BasicX509Credential((X509Certificate) privateKeyEntry.getCertificate(),
            privateKeyEntry.getPrivateKey());

        ((BasicX509Credential) credential).setEntityCertificateChain(
            Arrays.asList((X509Certificate[]) privateKeyEntry.getCertificateChain()));
      } else {
        KeyStore.TrustedCertificateEntry certificate = (KeyStore.TrustedCertificateEntry) key;
        credential = new BasicCredential(certificate.getTrustedCertificate().getPublicKey());
      }

      credential.setEntityId(samlKeystoreProps.getSpAlias());
      credential.setUsageType(UsageType.UNSPECIFIED);
      return credential;
    } catch (Exception ex) {
      throw new TechnicalException("Something went wrong reading credentials", ex);
    }
  }

  private KeyStore initKeyStore(KeystoreProperties samlKeystoreProps) {
    InputStream keyStoreInputStream = null;

    try {
      KeyStore keystore = KeyStore.getInstance(samlKeystoreProps.getKeystoreType());
      keyStoreInputStream = Files.newInputStream(Path.of(samlKeystoreProps.getKeystorePath()));
      keystore.load(keyStoreInputStream, samlKeystoreProps.getKeystorePassword().toCharArray());
      return keystore;
    } catch (IOException | GeneralSecurityException ex) {
      log.error("Could not initiate keystore, error message {}", ex.getMessage());
      throw new TechnicalException(ex.getMessage(), ex);
    } finally {
      silentClose(keyStoreInputStream);
    }
  }

  private void silentClose(InputStream inputStream) {
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException ex) {
        // ignore silently
      }
    }
  }
}
