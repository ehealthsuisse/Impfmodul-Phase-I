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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

public class VaccinationRecordTest {
  @Test
  void testConstructor_shouldDelegateSettingToBaseDTO() {
    VaccinationRecordDTO vaccinationRecord = new VaccinationRecordDTO(
        new AuthorDTO(new HumanNameDTO("firstNameA", "lastNameA", "prefixA", LocalDate.of(11, 11, 11), "genderA")), //
        new HumanNameDTO("firstNameB", "lastNameB", "prefixB", LocalDate.of(12, 12, 12), "genderB"), //
        List.of(new AllergyDTO()), //
        List.of(new PastIllnessDTO()), //
        List.of(new VaccinationDTO()), //
        List.of(new MedicalProblemDTO())//
    );
    vaccinationRecord.setCreatedAt(LocalDateTime.parse("2025-04-28T11:07:08"));

    assertNull(vaccinationRecord.getJson());
    assertNull(vaccinationRecord.getId());
    assertNull(vaccinationRecord.getRelatedId());

    assertEquals("firstNameA", vaccinationRecord.getAuthor().getUser().getFirstName());
    assertEquals("lastNameA", vaccinationRecord.getAuthor().getUser().getLastName());
    assertEquals("prefixA", vaccinationRecord.getAuthor().getUser().getPrefix());
    assertEquals(LocalDate.of(11, 11, 11), vaccinationRecord.getAuthor().getUser().getBirthday());
    assertEquals("genderA", vaccinationRecord.getAuthor().getUser().getGender());

    assertEquals("firstNameB", vaccinationRecord.getPatient().getFirstName());
    assertEquals("lastNameB", vaccinationRecord.getPatient().getLastName());
    assertEquals("prefixB", vaccinationRecord.getPatient().getPrefix());
    assertEquals(LocalDate.of(12, 12, 12), vaccinationRecord.getPatient().getBirthday());
    assertEquals("genderB", vaccinationRecord.getPatient().getGender());

    assertEquals(1, vaccinationRecord.getAllergies().size());
    assertEquals(1, vaccinationRecord.getPastIllnesses().size());
    assertEquals(1, vaccinationRecord.getVaccinations().size());
    assertEquals(1, vaccinationRecord.getMedicalProblems().size());
    assertEquals(LocalDateTime.parse("2025-04-28T11:07:08"), vaccinationRecord.getCreatedAt());
  }


  @Test
  void testConstructorAllArgsConstructor() {
    VaccinationRecordDTO vaccinationRecord = new VaccinationRecordDTO("de",
        new HumanNameDTO("firstNameA", "lastNameA", "prefixA",
            LocalDate.of(11, 11, 11), "genderA"), //
        List.of(new AllergyDTO()), //
        List.of(new PastIllnessDTO()), //
        List.of(new VaccinationDTO()), //
        List.of(new MedicalProblemDTO()),//
        List.of(new ValueDTO()) //
    );
    vaccinationRecord.setCreatedAt(LocalDateTime.parse("2025-04-28T11:07:08"));

    assertNull(vaccinationRecord.getJson());
    assertNull(vaccinationRecord.getId());
    assertNull(vaccinationRecord.getRelatedId());

    assertEquals("firstNameA", vaccinationRecord.getPatient().getFirstName());
    assertEquals("lastNameA", vaccinationRecord.getPatient().getLastName());
    assertEquals("prefixA", vaccinationRecord.getPatient().getPrefix());
    assertEquals(LocalDate.of(11, 11, 11), vaccinationRecord.getPatient().getBirthday());
    assertEquals("genderA", vaccinationRecord.getPatient().getGender());

    assertEquals(1, vaccinationRecord.getAllergies().size());
    assertEquals(1, vaccinationRecord.getPastIllnesses().size());
    assertEquals(1, vaccinationRecord.getVaccinations().size());
    assertEquals(1, vaccinationRecord.getMedicalProblems().size());
    assertEquals(1, vaccinationRecord.getI18nTargetDiseases().size());
    assertEquals(LocalDateTime.parse("2025-04-28T11:07:08"), vaccinationRecord.getCreatedAt());

  }
  @Test
  void testConstructorNoArgsConstructor() {
    VaccinationRecordDTO vaccinationRecord = new VaccinationRecordDTO();
    assertNull(vaccinationRecord.getJson());
    assertNull(vaccinationRecord.getId());
    assertNull(vaccinationRecord.getRelatedId());
    assertNull(vaccinationRecord.getAuthor());
    assertNull(vaccinationRecord.getPatient());
    assertNull(vaccinationRecord.getAllergies());
    assertNull(vaccinationRecord.getPastIllnesses());
    assertNull(vaccinationRecord.getVaccinations());
    assertNull(vaccinationRecord.getMedicalProblems());
    assertNull(vaccinationRecord.getCreatedAt());
    assertThrows(UnsupportedOperationException.class, vaccinationRecord::getDateOfEvent);
  }
}

