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
import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.fhir.epr.adapter.FhirAdapter;
import ch.fhir.epr.adapter.FhirUtils;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class VaccinationRecordServiceTest {

  @Autowired
  private VaccinationRecordService vaccinationRecordService;
  @Autowired
  private AllergyService allergyService;
  @Autowired
  private VaccinationService vaccinationService;
  @Autowired
  private PastIllnessService pastIllnessService;
  @Autowired
  private MedicalProblemService medicalProblemService;
  @Autowired
  private FhirAdapter fhirAdapter;
  @Autowired
  private ProfileConfig profileConfig;
  AuthorDTO author = new AuthorDTO(new HumanNameDTO("hor", "Aut", "Dr.", null, null), "HCP", "gln:1.2.3.4");

  @BeforeEach
  void before() {
    profileConfig.setLocalMode(true);
    profileConfig.setHuskyLocalMode(null);
  }

  @Test
  void saveToEPD_validEntries_noExceptions() {
    List<VaccinationDTO> vaccinations = vaccinationService.getAll("dummy", "dummy", "dummy", author, null, true);
    List<AllergyDTO> allergies = allergyService.getAll("dummy", "dummy", "dummy", author, null, true);
    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll("dummy", "dummy", "dummy", author, null, true);
    List<MedicalProblemDTO> medicalProblems =
        medicalProblemService.getAll("dummy", "dummy", "dummy", author, null, true);

    VaccinationRecordDTO record =
        new VaccinationRecordDTO(createAuthor(), null, allergies, pastIllnesses, vaccinations, medicalProblems);
    String json = vaccinationRecordService.create("EPDPLAYGROUND", "dummy", "dummy", record, null);
    log.error("created record + json {}", json);

    Bundle parsedBundle = fhirAdapter.unmarshallFromString(json);
    assertThat(parsedBundle.getMeta().getProfile().get(0).getValue()).isEqualTo(FhirUtils.VACCINATION_RECORD_TYPE_URL);
    parsedBundle.getMeta().getProfile().get(0).setValue(FhirUtils.VACCINATION_TYPE_URL);
    List<VaccinationDTO> vaccinationsResult = fhirAdapter.getDTOs(VaccinationDTO.class, parsedBundle);
    List<AllergyDTO> allergiesResult = fhirAdapter.getDTOs(AllergyDTO.class, parsedBundle);
    List<PastIllnessDTO> pastIllnessesResult = fhirAdapter.getDTOs(PastIllnessDTO.class, parsedBundle);

    assertThat(vaccinationsResult.size()).isEqualTo(vaccinations.size());
    assertThat(allergiesResult.size()).isEqualTo(allergies.size());
    assertThat(pastIllnessesResult.size()).isEqualTo(pastIllnesses.size());
  }

  private AuthorDTO createAuthor() {
    return new AuthorDTO(new HumanNameDTO("Test Firstname", "Test Lastname", "Test Prefix", LocalDate.now(), "MALE"));
  }
}
