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

import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class PriorityComparatorTest {

  @Test
  void compare_sortByPriority_highestValueFirst() {
    ValueDTO value1 = new ValueDTO("265940000", "Animal health occupation (occupation)", "http://snomed.info/sct");
    ValueDTO value2 = new ValueDTO("1237576009", "Birth weight 1.5 kilogram or less (finding)",
        "http://snomed.info/sct");
    ValueDTO value3 = new ValueDTO("42261000195101",
        "Disorder of musculoskeletal system with systemic manifestations (disorder)", "http://snomed.info/sct");
    ValueDTO value4 = new ValueDTO("223366009", "Healthcare professional (occupation)", "http://snomed.info/sct");
    ValueDTO value5 = new ValueDTO("1251534000", "Exposure to invasive meningococcal disease (event)",
        "http://snomed.info/sct");
    ValueDTO value6 = new ValueDTO("1237443008", "Birth weight 1.5 kilogram or less (finding)",
        "http://snomed.info/sct");

    PriorityValue pv1 = new PriorityValue(0, value1);
    PriorityValue pv2 = new PriorityValue(4, value2);
    PriorityValue pv3 = new PriorityValue(3, value3);
    PriorityValue pv4 = new PriorityValue(5, value4);
    PriorityValue pv5 = new PriorityValue(6, value5);
    PriorityValue pv6 = new PriorityValue(1, value6);

    List<PriorityValue> sorted = Stream.of(pv1, pv2, pv3, pv4, pv5, pv6).sorted(new PriorityComparator()).toList();

    assertEquals(6, sorted.getFirst().getPriority());
    assertEquals(3, sorted.get(3).getPriority());
    assertEquals(0, sorted.get(5).getPriority());
  }

  @Test
  void compare_sortByPriorityAndAlphabetic_highestValueFirst_ifSamePrioritySortAlphabeticallyAFirst() {
    ValueDTO value1 = new ValueDTO("265940000", "Animal health occupation (occupation)", "http://snomed.info/sct");
    ValueDTO value2 = new ValueDTO("1237030002", "At increased risk of exposure to Bordetella pertussis (finding)",
        "http://snomed.info/sct");
    ValueDTO value3 = new ValueDTO("1237028004",
        "At increased risk of exposure to Influenza virus (finding)", "http://snomed.info/sct");
    ValueDTO value4 = new ValueDTO("223366009", "Healthcare professional (occupation)", "http://snomed.info/sct");
    ValueDTO value5 = new ValueDTO("1251534000", "Exposure to invasive meningococcal disease (event)",
        "http://snomed.info/sct");
    ValueDTO value6 = new ValueDTO("1237443008", "Birth weight 1.5 kilogram or less (finding)",
        "http://snomed.info/sct");

    PriorityValue pv1 = new PriorityValue(0, value1);
    PriorityValue pv2 = new PriorityValue(0, value2);
    PriorityValue pv3 = new PriorityValue(0, value3);
    PriorityValue pv4 = new PriorityValue(1, value4);
    PriorityValue pv5 = new PriorityValue(3, value5);
    PriorityValue pv6 = new PriorityValue(1, value6);

    List<PriorityValue> sorted = Stream.of(pv1, pv2, pv3, pv4, pv5, pv6).sorted(new PriorityComparator()).toList();

    assertEquals(3, sorted.getFirst().getPriority());
    assertEquals(1, sorted.get(1).getPriority());
    assertEquals("Birth weight 1.5 kilogram or less (finding)", sorted.get(1).getDto().getName());
    assertEquals(1, sorted.get(2).getPriority());
    assertEquals("Healthcare professional (occupation)", sorted.get(2).getDto().getName());
    assertEquals(0, sorted.get(3).getPriority());
    assertEquals("Animal health occupation (occupation)", sorted.get(3).getDto().getName());
    assertEquals(0, sorted.get(3).getPriority());
    assertEquals("At increased risk of exposure to Bordetella pertussis (finding)", sorted.get(4).getDto().getName());
    assertEquals(0, sorted.get(3).getPriority());
    assertEquals("At increased risk of exposure to Influenza virus (finding)", sorted.get(5).getDto().getName());
  }
}
