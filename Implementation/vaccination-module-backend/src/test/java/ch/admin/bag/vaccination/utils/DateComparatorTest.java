/**
 * Copyright (c) 2024 eHealth Suisse
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
package ch.admin.bag.vaccination.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DateComparatorTest {

  @Test
  void unorderedLists_sortWithDateComparator_resultsInOrderedListsByDate() {
    List<AllergyDTO> allergyDTOS = DateComparator.sortByDateDesc(generateAllergyData(),
        Comparator.comparing(AllergyDTO::getOccurrenceDate));
    List<MedicalProblemDTO> medicalProblemDTOS = DateComparator.sortByDateDesc(generateMedicalProblemData(),
        Comparator.comparing(MedicalProblemDTO::getRecordedDate));
    List<PastIllnessDTO> pastIllnessDTOS = DateComparator.sortByDateDesc(generatePastIllnessData(),
        Comparator.comparing(PastIllnessDTO::getRecordedDate));

    assertEquals(allergyDTOS.getFirst().getOccurrenceDate(), LocalDate.of(2023, 8, 5));
    assertEquals(allergyDTOS.get(2).getOccurrenceDate(), LocalDate.of(2021, 3, 15));
    assertEquals(medicalProblemDTOS.getFirst().getRecordedDate(), LocalDate.of(2023, 7, 25));
    assertEquals(medicalProblemDTOS.get(2).getRecordedDate(), LocalDate.of(2021, 3, 5));
    assertEquals(pastIllnessDTOS.getFirst().getRecordedDate(), LocalDate.of(2023, 7, 25));
    assertEquals(pastIllnessDTOS.get(2).getRecordedDate(), LocalDate.of(2021, 3, 5));

  }

  public List<PastIllnessDTO> generatePastIllnessData() {
    List<PastIllnessDTO> data = new ArrayList<>();
    data.add(new PastIllnessDTO(
        new ValueDTO("ClinicalStatus1", "Clinical Status 1", null),
        LocalDate.of(2022, 5, 10),
        LocalDate.of(2022, 5, 20),
        LocalDate.of(2022, 5, 1)
    ));
    data.add(new PastIllnessDTO(
        new ValueDTO("ClinicalStatus2", "Clinical Status 2", null),
        LocalDate.of(2021, 3, 15),
        LocalDate.of(2021, 3, 25),
        LocalDate.of(2021, 3, 5)
    ));
    data.add(new PastIllnessDTO(
        new ValueDTO("ClinicalStatus3", "Clinical Status 3", null),
        LocalDate.of(2023, 8, 5),
        LocalDate.of(2023, 8, 15),
        LocalDate.of(2023, 7, 25)
    ));
    return data;
  }

  public List<MedicalProblemDTO> generateMedicalProblemData() {
    List<MedicalProblemDTO> data = new ArrayList<>();
    data.add(new MedicalProblemDTO(
        new ValueDTO("ClinicalStatus1", "Clinical Status 1", null),
        LocalDate.of(2022, 5, 10),
        LocalDate.of(2022, 5, 20),
        LocalDate.of(2022, 5, 1)
    ));
    data.add(new MedicalProblemDTO(
        new ValueDTO("ClinicalStatus2", "Clinical Status 2", null),
        LocalDate.of(2021, 3, 15),
        LocalDate.of(2021, 3, 25),
        LocalDate.of(2021, 3, 5)
    ));
    data.add(new MedicalProblemDTO(
        new ValueDTO("ClinicalStatus3", "Clinical Status 3", null),
        LocalDate.of(2023, 8, 5),
        LocalDate.of(2023, 8, 15),
        LocalDate.of(2023, 7, 25)
    ));
    return data;
  }

  public List<AllergyDTO> generateAllergyData() {
    List<AllergyDTO> data = new ArrayList<>();
    data.add(new AllergyDTO(
        LocalDate.of(2022, 5, 10),
        new ValueDTO("Type1", "Type 1", null),
        new ValueDTO("Criticality1", "Criticality 1", null),
        new ValueDTO("ClinicalStatus1", "Clinical Status 1", null)
    ));
    data.add(new AllergyDTO(
        LocalDate.of(2021, 3, 15),
        new ValueDTO("Type2", "Type 2", null),
        new ValueDTO("Criticality2", "Criticality 2", null),
        new ValueDTO("ClinicalStatus2", "Clinical Status 2", null)
    ));
    data.add(new AllergyDTO(
        LocalDate.of(2023, 8, 5),
        new ValueDTO("Type3", "Type 3", null),
        new ValueDTO("Criticality3", "Criticality 3", null),
        new ValueDTO("ClinicalStatus3", "Clinical Status 3", null)
    ));
    return data;
  }
}
