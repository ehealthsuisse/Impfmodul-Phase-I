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

import ch.fhir.epr.adapter.utils.ValidationUtils;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class AllergyDTO extends BaseDTO {
  private LocalDate occurrenceDate;
  private ValueDTO type;
  private ValueDTO criticality;
  private ValueDTO clinicalStatus;
  private ValueDTO verificationStatus;

  public AllergyDTO(String id, LocalDate occurrenceDate, ValueDTO code, ValueDTO criticality,
      ValueDTO clinicalStatus, ValueDTO verificationStatus, ValueDTO type, HumanNameDTO recorder,
      List<CommentDTO> comments, String organization) {
    this.occurrenceDate = ValidationUtils.isDateNotNull("occurrenceDate", occurrenceDate);
    this.criticality = criticality;
    this.clinicalStatus = clinicalStatus;
    this.verificationStatus = verificationStatus;
    this.type = type;
    setId(id);
    setCode(code);
    setRecorder(recorder);
    setOrganization(organization);
    setComments(comments);
  }

  @Override
  public LocalDate getDateOfEvent() {
    return getOccurrenceDate();
  }
}
