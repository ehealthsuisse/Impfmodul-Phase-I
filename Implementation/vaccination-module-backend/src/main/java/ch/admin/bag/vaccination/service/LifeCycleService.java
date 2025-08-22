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

import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.utils.ValidationUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 *
 * Manage the lifeCyle of a DTO
 * <ul>
 * <li>I1, I2 -> I1, I2</li>
 * <li>I1, I2=update(I1) -> I2</li>
 * <li>I1, I2=delete(I1) -> Nothing</li>
 * <li>I1, I2=update(I1), I3=update(I2)-> I3</li>
 * <li>I1, I2=update(I1), I3=delete(I2)-> Nothing</li>
 * </ul>
 *
 */
@Service
@Slf4j
public class LifeCycleService {

  /**
   * Handle a list of items
   *
   * @param items list of items {@link VaccinationDTO}, {@link AllergyDTO}, {@link PastIllnessDTO}
   * @param keepModifiedEntries if true, modified entries remain in the list.
   *
   * @return the managed list of items
   */
  public List<? extends BaseDTO> handle(List<? extends BaseDTO> items, boolean keepModifiedEntries) {
    Map<String, BaseDTO> map = new ConcurrentHashMap<>();
    for (BaseDTO item : items) {
      map.put(item.getId(), item);
    }

    if (!keepModifiedEntries) {
      map = keepOnePrevious(map);
    }

    List<BaseDTO> sortedByCreatedAt = map.values().stream()
        .sorted(Comparator.comparing(BaseDTO::getCreatedAt))
        .toList();

    for (BaseDTO entry : sortedByCreatedAt) {
      BaseDTO previous = previous(map, entry);
      if (previous != null) {
        boolean isDeleted = checkDeletedFlag(map, entry, keepModifiedEntries);
        entry.setValidated(ValidationUtils.shouldRecordBeValidated(previous, entry));
        while (previous != null) {
          previous.setDeleted(isDeleted);
          previous.setUpdated(true);
          if (!keepModifiedEntries) {
            remove(map, previous);
          }
          previous = previous(map, previous);
        }
      } else if (entry.isDeleted() && !keepModifiedEntries) {
        remove(map, entry);
      }
    }

    List<BaseDTO> result = new ArrayList<>(map.values());
    result.sort(Comparator.comparing(BaseDTO::getCreatedAt).reversed());
    return result;
  }

  protected BaseDTO extrapolatedPrevious(Map<String, BaseDTO> items, BaseDTO entry) {
    BaseDTO candidate = null;
    for (BaseDTO dto : items.values()) {
      if (isPreviousCandidate(dto, entry)) {
        if (candidate == null || candidate.getCreatedAt().isBefore(dto.getCreatedAt())) {
          candidate = dto;
        }
      }
    }
    return candidate;
  }

  protected boolean isPreviousCandidate(BaseDTO candidate, BaseDTO entry) {
    log.trace("isCandidate {} {}", candidate, entry);
    boolean isSameEntry = candidate.getId().equals(entry.getId());
    boolean createdAtSameTimeOrLater = candidate.getCreatedAt().compareTo(entry.getCreatedAt()) >= 1;
    boolean hasNotSameCode = !candidate.getCode().equals(entry.getCode());
    boolean notSameDayOfEvent = !candidate.getDateOfEvent().equals(entry.getDateOfEvent());

    return !(isSameEntry || createdAtSameTimeOrLater || hasNotSameCode || notSameDayOfEvent);
  }

  /**
   * <p>
   * I0 is previous entry of I1 and I2 and I2 was created after I1. This use case might occure if I1
   * is restricted or secret.
   * <p>
   * I0 <--- I2.
   *
   * @param items map of items
   */
  protected Map<String, BaseDTO> keepOnePrevious(Map<String, BaseDTO> items) {
    // Add items without previous
    Map<String, BaseDTO> mapWithAtMostOnePreviousEntry = new ConcurrentHashMap<>();
    for (BaseDTO item : items.values()) {
      if (item.getRelatedId() == null) {
        mapWithAtMostOnePreviousEntry.put(item.getId(), item);
        items.remove(item.getId());
      }
    }

    for (BaseDTO item1 : items.values()) {
      BaseDTO toKeep = item1;
      for (BaseDTO item2 : items.values()) {
        if (!item1.getId().equals(item2.getId()) &&
            item1.getRelatedId().equals(item2.getRelatedId())) {
          if (item2.getCreatedAt().isAfter(toKeep.getCreatedAt())) {
            toKeep = item2;
          }
        }
      }
      mapWithAtMostOnePreviousEntry.put(toKeep.getId(), toKeep);
    }

    return mapWithAtMostOnePreviousEntry;
  }


  private boolean checkDeletedFlag(Map<String, BaseDTO> map, BaseDTO current,
      boolean keepModifiedEntries) {
    boolean isDeleted = current.isDeleted();
    if (!keepModifiedEntries && isDeleted) {
      remove(map, current);
    }

    return isDeleted;
  }

  private BaseDTO previous(Map<String, BaseDTO> map, BaseDTO item) {
    log.debug("  previous {}", item);
    if (item == null) {
      return null;
    }
    BaseDTO previous = null;
    if (item.getRelatedId() != null) {
      previous = map.get(item.getRelatedId());
      if (previous == null) {
        previous = extrapolatedPrevious(map, item);
      }
    }

    return previous;
  }

  private void remove(Map<String, BaseDTO> map, BaseDTO item) {
    log.debug("  remove {}", item);
    if (item == null) {
      return;
    }

    BaseDTO currentOrPrevious = item;
    while (currentOrPrevious != null) {
      map.remove(item.getId());
      currentOrPrevious = previous(map, currentOrPrevious);
    }
  }
}
