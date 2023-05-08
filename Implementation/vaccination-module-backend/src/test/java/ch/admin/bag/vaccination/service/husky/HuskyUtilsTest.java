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

package ch.admin.bag.vaccination.service.husky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.projecthusky.xua.hl7v3.impl.CodedWithEquivalentImpl;

/**
 * 
 * Test of the {@link HuskyUtils}
 *
 */
public class HuskyUtilsTest {
  @Test
  void testGetCodedRole() throws Exception {
    CodedWithEquivalentImpl roleCode = (CodedWithEquivalentImpl) HuskyUtils.getCodedRole("ASS");

    assertEquals(roleCode.getCode(), "ASS");
    assertEquals(roleCode.getDisplayName(), "Assistant");
    assertEquals(roleCode.getCodeSystem(), "2.16.756.5.30.1.127.3.10.6");
    assertEquals(roleCode.getNamespaces().iterator().next().getNamespaceURI(),
        org.projecthusky.xua.hl7v3.Role.DEFAULT_NS_URI);

    assertNull(HuskyUtils.getCodedRole("Dummy"));
  }


  @Test
  void testGetCodedPurposeOfUse() throws Exception {
    CodedWithEquivalentImpl roleCode = (CodedWithEquivalentImpl) HuskyUtils.getCodedPurposeOfUse("NORM");

    assertEquals(roleCode.getCode(), "NORM");
    assertEquals(roleCode.getDisplayName(), "Normal Access");
    assertEquals(roleCode.getCodeSystem(), "2.16.756.5.30.1.127.3.10.5");
    assertEquals(roleCode.getNamespaces().iterator().next().getNamespaceURI(),
        org.projecthusky.xua.hl7v3.PurposeOfUse.DEFAULT_NS_URI);

    assertNull(HuskyUtils.getCodedPurposeOfUse("Dummy"));
  }
}
