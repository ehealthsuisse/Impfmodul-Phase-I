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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VaccinationRecordDTO extends BaseDTO {
  private String lang;
  private HumanNameDTO patient;
  private List<AllergyDTO> allergies;
  private List<PastIllnessDTO> pastIllnesses;
  private List<VaccinationDTO> vaccinations;
  private List<MedicalProblemDTO> medicalProblems;
  private List<ValueDTO> i18nTargetDiseases;

  public VaccinationRecordDTO(AuthorDTO author, HumanNameDTO patient, List<AllergyDTO> allergies,
      List<PastIllnessDTO> pastIllnesses, List<VaccinationDTO> vaccinations, List<MedicalProblemDTO> medicalProblems) {
    setAuthor(author);
    lang = "en";
    this.patient = patient;
    this.allergies = allergies;
    this.pastIllnesses = pastIllnesses;
    this.vaccinations = vaccinations;
    this.medicalProblems = medicalProblems;
    setValidated(false);
    setJson(null);
  }

  @Override
  public LocalDate getDateOfEvent() {
    throw new UnsupportedOperationException();
  }

  /**
   * Allow vaccination record to have incomplete data otherwise it is hard to store any legacy
   * entries, i.e. use read option independent of the parameter.
   */
  @Override
  public void validate(boolean isReadAction) {
    allergies.forEach(dto -> dto.validate(true));
    pastIllnesses.forEach(dto -> dto.validate(true));
    vaccinations.forEach(dto -> dto.validate(true));
    medicalProblems.forEach(dto -> dto.validate(true));
  }
}
