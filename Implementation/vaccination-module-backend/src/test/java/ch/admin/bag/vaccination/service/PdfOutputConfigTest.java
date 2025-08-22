/**
 * Copyright (c) 2023 eHealth Suisse
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class PdfOutputConfigTest {

  @Autowired
  private PdfOutputConfig config;

  @Test
  void basicVaccinationCodes() {
    assertThat(config.getBasicVaccination().getCodes().size()).isEqualTo(11);
    assertThat(config.getBasicVaccination().getCodes().getFirst()).isEqualTo("397430003");
    assertThat(config.getBasicVaccination().getCodes().get(10)).isEqualTo("240532009");
  }


  @Test
  void otherVaccinationCodes() {
    assertThat(config.getOtherVaccination().getCodes().size()).isEqualTo(18);
    assertThat(config.getOtherVaccination().getCodes().getFirst()).isEqualTo("40468003");
    assertThat(config.getOtherVaccination().getCodes().get(6)).isEqualTo("840539006");
  }
}
