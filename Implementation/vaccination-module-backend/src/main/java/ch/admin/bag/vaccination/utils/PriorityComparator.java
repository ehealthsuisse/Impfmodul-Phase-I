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

import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.util.Comparator;

/**
 * Sorts {@link ValueDTO} by using the priority field attached with the help of the wrapper class {@link PriorityValue}
 * In the case that priority is the same for multiple ValueDTOs, objects will nbe sorted alphabetically by name
 */
public class PriorityComparator implements Comparator<PriorityValue> {

  @Override
  public int compare(PriorityValue o1, PriorityValue o2) {
    int priorityComparison = Integer.compare(o2.getPriority(), o1.getPriority());

    if (priorityComparison != 0) {
      return priorityComparison;
    }

    return o1.getDto().getName().compareTo(o2.getDto().getName());
  }
}
