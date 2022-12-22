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

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ReflectionUtils;

@SpringBootTest
class SignatureServiceTest {

  private static final long TIMESTAMP = 1663872899153l;
  private static final String SIMPLE_VALID_QUERY_STRING =
      "timestamp=1663872899153&sig=4teqsGrKzNhjfLCH+SYZE8Igu+cpwLpAxwCWdfGbWJA=";
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

  // only executable if correct keystore is configured in idp-config.yml.
  // @Test
  void getSamlCredentials_noInput_noExceptionOccures_validCertificate() {
    assertThat(signatureService.getSamlSPCredential()).isNotNull();
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
  void validateQueryString_missingTimestamp_returnFalse() {
    assertThat(signatureService.validateQueryString(QUERY_STRING_MISSING_TIMESTAMP)).isFalse();
  }

  @Test
  void validateQueryString_validTimeStampAndSignature_returnTrue() throws Exception {
    SignatureService customSignatureService = createServiceWithDefaultTimestamp();

    assertThat(customSignatureService.validateQueryString(SIMPLE_VALID_QUERY_STRING)).isTrue();
  }

  private SignatureService createServiceWithDefaultTimestamp() throws NoSuchFieldException {
    SignatureService customSignatureService = new SignatureService() {
      @Override
      protected long getCurrentTimestamp() {
        // sync timestamp with the one from the query string to test
        return TIMESTAMP;
      }
    };

    Field pskField = SignatureService.class.getDeclaredField("portalPresharedKey");
    pskField.setAccessible(true);
    Object pskValue = ReflectionUtils.getField(pskField, signatureService);
    ReflectionUtils.setField(pskField, customSignatureService, pskValue);
    return customSignatureService;
  }

}
