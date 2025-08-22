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
package ch.admin.bag.vaccination.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("unchecked")
public class LifeCycleServiceTest {
  @Autowired
  private LifeCycleService lifeCycleService;
  private VaccinationDTO[] vaccinationDTOs = new VaccinationDTO[4];
  private AllergyDTO[] allergieDTOs = new AllergyDTO[4];
  private PastIllnessDTO[] pastIllnessDTOs = new PastIllnessDTO[4];
  private MedicalProblemDTO[] problemDTOs = new MedicalProblemDTO[4];

  /**
   * V1 <- V2 <- V3 <- V4 t0 < t1 < t2 < t3
   */
  @BeforeEach
  public void setup() {
    createVaccinations();
    createAllergies();
    createPastIllnesses();
    createMedicalProblems();
  }

  @Test
  void isPreviousCandidate_createdAtSameItem_returnFalse() {
    vaccinationDTOs[1].setCreatedAt(vaccinationDTOs[0].getCreatedAt());
    assertFalse(lifeCycleService.isPreviousCandidate(vaccinationDTOs[0], vaccinationDTOs[1]));
  }

  @Test
  void isPreviousCandidate_differentEntities_returnFalse() {
    vaccinationDTOs[1].setCreatedAt(vaccinationDTOs[0].getCreatedAt());
    assertFalse(lifeCycleService.isPreviousCandidate(vaccinationDTOs[0], vaccinationDTOs[1]));
  }

  @Test
  void isPreviousCandidate_sameItem_returnFalse() {
    assertFalse(lifeCycleService.isPreviousCandidate(vaccinationDTOs[0], vaccinationDTOs[0]));
  }

  @Test
  void previousEntryMissing_entryIsDeleted_dontShowAnything() {
    vaccinationDTOs[1].setDeleted(true);
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs[1]), false);

    assertTrue(managedList.isEmpty());
  }

  /**
   * V1, V2, V3 = update(V2), V4 = deleted(V3) -> V1
   */
  @Test
  void test_chained_deletion() {
    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[3].setRelatedId("3");
    vaccinationDTOs[3].setDeleted(true);
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), false);

    assertThat(managedList.size()).isEqualTo(1);
    assertThat(managedList.getFirst().getId()).isEqualTo("1");
  }

  @Test
  void test_chained_deletion_withoutLifecycle() {
    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[3].setRelatedId("3");
    vaccinationDTOs[3].setDeleted(true);
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), true);

    assertThat(managedList.size()).isEqualTo(4);
    assertThat(managedList.get(3).isUpdated()).isFalse();
    assertThat(managedList.get(3).isDeleted()).isFalse();
    assertThat(managedList.get(2).isUpdated()).isTrue();
    assertThat(managedList.get(2).isDeleted()).isTrue();
    assertThat(managedList.get(1).isUpdated()).isTrue();
    assertThat(managedList.get(1).getRelatedId()).isEqualTo("2");
    assertThat(managedList.get(1).isDeleted()).isTrue();
    assertThat(managedList.getFirst().isUpdated()).isFalse();
    assertThat(managedList.getFirst().getRelatedId()).isEqualTo("3");
    assertThat(managedList.getFirst().isDeleted()).isTrue();
  }

  /**
   * V1, V2, V3 = update(V2), V4 = update(V3) -> V1, V4
   */
  @Test
  void test_chained_update() {
    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[3].setRelatedId("3");
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), false);

    assertThat(managedList.size()).isEqualTo(2);
    assertThat(managedList.get(1).getId()).isEqualTo("1");
    assertThat(managedList.getFirst().getId()).isEqualTo("4");
  }

  @Test
  void test_chained_update_without_lifecycle() {
    vaccinationDTOs[1].setRelatedId("1");
    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[3].setRelatedId("3");
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), true);

    assertThat(managedList.size()).isEqualTo(4);
    assertThat(managedList.get(3).isUpdated()).isTrue();
    assertThat(managedList.get(2).getRelatedId()).isEqualTo("1");
    assertThat(managedList.get(2).isUpdated()).isTrue();
    assertThat(managedList.get(1).getRelatedId()).isEqualTo("2");
    assertThat(managedList.get(1).isUpdated()).isTrue();
    assertThat(managedList.getFirst().getRelatedId()).isEqualTo("3");
    assertThat(managedList.getFirst().isUpdated()).isFalse();
  }

  /**
   * V1<-V2<-V3 = deleted, V4 -> V4
   */
  @Test
  void test_chained_update2() {
    vaccinationDTOs[1].setRelatedId("1");
    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[3].setRelatedId("3");
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), false);

    assertThat(managedList.size()).isEqualTo(1);
    assertThat(managedList.getFirst().getId()).isEqualTo("4");
  }

  /**
   * V1, V2, V3 = deleted(V2), V4 -> V1, V4
   */
  @Test
  void test_deletion() {
    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[2].setDeleted(true);
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), false);

    assertThat(managedList.size()).isEqualTo(2);
    assertThat(managedList.get(1).getId()).isEqualTo("1");
    assertThat(managedList.getFirst().getId()).isEqualTo("4");
  }


  /**
   * V1 <- V2 <- V3 <- delete(V3) -> empty
   */
  @Test
  void test_multiple_deletion() {
    vaccinationDTOs[1].setRelatedId("1");
    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[3].setRelatedId("3");
    vaccinationDTOs[3].setDeleted(true);
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), false);

    assertThat(managedList).isEmpty();
  }

  /**
   * V1, V2, V3 = update(V2), V4 -> V1, V3, V4
   */
  @Test
  void test_update() {
    vaccinationDTOs[2].setRelatedId("2");
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), false);

    assertThat(managedList.size()).isEqualTo(3);
    assertThat(managedList.get(2).getId()).isEqualTo("1");
    assertThat(managedList.get(1).getId()).isEqualTo("3");
    assertThat(managedList.getFirst().getId()).isEqualTo("4");
  }

  @Test
  void test_handle_objectsAreEqualOnlyCommentsDiffer_updatedRecordShouldBeValidated() {
    vaccinationDTOs[1].setValidated(true);

    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[2].setDoseNumber(vaccinationDTOs[1].getDoseNumber());
    vaccinationDTOs[2].setLotNumber(vaccinationDTOs[1].getLotNumber());
    vaccinationDTOs[2].setCode(vaccinationDTOs[1].getCode());
    vaccinationDTOs[2].setRecorder(vaccinationDTOs[1].getRecorder());
    vaccinationDTOs[2].setComment(new CommentDTO(LocalDateTime.now(), "Max Mustermann", "first comment"));

    vaccinationDTOs[3].setRelatedId("3");
    vaccinationDTOs[3].setDoseNumber(vaccinationDTOs[1].getDoseNumber());
    vaccinationDTOs[3].setLotNumber(vaccinationDTOs[1].getLotNumber());
    vaccinationDTOs[3].setCode(vaccinationDTOs[1].getCode());
    vaccinationDTOs[3].setRecorder(vaccinationDTOs[1].getRecorder());
    vaccinationDTOs[3].setComment(new CommentDTO(LocalDateTime.now(), "Max Mustermann", "second comment"));

    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), true);

    assertThat(managedList.size()).isEqualTo(4);
    assertThat(managedList.get(2).getId()).isEqualTo("2");
    assertThat(managedList.get(2).isUpdated()).isEqualTo(true);
    assertThat(managedList.get(1).getRelatedId()).isEqualTo("2");
    assertThat(managedList.get(1).isValidated()).isEqualTo(true);
    assertThat(managedList.get(1).isUpdated()).isEqualTo(true);
    assertThat(managedList.getFirst().getRelatedId()).isEqualTo("3");
    assertThat(managedList.getFirst().isValidated()).isEqualTo(true);
    assertThat(managedList.getFirst().isUpdated()).isEqualTo(false);
  }

  @Test
  void test_handle_objectsAreNotEqual_updatedRecordShouldNotBeValidated() {
    vaccinationDTOs[1].setValidated(true);

    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[2].setComment(new CommentDTO(LocalDateTime.now(), "Max Mustermann", "first comment"));

    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), true);

    assertThat(managedList.size()).isEqualTo(4);
    assertThat(managedList.get(2).getId()).isEqualTo("2");
    assertThat(managedList.get(2).isUpdated()).isEqualTo(true);
    assertThat(managedList.get(1).getRelatedId()).isEqualTo("2");
    assertThat(managedList.get(1).isValidated()).isEqualTo(false);
    assertThat(managedList.get(1).isUpdated()).isEqualTo(false);
  }

  @Test
  void test_handle_authorOfTheUpdatedRecordIsHCP_updatedRecordShouldBeValidated() {
    vaccinationDTOs[1].setValidated(false);

    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[2].setAuthor(new AuthorDTO(new HumanNameDTO("testfirstname", "testlastname",
        "testprefix", LocalDate.now(), "MALE"), "HCP", "gln:1.2.3.4"));

    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), true);

    assertThat(managedList.size()).isEqualTo(4);
    assertThat(managedList.get(2).getId()).isEqualTo("2");
    assertThat(managedList.get(2).isUpdated()).isEqualTo(true);
    assertThat(managedList.get(1).getRelatedId()).isEqualTo("2");
    assertThat(managedList.get(1).isValidated()).isEqualTo(true);
    assertThat(managedList.get(1).isUpdated()).isEqualTo(false);
  }

  @Test
  void test_handle_authorOfTheUpdatedRecordIsASS_updatedRecordShouldBeValidated() {
    vaccinationDTOs[2].setRelatedId("2");
    vaccinationDTOs[2].setAuthor(new AuthorDTO(new HumanNameDTO("testfirstname", "testlastname",
        "testprefix", LocalDate.now(), "FEMALE"), "ASS", "gln:1.2.3.4"));

    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), true);

    assertThat(managedList.size()).isEqualTo(4);
    assertThat(managedList.get(2).getId()).isEqualTo("2");
    assertThat(managedList.get(2).isUpdated()).isEqualTo(true);
    assertThat(managedList.get(1).getRelatedId()).isEqualTo("2");
    assertThat(managedList.get(1).isValidated()).isEqualTo(true);
    assertThat(managedList.get(1).isUpdated()).isEqualTo(false);
  }

  /**
   * V1 <- V2 x V3 <- V4 -> V2, V4
   */
  @Test
  void testBrokenExtrapolation() {
    vaccinationDTOs[1].setCode(vaccinationDTOs[0].getCode());
    vaccinationDTOs[2].setCode(vaccinationDTOs[0].getCode());
    vaccinationDTOs[3].setCode(vaccinationDTOs[0].getCode());

    vaccinationDTOs[1].setOccurrenceDate(LocalDate.now().minusDays(2));
    vaccinationDTOs[1].setRelatedId("1");
    // intermediate is missing
    vaccinationDTOs[2].setRelatedId("20");
    vaccinationDTOs[3].setRelatedId("3");

    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), false);
    assertThat(managedList.size()).isEqualTo(2);
    assertThat(managedList.getFirst().getId()).isEqualTo("4");
    assertThat(managedList.get(1).getId()).isEqualTo("2");
  }

  @Test
  void testExtrapolatedPrevious() {
    vaccinationDTOs[1].setCode(vaccinationDTOs[0].getCode());
    vaccinationDTOs[2].setCode(vaccinationDTOs[0].getCode());
    vaccinationDTOs[3].setCode(vaccinationDTOs[0].getCode());

    vaccinationDTOs[0].setCreatedAt(LocalDateTime.now().minusDays(3));
    vaccinationDTOs[1].setCreatedAt(LocalDateTime.now().minusDays(2));

    Map<String, BaseDTO> items = new HashMap<>();
    items.put(vaccinationDTOs[0].getId(), vaccinationDTOs[0]);
    items.put(vaccinationDTOs[1].getId(), vaccinationDTOs[1]);
    items.put(vaccinationDTOs[2].getId(), vaccinationDTOs[2]);
    items.put(vaccinationDTOs[3].getId(), vaccinationDTOs[3]);
    assertThat(lifeCycleService.extrapolatedPrevious(new HashMap<>(items), vaccinationDTOs[3]).getId()).isEqualTo("3");
    assertThat(lifeCycleService.extrapolatedPrevious(new HashMap<>(items), vaccinationDTOs[2]).getId()).isEqualTo("2");
    assertThat(lifeCycleService.extrapolatedPrevious(new HashMap<>(items), vaccinationDTOs[1]).getId()).isEqualTo("1");
    assertThat(lifeCycleService.extrapolatedPrevious(new HashMap<>(items), vaccinationDTOs[0])).isNull();
  }

  /**
   * V1 <- V2 <- V3 <- x <- V4 -> V4
   */
  @Test
  void testExtrapolation() {
    vaccinationDTOs[1].setCode(vaccinationDTOs[0].getCode());
    vaccinationDTOs[2].setCode(vaccinationDTOs[0].getCode());
    vaccinationDTOs[3].setCode(vaccinationDTOs[0].getCode());
    vaccinationDTOs[1].setRelatedId("1");
    vaccinationDTOs[2].setRelatedId("2");
    // intermediate is missing
    vaccinationDTOs[3].setRelatedId("20");
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), false);
    assertThat(managedList.size()).isEqualTo(1);
    assertThat(managedList.getFirst().getId()).isEqualTo("4");
  }


  @Test
  void testIsPreviousCandidate() {
    assertThat(lifeCycleService.isPreviousCandidate(vaccinationDTOs[0], vaccinationDTOs[1])).isFalse();
    vaccinationDTOs[1].setCode(vaccinationDTOs[0].getCode());
    assertThat(lifeCycleService.isPreviousCandidate(vaccinationDTOs[0], vaccinationDTOs[1])).isTrue();
    vaccinationDTOs[0].setCreatedAt(LocalDateTime.now().minusDays(2));
    assertThat(lifeCycleService.isPreviousCandidate(vaccinationDTOs[0], vaccinationDTOs[1])).isTrue();
    vaccinationDTOs[0].setOccurrenceDate(LocalDate.now().minusDays(2));
    assertThat(lifeCycleService.isPreviousCandidate(vaccinationDTOs[1], vaccinationDTOs[0])).isFalse();

    assertThat(lifeCycleService.isPreviousCandidate(allergieDTOs[0], allergieDTOs[1])).isFalse();
    allergieDTOs[1].setCode(allergieDTOs[0].getCode());
    assertThat(lifeCycleService.isPreviousCandidate(allergieDTOs[0], allergieDTOs[1])).isTrue();
    allergieDTOs[0].setCreatedAt(LocalDateTime.now().minusDays(2));
    assertThat(lifeCycleService.isPreviousCandidate(allergieDTOs[0], allergieDTOs[1])).isTrue();
    allergieDTOs[0].setOccurrenceDate(LocalDate.now().minusDays(2));
    assertThat(lifeCycleService.isPreviousCandidate(allergieDTOs[1], allergieDTOs[0])).isFalse();

    assertThat(lifeCycleService.isPreviousCandidate(problemDTOs[0], problemDTOs[1])).isFalse();
    problemDTOs[1].setCode(problemDTOs[0].getCode());
    assertThat(lifeCycleService.isPreviousCandidate(problemDTOs[0], problemDTOs[1])).isTrue();
    problemDTOs[0].setCreatedAt(LocalDateTime.now().minusDays(2));
    assertThat(lifeCycleService.isPreviousCandidate(problemDTOs[0], problemDTOs[1])).isTrue();
    problemDTOs[0].setRecordedDate(LocalDate.now().minusDays(2));
    assertThat(lifeCycleService.isPreviousCandidate(problemDTOs[1], problemDTOs[0])).isFalse();

    assertThat(lifeCycleService.isPreviousCandidate(pastIllnessDTOs[0], pastIllnessDTOs[1])).isFalse();
    pastIllnessDTOs[1].setCode(pastIllnessDTOs[0].getCode());
    assertThat(lifeCycleService.isPreviousCandidate(pastIllnessDTOs[0], pastIllnessDTOs[1])).isTrue();
    pastIllnessDTOs[0].setCreatedAt(LocalDateTime.now().minusDays(2));
    assertThat(lifeCycleService.isPreviousCandidate(pastIllnessDTOs[0], pastIllnessDTOs[1])).isTrue();
    pastIllnessDTOs[0].setRecordedDate(LocalDate.now().minusDays(2));
    assertThat(lifeCycleService.isPreviousCandidate(pastIllnessDTOs[1], pastIllnessDTOs[0])).isFalse();
  }

  @Test
  void testKeepOnePrevious() {
    vaccinationDTOs[1].setRelatedId("1");
    vaccinationDTOs[2].setRelatedId("1");

    Map<String, BaseDTO> items = new ConcurrentHashMap<>();
    items.put(vaccinationDTOs[0].getId(), vaccinationDTOs[0]);
    items.put(vaccinationDTOs[1].getId(), vaccinationDTOs[1]);
    items.put(vaccinationDTOs[2].getId(), vaccinationDTOs[2]);
    items.put(vaccinationDTOs[3].getId(), vaccinationDTOs[3]);

    Map<String, BaseDTO> onePrevious = lifeCycleService.keepOnePrevious(items);

    assertThat(onePrevious.size()).isEqualTo(3);
    assertThat(onePrevious.get("4")).isNotNull();
    assertThat(onePrevious.get("3")).isNotNull();
    assertThat(onePrevious.get("2")).isNull();
    assertThat(onePrevious.get("1")).isNotNull();
  }

  /**
   * V1, V2, V3, V4 -> V1, V2, V3, V4
   */
  @Test
  void testNothingRelated() {
    List<VaccinationDTO> managedList =
        (List<VaccinationDTO>) lifeCycleService.handle(Arrays.asList(vaccinationDTOs), false);
    assertThat(managedList.size()).isEqualTo(4);
    assertThat(managedList.getFirst().getId()).isEqualTo("4");
    assertThat(managedList.get(1).getId()).isEqualTo("3");
    assertThat(managedList.get(2).getId()).isEqualTo("2");
    assertThat(managedList.get(3).getId()).isEqualTo("1");
  }

  private void createAllergies() {
    for (int i = 1; i <= 4; i++) {
      String counter = String.valueOf(i);
      allergieDTOs[i - 1] = new AllergyDTO(counter, LocalDate.now(), new ValueDTO(counter, counter, "testsystem"), null,
          null,
          null, null, new HumanNameDTO("Victor" + counter, "Frankenstein" + counter, "Dr.", null, null), null,
          null);
      allergieDTOs[i - 1].setCreatedAt(LocalDateTime.now().plusSeconds(i));
    }
  }

  private void createMedicalProblems() {
    for (int i = 1; i <= 4; i++) {
      String counter = String.valueOf(i);
      problemDTOs[i - 1] = new MedicalProblemDTO(counter, new ValueDTO(counter, counter, "testsystem"), null,
          null, LocalDate.now(), LocalDate.now().minusDays(3), null,
          new HumanNameDTO("Victor" + counter, "Frankenstein" + counter, "Dr.", null, null), null, null);
      problemDTOs[i - 1].setCreatedAt(LocalDateTime.now().plusSeconds(i));
    }
  }

  private void createPastIllnesses() {
    for (int i = 1; i <= 4; i++) {
      String counter = String.valueOf(i);
      pastIllnessDTOs[i - 1] = new PastIllnessDTO(counter, new ValueDTO(counter, counter, "testsystem"), null,
          null, LocalDate.now(), LocalDate.now().minusDays(10), null,
          new HumanNameDTO("Victor" + counter, "Frankenstein" + counter, "Dr.", null, null), null, null);
      pastIllnessDTOs[i - 1].setCreatedAt(LocalDateTime.now().plusSeconds(i));
    }
  }

  private void createVaccinations() {
    ValueDTO status = new ValueDTO("completed", "completed", "testsystem");
    ValueDTO verificationStatus = new ValueDTO("59156000", "Confirmed", "http://snomed.info/sct");
    ValueDTO targetDisease = new ValueDTO("38907003", "Varicella", "http://snomed.info/sct");
    for (int i = 1; i <= 4; i++) {
      String counter = String.valueOf(i);
      vaccinationDTOs[i - 1] = new VaccinationDTO(counter, new ValueDTO(counter, counter, "testsystem"),
          List.of(targetDisease), null, i, LocalDate.now(),
          new HumanNameDTO("Victor" + counter, "Frankenstein" + counter, "Dr.",
          null, null), null, "lotNumber" + counter, null, status, verificationStatus);
      vaccinationDTOs[i - 1].setAuthor(new AuthorDTO(new HumanNameDTO("testfirstname", "testlastname",
          "testprefix", LocalDate.now(), "FEMALE"), "PAT", "gln:1.2.3.4"));
      vaccinationDTOs[i - 1].setCreatedAt(LocalDateTime.now().plusSeconds(i));
    }
  }
}
