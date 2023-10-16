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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class VaccinationRecordTest {

  @Test
  void testConstructorAllArgsConstructor() {
    VaccinationRecordDTO vaccinationRecord = new VaccinationRecordDTO(
        new AuthorDTO(new HumanNameDTO("firstNameA", "lastNameA", "prefixA", LocalDate.of(11, 11, 11), "genderA")), //
        new HumanNameDTO("firstNameB", "lastNameB", "prefixB", LocalDate.of(12, 12, 12), "genderB"), //
        Arrays.asList(new AllergyDTO()), //
        Arrays.asList(new PastIllnessDTO()), //
        Arrays.asList(new VaccinationDTO()), //
        Arrays.asList(new MedicalProblemDTO())//
    );

    assertNull(vaccinationRecord.getJson());
    assertNull(vaccinationRecord.getId());
    assertNull(vaccinationRecord.getRelatedId());

    assertEquals(vaccinationRecord.getAuthor().getUser().getFirstName(), "firstNameA");
    assertEquals(vaccinationRecord.getAuthor().getUser().getLastName(), "lastNameA");
    assertEquals(vaccinationRecord.getAuthor().getUser().getPrefix(), "prefixA");
    assertEquals(vaccinationRecord.getAuthor().getUser().getBirthday(), LocalDate.of(11, 11, 11));
    assertEquals(vaccinationRecord.getAuthor().getUser().getGender(), "genderA");

    assertEquals(vaccinationRecord.getPatient().getFirstName(), "firstNameB");
    assertEquals(vaccinationRecord.getPatient().getLastName(), "lastNameB");
    assertEquals(vaccinationRecord.getPatient().getPrefix(), "prefixB");
    assertEquals(vaccinationRecord.getPatient().getBirthday(), LocalDate.of(12, 12, 12));
    assertEquals(vaccinationRecord.getPatient().getGender(), "genderB");

    assertEquals(vaccinationRecord.getAllergies().size(), 1);
    assertEquals(vaccinationRecord.getPastIllnesses().size(), 1);
    assertEquals(vaccinationRecord.getVaccinations().size(), 1);
    assertEquals(vaccinationRecord.getMedicalProblems().size(), 1);
  }

  @Test
  void testConstructorNoArgsConstructor() {
    VaccinationRecordDTO vaccinationRecord = new VaccinationRecordDTO();
    assertNull(vaccinationRecord.getJson());
    assertNull(vaccinationRecord.getId());
    assertNull(vaccinationRecord.getRelatedId());
    assertNull(vaccinationRecord.getAuthor());
    assertNull(vaccinationRecord.getPatient());

    assertNull(vaccinationRecord.getAuthor());

    assertNull(vaccinationRecord.getAllergies());
    assertNull(vaccinationRecord.getPastIllnesses());
    assertNull(vaccinationRecord.getVaccinations());
    assertNull(vaccinationRecord.getMedicalProblems());
    assertThrows(UnsupportedOperationException.class, () -> vaccinationRecord.getDateOfEvent());
  }
}

