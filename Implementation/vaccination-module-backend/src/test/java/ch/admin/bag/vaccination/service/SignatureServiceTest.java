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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.admin.bag.vaccination.service.saml.config.IdentityProviderConfig;
import ch.admin.bag.vaccination.service.saml.config.KeystoreProperties;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Tests signature encoded with Base64.
 */
@SpringBootTest
@Slf4j
class SignatureServiceTest {

  private static final String PORTAL_PRESHAREDKEY = "portalKey";
  private static final String HMAC_ALGORITM = "HmacSHA256";
  private static final long TIMESTAMP = 1663872899153l;
  private static final String SIMPLE_VALID_QUERY_STRING =
      "timestamp=1663872899153&sig=4teqsGrKzNhjfLCH+SYZE8Igu+cpwLpAxwCWdfGbWJA=";
  private static final String SIMPLE_VALID_QUERY_STRING_HEX_ENCODE =
      "timestamp=1663872899153&sig=e2d7aab06acaccd8637cb087f9261913c220bbe729c0ba40c7009675f19b5890";
  private static final String QUERY_STRING_WITHOUT_SIGNATURE =
      "timestamp=1663872899153";
  private static final String QUERY_STRING_EMPTY_SIGNATURE =
      "timestamp=1663872899153&sig=";
  private static final String QUERY_STRING_WRONG_SIGNATURE =
      "timestamp=1663872899153&sig=abc";
  private static final String QUERY_STRING_MISSING_TIMESTAMP =
      "idp=idp&sig=dsWWjo1c76MlHpfGq5o/AfNqey3+X4kVzdYEGWqPrrI=";

  @Autowired
  private SignatureService signatureService;

  @Test
  void currentTimestamp_tsInSecondsComparedToNow_returnTrue() {
    boolean timestampInSeconds = true;
    ReflectionTestUtils.setField(signatureService, "isPortalTimestampInSeconds", timestampInSeconds);

    Long allowedDerivation = (Long) ReflectionTestUtils.getField(signatureService, "portalTimestampDerivation") / 1000;
    long tsInSeconds = signatureService.getCurrentTimestamp();
    long epochMilli = Instant.now().toEpochMilli();

    log.warn("Timstamp in sec: {}, Comparison: {}", tsInSeconds, epochMilli / 1000);
    assertTrue((epochMilli / 1000 - tsInSeconds) < allowedDerivation);
  }

  @Test
  void validateQueryString_disableTimestampCheck_invalidTimeStamp_returnTrue() throws Exception {
    Field timestampCheckField = SignatureService.class.getDeclaredField("portalTimestampCheck");
    timestampCheckField.setAccessible(true);
    ReflectionUtils.setField(timestampCheckField, signatureService, false);

    assertThat(signatureService.validateQueryString(SIMPLE_VALID_QUERY_STRING)).isTrue();

    // enable check again
    ReflectionUtils.setField(timestampCheckField, signatureService, true);
    assertThat(signatureService.validateQueryString(SIMPLE_VALID_QUERY_STRING)).isFalse();
  }

  @Test
  void validateQueryString_emptyOrWrongSignature_returnFalse() {
    assertThat(signatureService.validateQueryString(QUERY_STRING_WITHOUT_SIGNATURE)).isFalse();
    assertThat(signatureService.validateQueryString(QUERY_STRING_EMPTY_SIGNATURE)).isFalse();
    assertThat(signatureService.validateQueryString(QUERY_STRING_WRONG_SIGNATURE)).isFalse();
  }

  void validateQueryString_expiredTimestamp_returnFalse() {
    assertThat(signatureService.validateQueryString(SIMPLE_VALID_QUERY_STRING)).isFalse();
  }

  @Test
  void validateQueryString_missingSignature_returnFalse() {
    assertThat(signatureService.validateQueryString(QUERY_STRING_WITHOUT_SIGNATURE)).isFalse();
  }

  @Test
  void validateQueryString_missingTimestamp_returnFalse() throws Exception {
    Field timestampCheckField = SignatureService.class.getDeclaredField("portalTimestampCheck");
    timestampCheckField.setAccessible(true);
    ReflectionUtils.setField(timestampCheckField, signatureService, true);

    assertThat(signatureService.validateQueryString(QUERY_STRING_MISSING_TIMESTAMP)).isFalse();
  }

  @Test
  void validateQueryString_tsInSeconds_returnTrue() {
    boolean timestampInSeconds = true;
    ReflectionTestUtils.setField(signatureService, "isPortalTimestampInSeconds", timestampInSeconds);
    long epochInSeconds = Instant.now().toEpochMilli() / 1000;

    String queryString = "timestamp=" + epochInSeconds;
    queryString = addSignature(queryString).substring(1);

    assertTrue(signatureService.validateQueryString(queryString));
  }

  @Test
  void validateQueryString_validTimeStampAndSignature_returnTrue() throws Exception {
    SignatureService customSignatureService = createServiceWithDefaultTimestamp();
    ReflectionTestUtils.setField(customSignatureService, "encodeSignatureBase64", true);

    assertThat(customSignatureService.validateQueryString(SIMPLE_VALID_QUERY_STRING)).isTrue();

    ReflectionTestUtils.setField(customSignatureService, "encodeSignatureBase64", false);
    assertThat(customSignatureService.validateQueryString(SIMPLE_VALID_QUERY_STRING_HEX_ENCODE)).isTrue();
  }

  private String addSignature(String queryString) {
    try {
      byte[] calculateHMac = calculateHMac(PORTAL_PRESHAREDKEY, queryString);
      String encodedSignature = Base64.getEncoder().encodeToString(calculateHMac);
      String queryStringWithSignature =
          "?" + queryString + "&sig=" + encodedSignature;

      log.info("Resulting queryStringWithSignature {}", queryStringWithSignature);
      return queryStringWithSignature;
    } catch (GeneralSecurityException ex) {
      log.error("Generating signature failed: {}", ex.getMessage());
      throw new RuntimeException(ex);
    }
  }

  private byte[] calculateHMac(String key, String data) throws GeneralSecurityException {
    Mac hmac = Mac.getInstance(HMAC_ALGORITM);
    SecretKeySpec secretKey =
        new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITM);
    hmac.init(secretKey);

    return hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
  }

  private SignatureService createServiceWithDefaultTimestamp() {
    SignatureService customSignatureService = new SignatureService() {
      @Override
      protected long getCurrentTimestamp() {
        // sync timestamp with the one from the query string to test
        return TIMESTAMP;
      }
    };

    ReflectionTestUtils.setField(customSignatureService, "portalPresharedKey", PORTAL_PRESHAREDKEY);
    return customSignatureService;
  }
}
