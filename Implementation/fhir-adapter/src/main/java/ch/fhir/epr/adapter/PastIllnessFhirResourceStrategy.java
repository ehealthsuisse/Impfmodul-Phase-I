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
package ch.fhir.epr.adapter;

import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.stereotype.Component;

@Component
class PastIllnessFhirResourceStrategy implements FhirResourceStrategy<PastIllnessDTO, Condition> {

  @Override
  public Class<PastIllnessDTO> getDtoClass() {
    return PastIllnessDTO.class;
  }

  @Override
  public Class<Condition> getResourceClass() {
    return Condition.class;
  }

  @Override
  public SectionType getSectionType() {
    return SectionType.PAST_ILLNESSES;
  }

  @Override
  public String getResourceName() {
    return "Past Illness";
  }

  @Override
  public Bundle create(FhirConverter converter, Bundle bundle, PastIllnessDTO dto,
      PatientIdentifier patientIdentifier) {
    return converter.createPastIllness(bundle, dto, patientIdentifier);
  }

  @Override
  public PastIllnessDTO toDto(FhirAdapter adapter, Bundle bundle, Condition resource) {
    return adapter.getPastIllnessDTO(bundle, resource);
  }

  @Override
  public List<PastIllnessDTO> getRecordEntries(VaccinationRecordDTO vaccinationRecord) {
    return vaccinationRecord.getPastIllnesses();
  }
}
