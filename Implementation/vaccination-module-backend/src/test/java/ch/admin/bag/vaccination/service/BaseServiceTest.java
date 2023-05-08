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

import static org.junit.jupiter.api.Assertions.assertThrows;
import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.fhir.epr.adapter.exception.TechnicalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base Service Test using allergy service as example
 */
@SpringBootTest
@ActiveProfiles("test")
class BaseServiceTest {
  @Autowired
  private AllergyService allergyService;
  @Autowired
  private ProfileConfig profileConfig;

  @BeforeEach
  void before() {
    profileConfig.setLocalMode(false);
  }

  @Test
  void create_invalidPatientInformation_throwException() {
    assertThrows(NullPointerException.class, () -> allergyService.getAll(null, null, null, null, null));
    assertThrows(NullPointerException.class, () -> allergyService.getAll("communityId", null, null, null, null));
    assertThrows(NullPointerException.class, () -> allergyService.getAll("communityId", "laaoid", null, null, null));
    // all parameters are set, however we do not have a valid community, a different exception is thrown
    assertThrows(TechnicalException.class, () -> allergyService.getAll("communityId", "laaoid", "localId", null, null));
  }


}
