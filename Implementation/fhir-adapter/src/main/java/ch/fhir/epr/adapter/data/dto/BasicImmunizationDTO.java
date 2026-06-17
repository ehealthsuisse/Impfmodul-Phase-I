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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class BasicImmunizationDTO extends BaseDTO {
  private ValueDTO category;
  private ValueDTO clinicalStatus;
  private LocalDate onsetDate;
  private LocalDate recordedDate;

  public BasicImmunizationDTO(String id, ValueDTO code, ValueDTO category, ValueDTO clinicalStatus,
      ValueDTO verificationStatus, LocalDate onsetDate, LocalDate recordedDate, CommentDTO comment) {
    this.category = category;
    this.clinicalStatus = clinicalStatus;
    this.onsetDate = onsetDate;
    this.recordedDate = recordedDate;
    setId(id);
    setCode(code);
    setComment(comment);
    setVerificationStatus(verificationStatus);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BasicImmunizationDTO other)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    return Objects.equals(category, other.category)
        && Objects.equals(clinicalStatus, other.clinicalStatus)
        && Objects.equals(onsetDate, other.onsetDate)
        && Objects.equals(recordedDate, other.recordedDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), category, clinicalStatus, onsetDate, recordedDate);
  }

  @Override
  public LocalDate getDateOfEvent() {
    return getOnsetDate();
  }

  @JsonIgnore
  @Override
  public HumanNameDTO getRecorder() {
    throw new UnsupportedOperationException();
  }

  @JsonIgnore
  @Override
  public void setRecorder(HumanNameDTO recorder) {
    throw new UnsupportedOperationException();
  }

  @JsonIgnore
  @Override
  public String getOrganization() {
    throw new UnsupportedOperationException();
  }

  @JsonIgnore
  @Override
  public void setOrganization(String organization) {
    throw new UnsupportedOperationException();
  }
}
