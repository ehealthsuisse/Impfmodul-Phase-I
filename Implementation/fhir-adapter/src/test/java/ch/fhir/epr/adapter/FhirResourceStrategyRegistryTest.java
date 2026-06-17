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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Immunization;
import org.junit.jupiter.api.Test;

class FhirResourceStrategyRegistryTest {

  @Test
  void getStrategy_supportedDto_returnsMatchingStrategy() {
    TestVaccinationStrategy vaccinationStrategy = new TestVaccinationStrategy();
    FhirResourceStrategyRegistry registry =
        new FhirResourceStrategyRegistry(List.of(vaccinationStrategy));

    FhirResourceStrategy<VaccinationDTO, ? extends DomainResource> result =
        registry.getStrategy(VaccinationDTO.class);

    assertSame(vaccinationStrategy, result);
  }

  @Test
  void getStrategies_returnsConfiguredStrategiesInOrder() {
    TestVaccinationStrategy vaccinationStrategy = new TestVaccinationStrategy();
    TestAllergyStrategy allergyStrategy = new TestAllergyStrategy();
    FhirResourceStrategyRegistry registry =
        new FhirResourceStrategyRegistry(List.of(vaccinationStrategy, allergyStrategy));

    List<FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource>> result = registry.getStrategies();

    assertEquals(List.of(vaccinationStrategy, allergyStrategy), result);
  }

  @Test
  void getStrategy_unsupportedDto_throwsTechnicalException() {
    FhirResourceStrategyRegistry registry =
        new FhirResourceStrategyRegistry(List.of(new TestVaccinationStrategy()));

    TechnicalException exception = assertThrows(TechnicalException.class,
        () -> registry.getStrategy(AllergyDTO.class));

    assertEquals("FHIR resource strategy not supported for class AllergyDTO", exception.getMessage());
  }

  @Test
  void constructor_duplicateDtoStrategies_throwsTechnicalException() {
    TechnicalException exception = assertThrows(TechnicalException.class,
        () -> new FhirResourceStrategyRegistry(List.of(new TestVaccinationStrategy(),
            new TestDuplicateVaccinationStrategy())));

    assertEquals("Duplicate FHIR resource strategy for class VaccinationDTO", exception.getMessage());
  }

  private static class TestVaccinationStrategy implements FhirResourceStrategy<VaccinationDTO, Immunization> {

    @Override
    public Class<VaccinationDTO> getDtoClass() {
      return VaccinationDTO.class;
    }

    @Override
    public Class<Immunization> getResourceClass() {
      return Immunization.class;
    }

    @Override
    public SectionType getSectionType() {
      return SectionType.IMMUNIZATION;
    }

    @Override
    public String getResourceName() {
      return "Immunization";
    }

    @Override
    public Bundle create(FhirConverter converter, Bundle bundle, VaccinationDTO dto,
        PatientIdentifier patientIdentifier) {
      return bundle;
    }

    @Override
    public VaccinationDTO toDto(FhirAdapter adapter, Bundle bundle, Immunization resource) {
      return null;
    }

    @Override
    public List<VaccinationDTO> getRecordEntries(VaccinationRecordDTO vaccinationRecord) {
      return vaccinationRecord.getVaccinations();
    }
  }

  private static class TestDuplicateVaccinationStrategy extends TestVaccinationStrategy {
  }

  private static class TestAllergyStrategy implements FhirResourceStrategy<AllergyDTO, DomainResource> {

    @Override
    public Class<AllergyDTO> getDtoClass() {
      return AllergyDTO.class;
    }

    @Override
    public Class<DomainResource> getResourceClass() {
      return DomainResource.class;
    }

    @Override
    public SectionType getSectionType() {
      return SectionType.ALLERGIES;
    }

    @Override
    public String getResourceName() {
      return "AllergyIntolerance";
    }

    @Override
    public Bundle create(FhirConverter converter, Bundle bundle, AllergyDTO dto,
        PatientIdentifier patientIdentifier) {
      return bundle;
    }

    @Override
    public AllergyDTO toDto(FhirAdapter adapter, Bundle bundle, DomainResource resource) {
      return null;
    }

    @Override
    public List<AllergyDTO> getRecordEntries(VaccinationRecordDTO vaccinationRecord) {
      return vaccinationRecord.getAllergies();
    }
  }
}
