/**
 * Copyright (c) 2026 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ch.fhir.epr.adapter.data.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class BasicImmunizationDTOTest {

  @Test
  void getDateOfDay_noInput_returnOnsetDate() {
    BasicImmunizationDTO dto = new BasicImmunizationDTO();
    LocalDate now = LocalDate.now();
    dto.setOnsetDate(now);

    assertEquals(now, dto.getDateOfEvent());
  }

  @Test
  void getRecorder_throwsUnsupportedOperationException() {
    BasicImmunizationDTO dto = new BasicImmunizationDTO();
    assertThrows(UnsupportedOperationException.class, dto::getRecorder);
  }

  @Test
  void setRecorder_throwsUnsupportedOperationException() {
    BasicImmunizationDTO dto = new BasicImmunizationDTO();
    assertThrows(UnsupportedOperationException.class, () -> dto.setRecorder(null));
  }

  @Test
  void getOrganization_throwsUnsupportedOperationException() {
    BasicImmunizationDTO dto = new BasicImmunizationDTO();
    assertThrows(UnsupportedOperationException.class, dto::getOrganization);
  }

  @Test
  void setOrganization_throwsUnsupportedOperationException() {
    BasicImmunizationDTO dto = new BasicImmunizationDTO();
    assertThrows(UnsupportedOperationException.class, () -> dto.setOrganization(null));
  }

}
