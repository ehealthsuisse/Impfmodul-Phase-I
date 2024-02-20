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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.fhir.epr.adapter.FhirAdapter;
import ch.fhir.epr.adapter.FhirConstants;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class VaccinationRecordServiceTest {

  private static final String EPDPLAYGROUND = "EPDPLAYGROUND";

  @Autowired
  private AllergyService allergyService;
  @Autowired
  private FhirAdapter fhirAdapter;
  @Autowired
  private MedicalProblemService medicalProblemService;
  @Autowired
  private PastIllnessService pastIllnessService;
  @Autowired
  private ProfileConfig profileConfig;
  @Autowired
  private VaccinationRecordService vaccinationRecordService;
  @Autowired
  private VaccinationService vaccinationService;

  @BeforeEach
  void before() {
    profileConfig.setLocalMode(true);
    profileConfig.setHuskyLocalMode(null);
    setAuthorInSession(
        new AuthorDTO(new HumanNameDTO("Test Firstname", "Test Lastname", "Test Prefix", LocalDate.now(), "MALE")));
  }

  @Test
  void getAll_patientNotLinkedToTheSession_shouldReturnPatientInfoNullIfNotInLocalMode() {
    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "dummy", "dummy"));
    PatientIdentifier patientIdentifier =
        new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "test", "test");
    patientIdentifier.setPatientInfo(new HumanNameDTO("Firstname", "Lastname", "Prefix", LocalDate.now(), "FEMALE"));

    VaccinationRecordDTO result = vaccinationRecordService.create(patientIdentifier, null);

    // localMode is activated by before()
    assertNotNull(result.getPatient());

    // remove local mode and check one more time
    profileConfig.setLocalMode(false);
    result = vaccinationRecordService.create(patientIdentifier, null);
    assertNull(result.getPatient());
  }

  @Test
  void saveToEPD_validEntries_noExceptions() {
    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "dummy", "dummy"));
    List<VaccinationDTO> vaccinations = vaccinationService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<AllergyDTO> allergies = allergyService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<MedicalProblemDTO> medicalProblems =
        medicalProblemService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);

    VaccinationRecordDTO record =
        new VaccinationRecordDTO(null, null, allergies, pastIllnesses, vaccinations, medicalProblems);
    String json = vaccinationRecordService.create(EPDPLAYGROUND, "dummy", "dummy", record, null);
    log.error("created record + json {}", json);

    Bundle parsedBundle = fhirAdapter.unmarshallFromString(json);
    assertThat(parsedBundle.getMeta().getProfile().get(0).getValue())
        .isEqualTo(FhirConstants.META_VACCINATION_RECORD_TYPE_URL);
    parsedBundle.getMeta().getProfile().get(0).setValue(FhirConstants.META_VACCINATION_TYPE_URL);
    List<VaccinationDTO> vaccinationsResult = fhirAdapter.getDTOs(VaccinationDTO.class, parsedBundle);
    List<AllergyDTO> allergiesResult = fhirAdapter.getDTOs(AllergyDTO.class, parsedBundle);
    List<PastIllnessDTO> pastIllnessesResult = fhirAdapter.getDTOs(PastIllnessDTO.class, parsedBundle);

    assertThat(vaccinationsResult.size()).isEqualTo(vaccinations.size());
    assertThat(allergiesResult.size()).isEqualTo(allergies.size());
    assertThat(pastIllnessesResult.size()).isEqualTo(pastIllnesses.size());
  }


  private void setAuthorInSession(AuthorDTO author) {
    Objects.nonNull(author);
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    HttpSessionUtils.setParameterInSession(request, HttpSessionUtils.AUTHOR, author);
  }

  private void setPatientIdentifierInSession(PatientIdentifier identifier) {
    Objects.nonNull(identifier);
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    HttpSessionUtils.setParameterInSession(request, HttpSessionUtils.CACHE_PATIENT_IDENTIFIER, identifier);
  }
}
