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
public class MedicalProblemDTO extends BaseDTO {
  private ValueDTO clinicalStatus;
  private ValueDTO verificationStatus;
  private LocalDate begin;
  private LocalDate end;
  private LocalDate recordedDate;

  public MedicalProblemDTO(String id, ValueDTO code, ValueDTO clinicalStatus, ValueDTO verificationStatus,
      LocalDate recordedDate, LocalDate begin, LocalDate end, HumanNameDTO recorder, List<CommentDTO> comments,
      String organization) {
    this.begin = begin;
    this.end = end;
    this.clinicalStatus = clinicalStatus;
    this.verificationStatus = verificationStatus;
    this.recordedDate = recordedDate;
    setId(id);
    setCode(code);
    setRecorder(recorder);
    setOrganization(organization);
    setComments(comments);
  }
}
