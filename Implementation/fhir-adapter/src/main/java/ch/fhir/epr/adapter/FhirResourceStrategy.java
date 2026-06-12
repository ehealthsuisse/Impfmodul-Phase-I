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
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;

/**
 * Strategy for converting one vaccination module DTO type to and from the corresponding FHIR
 * resource type.
 *
 * @param <T> vaccination module DTO type handled by this strategy
 * @param <R> FHIR resource type handled by this strategy
 */
public interface FhirResourceStrategy<T extends BaseDTO, R extends DomainResource> {

  /**
   * Returns the DTO class handled by this strategy.
   */
  Class<T> getDtoClass();

  /**
   * Returns the FHIR resource class handled by this strategy.
   */
  Class<R> getResourceClass();

  /**
   * Returns the vaccination record section where the resource belongs.
   */
  SectionType getSectionType();

  /**
   * Returns the FHIR resource name used in error messages.
   */
  String getResourceName();

  /**
   * Adds the DTO represented by this strategy to the provided bundle.
   */
  Bundle create(FhirConverter converter, Bundle bundle, T dto, PatientIdentifier patientIdentifier);

  /**
   * Converts a FHIR resource from the provided bundle to its DTO representation.
   */
  T toDto(FhirAdapter adapter, Bundle bundle, R resource);

  /**
   * Returns all vaccination record entries handled by this strategy.
   */
  List<T> getRecordEntries(VaccinationRecordDTO vaccinationRecord);
}
