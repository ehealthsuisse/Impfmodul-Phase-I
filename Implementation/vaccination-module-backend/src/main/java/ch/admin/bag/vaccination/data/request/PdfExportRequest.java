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
package ch.admin.bag.vaccination.data.request;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.admin.bag.vaccination.data.dto.PdfExportOptionsDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request class for PDF export options and related inputs.
 */
@NoArgsConstructor
@Getter
public class PdfExportRequest {
  @Setter
  private List<ValueDTO> targetDiseases;
  private List<ValueDTO> vaccineCodes;
  private List<ValueDTO> allergyCodes;
  private List<ValueDTO> medicalProblemCodes;
  private List<ValueDTO> illnessCodes;
  private List<ValueDTO> basicImmunizationCodes;
  private List<ValueDTO> laboratorySerologyCodes;
  @Setter
  private PdfExportOptionsDTO pdfOptions;

  @JsonIgnore
  private Map<Pair<String, String>, String> vaccineCodeToName;
  @JsonIgnore
  private Map<Pair<String, String>, String> allergyCodeToName;
  @JsonIgnore
  private Map<Pair<String, String>, String> illnessCodeToName;
  @JsonIgnore
  private Map<Pair<String, String>, String> medicalProblemCodeToName;
  @JsonIgnore
  private Map<Pair<String, String>, String> basicImmunizationCodeToName;
  @JsonIgnore
  private Map<Pair<String, String>, String> laboratorySerologyCodeToName;

  public void setAllergyCodes(List<ValueDTO> allergyCodes) {
    this.allergyCodes = allergyCodes;
    allergyCodeToName = mapValuesFromCollection(allergyCodes);
  }

  public void setIllnessCodes(List<ValueDTO> illnessCodes) {
    this.illnessCodes = illnessCodes;
    illnessCodeToName = mapValuesFromCollection(illnessCodes);
  }

  public void setMedicalProblemCodes(List<ValueDTO> medicalProblemCodes) {
    this.medicalProblemCodes = medicalProblemCodes;
     medicalProblemCodeToName = mapValuesFromCollection(medicalProblemCodes);
  }

  public void setVaccineCodes(List<ValueDTO> vaccineCodes) {
    this.vaccineCodes = vaccineCodes;
    vaccineCodeToName = mapValuesFromCollection(vaccineCodes);
  }

  public void setBasicImmunizationCodes(List<ValueDTO> basicImmunizationCodes) {
    this.basicImmunizationCodes = basicImmunizationCodes;
    basicImmunizationCodeToName = mapValuesFromCollection(basicImmunizationCodes);
  }

  public void setLaboratorySerologyCodes(List<ValueDTO> laboratorySerologyCodes) {
    this.laboratorySerologyCodes = laboratorySerologyCodes;
    laboratorySerologyCodeToName = mapValuesFromCollection(laboratorySerologyCodes);
  }

  private static Map<Pair<String, String>, String> mapValuesFromCollection(List<ValueDTO> valueDtos) {
    return valueDtos == null ? null : valueDtos.stream().distinct()
               .collect(Collectors.toMap(value -> Pair.of(value.getCode(), value.getSystem()), ValueDTO::getName));
  }
}
