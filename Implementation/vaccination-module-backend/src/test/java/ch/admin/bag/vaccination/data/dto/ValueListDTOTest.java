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
package ch.admin.bag.vaccination.data.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ValueListDTOTest {

  private ValueListDTO valueListDTO;
  private final String NAME = "name";
  private final String SYSTEM = "system";
  private final String CODE = "code";
  private final boolean ALLOW_DISPLAY = true;

  @Test
  void contructorList_validInput_getterReturnSameValue() {
    ValueDTO value = new ValueDTO(NAME, CODE, SYSTEM, ALLOW_DISPLAY);
    List<ValueDTO> valueList = new ArrayList<>();
    valueList.add(value);

    valueListDTO = new ValueListDTO(NAME, valueList);

    assertEquals(NAME, valueListDTO.getName());
    assertEquals(1, valueListDTO.getEntries().size());
    assertEquals(value, valueListDTO.getEntries().getFirst());
  }

  @Test
  void toString_validInput_returnCorrectFormatting() {
    ValueDTO value = new ValueDTO(CODE, NAME, SYSTEM);
    ValueListDTO valueList = new ValueListDTO(NAME, List.of(value));

    assertThat(valueList.toString()).hasToString(
        "ValueListDTO(name=" + NAME + ", entries=[ValueDTO(code=" + CODE + ", name=" + NAME
            + ", system=" + SYSTEM + ", allowDisplay=" + ALLOW_DISPLAY + ")])");
  }
}
