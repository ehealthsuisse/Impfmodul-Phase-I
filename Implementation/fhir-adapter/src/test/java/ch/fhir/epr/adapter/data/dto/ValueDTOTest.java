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
package ch.fhir.epr.adapter.data.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

public class ValueDTOTest {

  private final String NAME = "name";
  private final String CODE = "code";
  private final String SYSTEM = "system";

  @Test
  void constructor_validInput_getterReturnSameValue() {
    ValueDTO value = new ValueDTO(CODE, NAME, SYSTEM, false);

    assertEquals(NAME, value.getName());
    assertEquals(CODE, value.getCode());
    assertEquals(SYSTEM, value.getSystem());
    assertFalse(value.isAllowDisplay());
  }

  @Test
  void toString_validInput_returnCorrectFormatting() {
    ValueDTO value = new ValueDTO(CODE, NAME, SYSTEM);

    assertThat(value.toString()).hasToString(
        "ValueDTO(code=" + CODE + ", name=" + NAME + ", system=" + SYSTEM + ", allowDisplay=true)");
  }

  @Test
  void testEqualsAndHashCode() {
    ValueDTO value1 = new ValueDTO(CODE, NAME, SYSTEM);
    ValueDTO value2 = new ValueDTO(CODE, NAME, SYSTEM + "x");

    assertThat(value1.equals(value2)).isFalse();
    assertThat(value1.hashCode()).isNotEqualTo(value2.hashCode());
  }

  @Test
  void testNoArgsConstructor() {
    ValueDTO value = new ValueDTO(CODE, NAME, SYSTEM);

    assertThat(value.toString()).hasToString(
        "ValueDTO(code=" + CODE + ", name=" + NAME + ", system=" + SYSTEM + ", allowDisplay=true)");
  }

  @Test
  void testSetter() {
    ValueDTO value = new ValueDTO(CODE, NAME, SYSTEM);
    value.setCode(NAME);
    value.setName(SYSTEM);
    value.setSystem(CODE);
    value.setAllowDisplay(false);

    assertEquals(NAME, value.getCode());
    assertEquals(SYSTEM, value.getName());
    assertEquals(CODE, value.getSystem());
    assertFalse(value.isAllowDisplay());
  }
}


