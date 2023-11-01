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
package ch.admin.bag.vaccination.controller;

import ch.admin.bag.vaccination.data.dto.VaccineToTargetDiseasesDTO;
import ch.admin.bag.vaccination.data.dto.ValueListDTO;
import ch.admin.bag.vaccination.service.ValueListService;
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/utility")
@Tag(name = "UtilityController", description = "Controller with utility functionalities")
@Slf4j
public class UtilityController {
  @Autowired
  private ValueListService valueListService;

  @Autowired
  private Cache cache;

  @GetMapping("/clearCache")
  public String clearCache() {
    log.info("Clearing caches");
    cache.clear();

    return "Cache was successfully cleared.";
  }

  @GetMapping("/getAllValuesLists")
  public List<ValueListDTO> getAllValuesList() {
    log.info("getAllValuesLists");
    return valueListService.getAllListOfValues();
  }

  @GetMapping("/targetDiseases")
  public Collection<ValueDTO> getTargetDiseases() {
    log.info("getTargetDiseases");
    return valueListService.getTargetDiseases();
  }

  @GetMapping("/vaccinesToTargetDiseases")
  public List<VaccineToTargetDiseasesDTO> getVaccinesToTargetDiseases() {
    log.info("getVaccinesToTargetDiseases");
    return valueListService.getVaccinesToTargetDiseases();
  }
}
