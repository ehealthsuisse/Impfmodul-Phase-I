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

import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.hl7.fhir.r4.model.DomainResource;
import org.springframework.stereotype.Component;

@Component
public class FhirResourceStrategyRegistry {

  @Getter
  private final List<FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource>> strategies;
  private final Map<Class<? extends BaseDTO>, FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource>>
      strategiesByDtoClass;

  public FhirResourceStrategyRegistry(
      List<FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource>> strategies) {
    this.strategies = List.copyOf(strategies);
    this.strategiesByDtoClass = createStrategiesByDtoClass(strategies);
  }

  @SuppressWarnings("unchecked")
  public <T extends BaseDTO> FhirResourceStrategy<T, ? extends DomainResource> getStrategy(Class<T> dtoClass) {
    FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource> strategy = strategiesByDtoClass.get(dtoClass);
    if (strategy == null) {
      throw new TechnicalException("FHIR resource strategy not supported for class " + dtoClass.getSimpleName());
    }

    return (FhirResourceStrategy<T, ? extends DomainResource>) strategy;
  }

  private Map<Class<? extends BaseDTO>, FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource>>
      createStrategiesByDtoClass(
      List<FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource>> strategies) {
    Map<Class<? extends BaseDTO>, FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource>> result =
        new LinkedHashMap<>();
    for (FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource> strategy : strategies) {
      FhirResourceStrategy<? extends BaseDTO, ? extends DomainResource> previous =
          result.put(strategy.getDtoClass(), strategy);
      if (previous != null) {
        throw new TechnicalException("Duplicate FHIR resource strategy for class "
            + strategy.getDtoClass().getSimpleName());
      }
    }
    return result;
  }
}
