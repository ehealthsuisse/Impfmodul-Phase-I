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
package ch.fhir.epr.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.fhir.epr.TestMain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestMain.class)
@ActiveProfiles("test")
public class FhirConfigTest {

  @Autowired
  private FhirConfig fhirConfig;

  @Test
  void config() {
    assertThat(fhirConfig.getBaseURL()).isEqualTo("https://www.e-health-suisse.ch/");
  }

  @Test
  void roles() {
    assertThat(fhirConfig.isPatient(null)).isTrue();
    assertThat(fhirConfig.isPatient("REP")).isTrue();
    assertThat(fhirConfig.isPatient("PAT")).isTrue();
    assertThat(fhirConfig.isPatient("dummy")).isFalse();

    assertThat(fhirConfig.isPractitioner(null)).isFalse();
    assertThat(fhirConfig.isPractitioner("HCP")).isTrue();
    assertThat(fhirConfig.isPractitioner("ASS")).isTrue();
    assertThat(fhirConfig.isPractitioner("dummy")).isFalse();
  }
}
