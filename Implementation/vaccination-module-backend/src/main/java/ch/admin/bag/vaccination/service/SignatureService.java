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
package ch.admin.bag.vaccination.service;

import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import jakarta.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.BasicX509Credential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

/***
 * Simple service which is able to generate and validate signatures loading predefined keys.
 */
@Slf4j
@Service
public class SignatureService {
  private static final String SIGNATURE_URL_PARAMETER = "&sig=";
  private static final String HMAC_ALGORITM = "HmacSHA256";

  @Value("${portal.hmacpresharedkey}")
  private String portalPresharedKey;

  @Value("${portal.encodeSignatureBase64}")
  private boolean encodeSignatureBase64;

  @Value("${portal.activateTimestampCheck:false}")
  private boolean portalTimestampCheck;

  @Value("${portal.timestampAllowedDerivationMillis:2000}")
  private long portalTimestampDerivation;

  @Value("${portal.useTimestampInSeconds:false}")
  private boolean isPortalTimestampInSeconds;

  @Value("${sp.keystore.keystore-type}")
  private String serviceProviderKeystoreType;

  @Value("${sp.keystore.keystore-path}")
  private String serviceProviderKeystorePath;

  @Value("${sp.keystore.keystore-password}")
  private String serviceProviderKeystorePassword;

  @Value("${sp.keystore.sp-alias}")
  private String serviceProviderKeyAlias;

  public Credential getSamlSPCredential() {
    return getSamlCredential(serviceProviderKeyAlias, true);
  }

  @PostConstruct
  public void init() {
    if (portalPresharedKey == null || portalPresharedKey.isBlank()) {
      throw new IllegalArgumentException(
          "Portal Preshared key must be defined in config/portal-config.yaml");
    }
  }

  public boolean validateQueryString(String fullQueryString) {
    boolean isValid = true;

    try {
      isValid &= validateTimestamp(fullQueryString);
      isValid &= validateSignature(fullQueryString);
    } catch (GeneralSecurityException ex) {
      isValid = false;
      log.error("Query string could not be validated: {}", ex.getMessage());
    }

    return isValid;
  }

  // Hook method - overwrite only for testing purpose
  protected long getCurrentTimestamp() {
    return Instant.now().toEpochMilli() / (isPortalTimestampInSeconds ? 1000 : 1);
  }

  private String calculateHMac(String key, String data) throws GeneralSecurityException {
    Mac hmac = Mac.getInstance(HMAC_ALGORITM);
    SecretKeySpec secretKey =
        new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITM);
    hmac.init(secretKey);
    byte[] hmacByte = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

    return encodeSignatureBase64 ? Base64.getEncoder().encodeToString(hmacByte)
        : new String(Hex.encode(hmac.doFinal(data.getBytes(StandardCharsets.UTF_8))));
  }

  private Credential getSamlCredential(String alias, boolean isPrivateKey) {
    try {
      KeyStore idpKeyStore =
          initKeyStore(serviceProviderKeystorePath, serviceProviderKeystorePassword.toCharArray());
      KeyStore.Entry key = idpKeyStore.getEntry(alias,
          isPrivateKey ? new PasswordProtection(serviceProviderKeystorePassword.toCharArray())
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

      credential.setEntityId(alias);
      credential.setUsageType(UsageType.UNSPECIFIED);
      return credential;
    } catch (Exception ex) {
      throw new TechnicalException("Something went wrong reading credentials", ex);
    }
  }

  private KeyStore initKeyStore(String path, char[] password) {
    InputStream keyStoreInputStream = null;

    try {
      KeyStore keystore = KeyStore.getInstance(serviceProviderKeystoreType);
      keyStoreInputStream = Files.newInputStream(Path.of(path));
      keystore.load(keyStoreInputStream, password);
      return keystore;
    } catch (IOException | GeneralSecurityException ex) {
      log.error("Could not initiate keystore or truststore, error message {}", ex.getMessage());
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

  private boolean validateSignature(String fullQueryString) throws GeneralSecurityException {
    int signatureIndex =
        fullQueryString != null ? fullQueryString.toLowerCase().lastIndexOf(SIGNATURE_URL_PARAMETER)
            : -1;
    boolean containsSignature = fullQueryString != null
        && signatureIndex > 0
        && fullQueryString.lastIndexOf("&") == signatureIndex;

    if (containsSignature) {
      String queryString =
          fullQueryString.substring(0, fullQueryString.indexOf(SIGNATURE_URL_PARAMETER));
      String signatureString = fullQueryString.substring(signatureIndex + 5);

      boolean result = signatureString.equals(calculateHMac(portalPresharedKey, queryString));
      if (!result) {
        log.debug("Signature is not valid! Please check the portal-config.yml settings.");
      }
      return result;
    }
    log.debug("Signature could not be determined, please check that signature parameter 'sig' is provided "
        + "correctly at the end of the querystring.");
    return false;
  }

  private boolean validateTimestamp(String fullQueryString) {
    if (!portalTimestampCheck) {
      log.warn("Timestamp check is deactivated, this is not recommended for productive use!");
      return true;
    }

    long currentTimestamp = getCurrentTimestamp();
    int indexTimestamp = fullQueryString.indexOf("timestamp=");
    if (indexTimestamp >= 0) {
      int indexParameterAfterTimestamp = fullQueryString.indexOf("&", indexTimestamp + 1);

      if (indexParameterAfterTimestamp > 0) {
        String timestamp =
            fullQueryString.substring(indexTimestamp + 10, indexParameterAfterTimestamp);
        long timestampLong = Long.parseLong(timestamp);

        long derivation = Math.abs(currentTimestamp - timestampLong) * (isPortalTimestampInSeconds ? 1000 : 1);
        log.debug("Timestamp derivation check: {} ms derivation, allowed: {}", derivation,
            portalTimestampDerivation);

        boolean result = derivation <= portalTimestampDerivation;
        if (!result) {
          log.debug(
              "Provided timestamp is out of bounds and therefore failed! Please check the portal-config.yml settings.");
        }
        return result;
      }
    }
    log.debug("Timestamp could not be determined, please check that timestamp parameter 'timestamp' is "
        + "provided in the querystring.");
    return false;
  }

}
