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

import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
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

  /**
   * V1 <- V2 <- V3 <- V4 t0 < t1 < t2 < t3
   */
  @BeforeEach
  public void setup() {
    ValueDTO status = new ValueDTO("completed", "completed", "testsystem");
    vaccinationDTOs[0] = new VaccinationDTO("1", new ValueDTO("1", "1", "testsystem"), null, null, 1,
        LocalDate.now(), new HumanNameDTO("Victor1", "Frankenstein1", "Dr.", null, null), null,
        "lotNumber1", null, status);
    vaccinationDTOs[1] = new VaccinationDTO("2", new ValueDTO("2", "2", "testsystem"), null, null, 2,
        LocalDate.now(), new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null), null,
        "lotNumber2", null, status);
    vaccinationDTOs[2] = new VaccinationDTO("3", new ValueDTO("3", "3", "testsystem"), null, null, 3,
        LocalDate.now(), new HumanNameDTO("Victor3", "Frankenstein3", "Dr.", null, null), null,
        "lotNumber3", null, status);
    vaccinationDTOs[3] = new VaccinationDTO("4", new ValueDTO("4", "4", "testsystem"), null, null, 4,
        LocalDate.now(), new HumanNameDTO("Victor4", "Frankenstein4", "Dr.", null, null), null,
        "lotNumber4", null, status);

    vaccinationDTOs[0].setCreatedAt(LocalDateTime.now());
    vaccinationDTOs[1].setCreatedAt(LocalDateTime.now().plusSeconds(1));
    vaccinationDTOs[2].setCreatedAt(LocalDateTime.now().plusSeconds(2));
    vaccinationDTOs[3].setCreatedAt(LocalDateTime.now().plusSeconds(3));
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
    assertThat(managedList.get(0).getId()).isEqualTo("1");
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
    assertThat(managedList.get(0).isUpdated()).isFalse();
    assertThat(managedList.get(0).getRelatedId()).isEqualTo("3");
    assertThat(managedList.get(0).isDeleted()).isTrue();
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
    assertThat(managedList.get(0).getId()).isEqualTo("4");
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
    assertThat(managedList.get(0).getRelatedId()).isEqualTo("3");
    assertThat(managedList.get(0).isUpdated()).isFalse();
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
    assertThat(managedList.get(0).getId()).isEqualTo("4");
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
    assertThat(managedList.get(0).getId()).isEqualTo("4");
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
    assertThat(managedList.get(0).getId()).isEqualTo("4");
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
    assertThat(managedList.get(0).getId()).isEqualTo("4");
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
    assertThat(managedList.get(0).getId()).isEqualTo("4");
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
    assertThat(managedList.get(0).getId()).isEqualTo("4");
    assertThat(managedList.get(1).getId()).isEqualTo("3");
    assertThat(managedList.get(2).getId()).isEqualTo("2");
    assertThat(managedList.get(3).getId()).isEqualTo("1");
  }
}
