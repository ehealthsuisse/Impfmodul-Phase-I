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

import ch.admin.bag.vaccination.data.dto.VaccineToTargetDiseasesDTO;
import ch.admin.bag.vaccination.data.dto.ValueListDTO;
import ch.admin.bag.vaccination.utils.PriorityComparator;
import ch.admin.bag.vaccination.utils.PriorityValue;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ValueListService {
  public static final String SWISSMEDIC_CS_SYSTEM_URL = "http://fhir.ch/ig/ch-vacd/CodeSystem/ch-vacd-swissmedic-cs";
  public static final String MYVACCINES_CS_SYSTEM_URL = "http://fhir.ch/ig/ch-vacd/CodeSystem/ch-vacd-myvaccines-cs";
  public static final String IMMUNIZATION_VACCINE_CODE_VALUELIST = "immunizationVaccineCode";
  public static final String CONDITION_CLINICAL_STATUS = "conditionClinicalStatus";
  public static final String UNKNOWN_VACCINE_CODE = "787859002";

  @Autowired
  private VaccinesToTargetDiseasesConfig vaccinesToTargetDiseasesConfig;

  @Value("${application.valueListPath}")
  String folderPathValueListProperties;

  public List<ValueListDTO> getAllListOfValues() {
    log.info("Read valuelists from folder {}", folderPathValueListProperties);
    List<ValueListDTO> codeSystemValueList = new ArrayList<>();
    try (DirectoryStream<Path> streamFiles =
        Files.newDirectoryStream(Path.of(folderPathValueListProperties))) {
      streamFiles.forEach(propertyFile -> {
        if (propertyFile.toString().endsWith("properties")) {
          ValueListDTO createdValueList = createValueList(propertyFile);
          modifyVisibilities(createdValueList);
          codeSystemValueList.add(createdValueList);
        }
      });
    } catch (IOException ex) {
      log.error("Could not getvalues from file: {}", ex.getMessage());
    }

    return codeSystemValueList;
  }


  public Collection<ValueDTO> getTargetDiseases() {
    Map<String, ValueDTO> values = new HashMap<>();
    for (VaccineToTargetDiseases v2td : vaccinesToTargetDiseasesConfig.getVaccines()) {
      for (ValueDTO targetDisease : v2td.getTarget()) {
        v2td.setTargetSystem(vaccinesToTargetDiseasesConfig.getTargetDiseaseSystem());
        values.putIfAbsent(targetDisease.getCode(), targetDisease);
      }
    }
    return values.values();
  }

  public List<VaccineToTargetDiseasesDTO> getVaccinesToTargetDiseases() {
    List<VaccineToTargetDiseasesDTO> dtos = new ArrayList<>();
    for (VaccineToTargetDiseases v2td : vaccinesToTargetDiseasesConfig.getVaccines()) {
      v2td.setTargetSystem(vaccinesToTargetDiseasesConfig.getTargetDiseaseSystem());
      dtos.add(new VaccineToTargetDiseasesDTO(
          v2td.getVaccine(
              isCodeSmaller200(v2td.getCode()) ? vaccinesToTargetDiseasesConfig.getVaccineSystemBelowCode200()
                  : vaccinesToTargetDiseasesConfig.getTargetDiseaseSystem()),
          v2td.getTarget()));
    }
    return dtos;
  }

  private PriorityValue createCodeSystemDTOs(String valueLine) {
    String[] codeValuePair = valueLine.split(";");
    int priority = 0;
    String code = codeValuePair[0];
    String value = codeValuePair[1].trim();
    String system = codeValuePair[2].trim();

    if (codeValuePair.length > 3) {
      priority = Integer.parseInt(codeValuePair[3]);
    }

    return new PriorityValue(priority, new ValueDTO(code, value, system));
  }


  private ValueListDTO createValueList(Path file) {
    try {
      List<String> fileContentLines = Files.readAllLines(file, StandardCharsets.UTF_8);
      String name = file.getFileName().toString().replace(".properties", "");
      List<ValueDTO> listCodeSystemDTO = fileContentLines.stream()
          .map(this::createCodeSystemDTOs)
          .sorted(new PriorityComparator())
          .map(PriorityValue::getDto)
          .toList();

      return new ValueListDTO(name, listCodeSystemDTO);
    } catch (IOException ex) {
      log.error("Could not read the file {}: {}", file.getFileName().toString(), ex.getMessage());
      return null;
    }
  }

  private boolean isCodeSmaller200(String code) {
    try {
      return Integer.parseInt(code) <= 200;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private void modifyVisibilities(ValueListDTO createdValueList) {
    if (IMMUNIZATION_VACCINE_CODE_VALUELIST.equals(createdValueList.getName())) {
      createdValueList.getEntries().stream()
          .filter(entry -> {
            boolean isSuisseMedicVaccination = SWISSMEDIC_CS_SYSTEM_URL.equals(entry.getSystem());
            boolean isMyVaccinesVaccination = MYVACCINES_CS_SYSTEM_URL.equals(entry.getSystem());
            boolean isUnknownVaccine = UNKNOWN_VACCINE_CODE.equals(entry.getCode());
            return !(isSuisseMedicVaccination || isMyVaccinesVaccination ||isUnknownVaccine);
          })
          .forEach(entry -> entry.setAllowDisplay(false));
    } else if (CONDITION_CLINICAL_STATUS.equals(createdValueList.getName())) {
      // allow only active and inactive
      createdValueList.getEntries().stream()
          .filter(entry -> !entry.getCode().toLowerCase().contains("active"))
          .forEach(entry -> entry.setAllowDisplay(false));
    }
  }
}
