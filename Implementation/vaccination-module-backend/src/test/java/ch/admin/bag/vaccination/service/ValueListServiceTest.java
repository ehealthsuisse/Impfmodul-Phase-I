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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ch.admin.bag.vaccination.data.dto.ValueListDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class ValueListServiceTest {

  @Autowired
  private ValueListService valueListService;

  @Test
  void getAllListOfValues_conditionClinicalStatusList_doOnlyDisplayActiveInactive() {
    List<ValueListDTO> valueListDTO = valueListService.getAllListOfValues();
    boolean foundResult = false;

    for (ValueListDTO list : valueListDTO) {
      if (ValueListService.CONDITION_CLINICAL_STATUS.equals(list.getName())) {
        foundResult = true;
        list.getEntries().forEach(entry -> {
          assertThat(entry.isAllowDisplay()).isEqualTo(entry.getCode().contains("active"));
        });
      }
    }

    assertThat(foundResult).isTrue();
  }

  @Test
  void getAllListOfValues_returnValues() {
    List<ValueListDTO> valueListDTO = valueListService.getAllListOfValues();
    assertThat(valueListDTO.size()).isEqualByComparingTo(16);
    assertFalse(valueListDTO.get(0).getName().contains("properties"));
    assertEquals(valueListDTO.get(0).getEntries().get(0).getName(),
        valueListDTO.get(0).getEntries().get(0).getName().trim());
  }

  @Test
  void getAllListOfValues_vaccinationList_doNotDisplayEntriesForNonSwissMedicSystem() {
    List<ValueListDTO> valueListDTO = valueListService.getAllListOfValues();
    boolean foundResult = false;

    for (ValueListDTO list : valueListDTO) {
      if (ValueListService.IMMUNIZATION_VACCINE_CODE_VALUELIST.equals(list.getName())) {
        foundResult = true;
        list.getEntries().forEach(entry -> {
          boolean shouldBeVisible = ValueListService.SWISSMEDIC_CS_SYSTEM.equals(entry.getSystem());
          assertThat(entry.isAllowDisplay()).isEqualTo(shouldBeVisible);
        });
      }
    }

    assertThat(foundResult).isTrue();
  }
}
