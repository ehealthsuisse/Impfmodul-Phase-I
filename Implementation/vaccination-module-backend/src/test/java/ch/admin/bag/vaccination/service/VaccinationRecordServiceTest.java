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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.data.request.EPRDocument;
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.admin.bag.vaccination.service.cache.CacheIdentifierKey;
import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.fhir.epr.adapter.FhirAdapter;
import ch.fhir.epr.adapter.FhirConstants;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
  @MockitoBean
  private BaseService<BaseDTO> baseService;
  @MockitoBean
  private Cache cache;

  @BeforeEach
  void before() {
    profileConfig.setLocalMode(true);
    profileConfig.setHuskyLocalMode(null);
    setAuthorInSession(
        new AuthorDTO(new HumanNameDTO("Test Firstname", "Test Lastname", "Test Prefix", LocalDate.now(), "MALE")));
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
    assertThat(parsedBundle.getMeta().getProfile().getFirst().getValue())
        .isEqualTo(FhirConstants.META_VACCINATION_RECORD_TYPE_URL);
    parsedBundle.getMeta().getProfile().getFirst().setValue(FhirConstants.META_VACCINATION_TYPE_URL);
    List<VaccinationDTO> vaccinationsResult = fhirAdapter.getDTOs(VaccinationDTO.class, parsedBundle);
    List<AllergyDTO> allergiesResult = fhirAdapter.getDTOs(AllergyDTO.class, parsedBundle);
    List<PastIllnessDTO> pastIllnessesResult = fhirAdapter.getDTOs(PastIllnessDTO.class, parsedBundle);

    assertThat(vaccinationsResult.size()).isEqualTo(vaccinations.size());
    assertThat(allergiesResult.size()).isEqualTo(allergies.size());
    assertThat(pastIllnessesResult.size()).isEqualTo(pastIllnesses.size());
  }

  @Test
  void getDocumentsFromEPD_immunizationDocumentsAreFound_noVaccRecordCreationDateShouldBePresent() {
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setFirstName("John");
    humanNameDTO.setLastName("Doe");
    PatientIdentifier patientIdentifier = new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "dummy", "dummy");
    patientIdentifier.setPatientInfo(humanNameDTO);

    List<VaccinationDTO> vaccinations = vaccinationService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<AllergyDTO> allergies = allergyService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<MedicalProblemDTO> medicalProblems =
        medicalProblemService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);

    List<BaseDTO> baseDTOS = new ArrayList<>();
    baseDTOS.addAll(vaccinations);
    baseDTOS.addAll(allergies);
    baseDTOS.addAll(pastIllnesses);
    baseDTOS.addAll(medicalProblems);
    when(baseService.getAll(patientIdentifier, null, true)).thenReturn(baseDTOS);

    when(baseService.getAll(any(PatientIdentifier.class), any(Assertion.class), anyBoolean())).thenReturn(baseDTOS);

    VaccinationRecordDTO vaccinationRecordDTO = vaccinationRecordService.create(patientIdentifier, null);

    assertNull(vaccinationRecordDTO.getCreatedAt());
  }

  @Test
  void getDocumentsFromEPD_immunizationDocumentsNotFound_vaccRecordRetrievedFromEPD_createdVaccRecordShouldHaveACreationDate() {
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setFirstName("John");
    humanNameDTO.setLastName("Doe");
    PatientIdentifier patientIdentifier = new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "dummy", "dummy");
    patientIdentifier.setPatientInfo(humanNameDTO);
    CacheIdentifierKey cacheIdentifierKey = new CacheIdentifierKey(patientIdentifier, HttpSessionUtils.getAuthorFromSession());

    when(baseService.getAll(any(PatientIdentifier.class), any(Assertion.class), anyBoolean())).thenReturn(Collections.emptyList());
    when(cache.createCacheIdentifier(patientIdentifier)).thenReturn(cacheIdentifierKey);
    when(cache.getData(cacheIdentifierKey, Cache.VACCINATION_RECORDS_CACHE_NAME)).thenReturn(List.of(new EPRDocument(false, null, null,
        LocalDateTime.now())));

    VaccinationRecordDTO vaccinationRecordDTO = vaccinationRecordService.create(patientIdentifier, null);

    assertNotNull(vaccinationRecordDTO.getCreatedAt());
  }

  @Test
  void convertVaccinationRecord_vaccRecordRetrievedFromEPD_forceImmunizationFlagIsTrueWhenWritingDocBackToEPD() {
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setFirstName("John");
    humanNameDTO.setLastName("Doe");
    PatientIdentifier patientIdentifier = new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "dummy", "dummy");
    patientIdentifier.setPatientInfo(humanNameDTO);

    List<VaccinationDTO> vaccinations = vaccinationService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<AllergyDTO> allergies = allergyService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<MedicalProblemDTO> medicalProblems =
        medicalProblemService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);

    VaccinationRecordDTO record =
        new VaccinationRecordDTO(null, null, allergies, pastIllnesses, vaccinations, medicalProblems);
    String json = vaccinationRecordService.create(EPDPLAYGROUND, "dummy", "dummy", record, null);
    EPRDocument eprDocument = new EPRDocument(false, json, new RetrievedDocument(), LocalDateTime.now());

    CacheIdentifierKey cacheIdentifierKey = new CacheIdentifierKey(patientIdentifier, HttpSessionUtils.getAuthorFromSession());
    when(cache.createCacheIdentifier(patientIdentifier)).thenReturn(cacheIdentifierKey);
    when(cache.getData(cacheIdentifierKey, Cache.VACCINATION_RECORDS_CACHE_NAME)).thenReturn(List.of(eprDocument));

    List<BaseDTO> baseDTOS = new ArrayList<>();
    baseDTOS.addAll(vaccinations);
    baseDTOS.addAll(allergies);
    baseDTOS.addAll(pastIllnesses);
    baseDTOS.addAll(medicalProblems);
    when(baseService.parseBundle(any(PatientIdentifier.class), any(Bundle.class))).thenReturn(baseDTOS);

    VaccinationRecordDTO vaccinationRecordDTO =
        vaccinationRecordService.convertVaccinationToImmunization(patientIdentifier, null);

    assertNotNull(vaccinationRecordDTO);
    verify(baseService).create(eq(EPDPLAYGROUND), eq("dummy"), eq("dummy"),
        any(VaccinationRecordDTO.class), isNull(), eq(true));
  }

  @Test
  void convertVaccinationRecord_noDocumentsFound_anEmptyVaccinationRecordShouldBeReturned() {
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setFirstName("John");
    humanNameDTO.setLastName("Doe");
    PatientIdentifier patientIdentifier = new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "dummy", "dummy");
    patientIdentifier.setPatientInfo(humanNameDTO);

    CacheIdentifierKey cacheIdentifierKey = new CacheIdentifierKey(patientIdentifier, HttpSessionUtils.getAuthorFromSession());
    when(cache.createCacheIdentifier(patientIdentifier)).thenReturn(cacheIdentifierKey);
    when(cache.getData(cacheIdentifierKey, Cache.VACCINATION_RECORDS_CACHE_NAME)).thenReturn(Collections.emptyList());
    when(baseService.getAll(any(PatientIdentifier.class), any(Assertion.class), eq(true))).thenReturn(Collections.emptyList());

    VaccinationRecordDTO vaccinationRecordDTO =
        vaccinationRecordService.convertVaccinationToImmunization(patientIdentifier, null);

    assertNotNull(vaccinationRecordDTO);
    assertTrue(vaccinationRecordDTO.getVaccinations().isEmpty());
    assertTrue(vaccinationRecordDTO.getAllergies().isEmpty());
    assertTrue(vaccinationRecordDTO.getMedicalProblems().isEmpty());
    assertTrue(vaccinationRecordDTO.getPastIllnesses().isEmpty());
    assertNull(vaccinationRecordDTO.getCreatedAt());
  }

  @Test
  void convertVaccRecord_immunizationRecordsFoundInEPD_aVaccRecordWillBeCreatedWithoutConversion() {
    HumanNameDTO humanNameDTO = new HumanNameDTO();
    humanNameDTO.setFirstName("John");
    humanNameDTO.setLastName("Doe");
    PatientIdentifier patientIdentifier = new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "dummy", "dummy");
    patientIdentifier.setPatientInfo(humanNameDTO);

    CacheIdentifierKey cacheIdentifierKey = new CacheIdentifierKey(patientIdentifier, HttpSessionUtils.getAuthorFromSession());
    when(cache.createCacheIdentifier(patientIdentifier)).thenReturn(cacheIdentifierKey);
    when(cache.getData(cacheIdentifierKey)).thenReturn(Collections.emptyList());

    List<VaccinationDTO> vaccinations = vaccinationService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<AllergyDTO> allergies = allergyService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);
    List<MedicalProblemDTO> medicalProblems =
        medicalProblemService.getAll(EPDPLAYGROUND, "dummy", "dummy", null, true);

    List<BaseDTO> baseDTOS = new ArrayList<>();
    baseDTOS.addAll(vaccinations);
    baseDTOS.addAll(allergies);
    baseDTOS.addAll(pastIllnesses);
    baseDTOS.addAll(medicalProblems);
    when(baseService.getAll(patientIdentifier, null, true)).thenReturn(baseDTOS);

    VaccinationRecordDTO vaccinationRecordDTO =
        vaccinationRecordService.convertVaccinationToImmunization(patientIdentifier, null);

    assertNotNull(vaccinationRecordDTO);
    assertThat(vaccinationRecordDTO.getVaccinations().size()).isEqualTo(vaccinations.size());
    assertThat(vaccinationRecordDTO.getAllergies().size()).isEqualTo(allergies.size());
    assertThat(vaccinationRecordDTO.getPastIllnesses().size()).isEqualTo(pastIllnesses.size());
    assertThat(vaccinationRecordDTO.getMedicalProblems().size()).isEqualTo(medicalProblems.size());
  }

  private void setAuthorInSession(AuthorDTO author) {
    Objects.nonNull(author);
    HttpSessionUtils.setParameterInSession(HttpSessionUtils.AUTHOR, author);
  }

  private void setPatientIdentifierInSession(PatientIdentifier identifier) {
    Objects.nonNull(identifier);
    HttpSessionUtils.setParameterInSession(HttpSessionUtils.CACHE_PATIENT_IDENTIFIER, identifier);
  }
}
