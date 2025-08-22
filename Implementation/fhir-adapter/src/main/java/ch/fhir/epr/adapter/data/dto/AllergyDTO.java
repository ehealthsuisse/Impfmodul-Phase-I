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

import java.time.LocalDate;
import java.util.Objects;
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
  /** excluded from equals method for backward compatibility, there are older records without criticality */
  private ValueDTO criticality;
  private ValueDTO clinicalStatus;

  public AllergyDTO(String id, LocalDate occurrenceDate, ValueDTO code, ValueDTO criticality,
      ValueDTO clinicalStatus, ValueDTO verificationStatus, ValueDTO type, HumanNameDTO recorder,
      CommentDTO comment, String organization) {
    this.occurrenceDate = occurrenceDate;
    this.criticality = criticality;
    this.clinicalStatus = clinicalStatus;
    this.type = type;
    setId(id);
    setCode(code);
    setRecorder(recorder);
    setOrganization(organization);
    setComment(comment);
    setVerificationStatus(verificationStatus);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof AllergyDTO allergy)) {
      return false;
    }

    if (!super.equals(o)) {
      return false;
    }

    return Objects.equals(occurrenceDate, allergy.occurrenceDate) &&
        Objects.equals(type, allergy.type) &&
        Objects.equals(clinicalStatus, allergy.clinicalStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), occurrenceDate, type, clinicalStatus);
  }

  @Override
  public LocalDate getDateOfEvent() {
    return getOccurrenceDate();
  }
}
