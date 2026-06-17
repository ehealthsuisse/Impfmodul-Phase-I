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
import ch.fhir.epr.adapter.data.dto.LaboratorySerologyDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.stereotype.Component;

@Component
class LaboratorySerologyFhirResourceStrategy implements FhirResourceStrategy<LaboratorySerologyDTO, Observation> {

  @Override
  public Class<LaboratorySerologyDTO> getDtoClass() {
    return LaboratorySerologyDTO.class;
  }

  @Override
  public Class<Observation> getResourceClass() {
    return Observation.class;
  }

  @Override
  public SectionType getSectionType() {
    return SectionType.LABORATORY_SEROLOGY;
  }

  @Override
  public String getResourceName() {
    return "Laboratory/Serology";
  }

  @Override
  public Bundle create(FhirConverter converter, Bundle bundle, LaboratorySerologyDTO dto,
      PatientIdentifier patientIdentifier) {
    return converter.createLaboratorySerology(bundle, dto, patientIdentifier);
  }

  @Override
  public LaboratorySerologyDTO toDto(FhirAdapter adapter, Bundle bundle, Observation resource) {
    return adapter.getLaboratorySerologyDTO(bundle, resource);
  }

  @Override
  public List<LaboratorySerologyDTO> getRecordEntries(VaccinationRecordDTO vaccinationRecord) {
    return vaccinationRecord.getLaboratorySerologies();
  }
}
