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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;

import ch.admin.bag.vaccination.service.husky.HuskyAdapter;
import ch.admin.bag.vaccination.service.husky.HuskyUtils;
import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.fhir.epr.adapter.FhirConstants;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class VaccinationServiceTest extends AbstractServiceTest {
  @Autowired
  private VaccinationService vaccinationService;
  @Autowired
  private VaccinationConfig vaccinationConfig;
  @MockitoSpyBean
  private HuskyAdapter huskyAdapter;

  @Override
  @Test
  public void testCreate() {
    PatientIdentifier patientIdentifier =
      vaccinationService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234");

    List<VaccinationDTO> vaccinations = vaccinationService.getAll(patientIdentifier, null, true);
    assertThat(vaccinations.size()).isEqualTo(3);

    HumanNameDTO performer = new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null);

    ValueDTO vaccineCode = new ValueDTO("123456789", "123456789", "testsystem");
    ValueDTO status = new ValueDTO("completed", "completed", "http://hl7.org/fhir/event-status");
    ValueDTO verificationStatus = new ValueDTO("59156000", "Confirmed", "http://snomed.info/sct");
    ValueDTO targetDisease = new ValueDTO("38907003", "Varicella", "http://snomed.info/sct");
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);
    VaccinationDTO newVaccinationDTO = new VaccinationDTO(null, vaccineCode, List.of(targetDisease), comment,
      3, LocalDate.now(), performer, null, "lotNumber", null, status, verificationStatus);
    newVaccinationDTO.setAuthor(author);

    VaccinationDTO result = vaccinationService.create(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
      "waldspital-Id-1234", newVaccinationDTO, null, false);
    assertThat(result.getCode().getCode()).isEqualTo(vaccineCode.getCode());
    assertThat(result.getStatus()).isEqualTo(status);
    assertThat(result.getVerificationStatus()).isEqualTo(verificationStatus);
    assertThat(result.getRecorder().getFullName()).isEqualTo(performer.getFullName());
    assertThat(result.getLotNumber()).isEqualTo("lotNumber");
    assertThat(result.isValidated()).isTrue();
    assertThat(result.getConfidentiality().getCode()).isEqualTo(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getCode());
    assertThat(result.getConfidentiality().getName()).isEqualTo(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getName());
    assertThat(result.getConfidentiality().getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);
    assertThat(result.getComment().getAuthor()).isEqualTo(author.getUser().getFullName());
    assertThat(result.getComment().getText()).isEqualTo(commentText);
    assertThat(result.getComment().getDate()).isNotNull();

    vaccinations = vaccinationService.getAll(patientIdentifier, null, true);
    assertThat(vaccinations.size()).isEqualTo(4);
  }

  @Override
  @Test
  public void testDelete() {
    PatientIdentifier patientIdentifier =
      vaccinationService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234");

    List<VaccinationDTO> vaccinations = vaccinationService.getAll(patientIdentifier, null, true);
    assertThat(vaccinations.size()).isEqualTo(3);

    VaccinationDTO result =
      vaccinationService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", "acc1f090-5e0c-45ae-b283-521d57c3aa2f",
        new ValueDTO("1141000195107", "Secret", "url"), null);

    assertThat(result.getRelatedId()).isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    assertThat(result.getStatus().getCode()).isEqualTo(FhirConstants.ENTERED_IN_ERROR);
    assertThat(result.isDeleted()).isTrue();
    assertThat(result.getConfidentiality().getCode()).isEqualTo("1141000195107");
    assertThat(result.getConfidentiality().getName()).isEqualTo("Secret");
    assertThat(result.getConfidentiality().getSystem()).isEqualTo("url");

    vaccinations = vaccinationService.getAll(patientIdentifier, null, true);

    assertThat(vaccinations.size()).isEqualTo(2);

    // delete the resource again
    vaccinationService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1", "waldspital-Id-1234",
      "acc1f090-5e0c-45ae-b283-521d57c3aa2f", new ValueDTO("1141000195107", "Secret", "url"), null);

    // check that no deletion file was created after the second deletion
    verify(huskyAdapter, atMostOnce()).writeDocument(any(), anyString(), anyString(), any(), any());
  }

  @Override
  @Test
  public void testGetAll() {
    List<VaccinationDTO> vaccinationDTOs =
      vaccinationService.getAll(EPDCommunity.EPDPLAYGROUND.name(),
        "1.2.3.4.123456.1",
        "waldspital-Id-1234", null);

    assertThat(vaccinationDTOs.size()).isEqualTo(3);
    assertThat(vaccinationDTOs.get(2).getId()).isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    assertThat(vaccinationDTOs.get(2).getDoseNumber()).isEqualTo(1);
    assertThat(vaccinationDTOs.get(2).getTargetDiseases().getFirst().getCode()).isEqualTo("40468003");
    assertThat(vaccinationDTOs.get(2).getCode().getCode()).isEqualTo("558");
    assertThat(vaccinationDTOs.get(2).getLotNumber()).isEqualTo("AHAVB946A");
    assertThat(vaccinationDTOs.get(2).getAuthor().getUser().getLastName()).isEqualTo("Wegmueller");
    assertThat(vaccinationDTOs.get(2).getRecorder().getLastName()).endsWith("ller");
    assertThat(vaccinationDTOs.get(2).getJson()).contains("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
  }

  @Override
  @Test
  public void testUpdate() {
    PatientIdentifier patientIdentifier =
      vaccinationService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234");
    List<VaccinationDTO> vaccinations = vaccinationService.getAll(patientIdentifier, null, true);
    assertThat(vaccinations.size()).isEqualTo(3);

    ValueDTO vaccineCode = new ValueDTO("987654321", "123456789", "testsystem");
    ValueDTO newStatus = new ValueDTO("completed", "completed", "http://hl7.org/fhir/event-status");
    ValueDTO verificationStatus = new ValueDTO("59156000", "Confirmed", "http://snomed.info/sct");
    ValueDTO targetDisease = new ValueDTO("38907003", "Varicella", "http://snomed.info/sct");
    String newOrga = "My new organization AG";
    String newLotNumber = "newLotNumber";
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);
    VaccinationDTO newVaccinationDTO = new VaccinationDTO(null, vaccineCode, List.of(targetDisease),
      comment, 3,
      LocalDate.now(), performer, newOrga, newLotNumber, null, newStatus, verificationStatus);
    newVaccinationDTO.setAuthor(author);

    VaccinationDTO updatedVaccinationDTO =
      vaccinationService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", "acc1f090-5e0c-45ae-b283-521d57c3aa2f", newVaccinationDTO, null);
    assertThat(updatedVaccinationDTO.getRelatedId()).isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    assertThat(updatedVaccinationDTO.getCode()).isEqualTo(vaccineCode);
    assertThat(updatedVaccinationDTO.getStatus()).isEqualTo(newStatus);
    assertThat(updatedVaccinationDTO.getVerificationStatus()).isEqualTo(verificationStatus);
    assertThat(updatedVaccinationDTO.getLotNumber()).isEqualTo(newLotNumber);
    assertThat(updatedVaccinationDTO.getOrganization()).isEqualTo("My new organization AG");
    assertThat(updatedVaccinationDTO.getComment().getAuthor())
      .isEqualTo(author.getUser().getFullName());
    assertThat(updatedVaccinationDTO.getComment().getText()).isEqualTo("BlaBla");
    assertThat(updatedVaccinationDTO.getComment().getDate()).isNotNull();

    vaccinations = vaccinationService.getAll(patientIdentifier, null, true);
    assertThat(vaccinations.size()).isEqualTo(3);
  }

  @Test
  public void testUpdate_authorAsPAT_multipleFieldsWereUpdated_verificationStatusShouldChangeToNotConfirmed() {
    PatientIdentifier patientIdentifier =
        vaccinationService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");
    List<VaccinationDTO> vaccinations = vaccinationService.getAll(patientIdentifier, null, true);
    assertNull(vaccinations.getFirst().getVerificationStatus());

    VaccinationDTO newVaccinationDTO = vaccinations.getFirst();
    String newOrga = "My new organization AG";
    String newLotNumber = "newLotNumber";
    String commentText = "BlaBla";
    ValueDTO verificationStatus = new ValueDTO("76104008", "Not confirmed", "http://snomed.info/sct");
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);

    author.setRole("PAT");
    newVaccinationDTO.setAuthor(author);
    newVaccinationDTO.setOrganization(newOrga);
    newVaccinationDTO.setLotNumber(newLotNumber);
    newVaccinationDTO.setComment(comment);
    newVaccinationDTO.setVerificationStatus(verificationStatus);

    VaccinationDTO updatedVaccinationDTO =
        vaccinationService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "acc1f090-5e0c-45ae-b283-521d57c3aa2f", newVaccinationDTO, null);
    assertThat(updatedVaccinationDTO.getRelatedId()).isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    assertThat(updatedVaccinationDTO.getVerificationStatus()).isEqualTo(verificationStatus);
    assertThat(updatedVaccinationDTO.getLotNumber()).isEqualTo(newLotNumber);
    assertThat(updatedVaccinationDTO.getOrganization()).isEqualTo(newOrga);
    assertThat(updatedVaccinationDTO.getComment().getAuthor())
        .isEqualTo(author.getUser().getFullName());
    assertThat(updatedVaccinationDTO.getComment().getText()).isEqualTo("BlaBla");
    assertThat(updatedVaccinationDTO.getComment().getDate()).isNotNull();
  }

  @Override
  @Test
  public void testValidate() {
    ValueDTO vaccineCode = new ValueDTO("987654321", "123456789", "testsystem");
    ValueDTO newStatus = new ValueDTO(FhirConstants.ENTERED_IN_ERROR, FhirConstants.ENTERED_IN_ERROR,
      "http://hl7.org/fhir/event-status");
    ValueDTO verificationStatus = new ValueDTO("59156000", "Confirmed", "http://snomed.info/sct");
    ValueDTO targetDisease = new ValueDTO("38907003", "Varicella", "http://snomed.info/sct");
    String newOrga = "My new organization AG";
    String newLotNumber = "newLotNumber";
    VaccinationDTO newVaccinationDTO = new VaccinationDTO(null, vaccineCode, List.of(targetDisease), null, 3,
      LocalDate.now(), performer, newOrga, newLotNumber, null, newStatus, verificationStatus);
    newVaccinationDTO.setAuthor(author);

    VaccinationDTO updatedVaccinationDTO =
      vaccinationService.validate(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", "acc1f090-5e0c-45ae-b283-521d57c3aa2f", newVaccinationDTO, null);
    assertThat(updatedVaccinationDTO.getRelatedId()).isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    assertThat(updatedVaccinationDTO.getCode()).isEqualTo(vaccineCode);
    assertThat(updatedVaccinationDTO.getStatus()).isEqualTo(newStatus);
    assertThat(updatedVaccinationDTO.getVerificationStatus()).isEqualTo(verificationStatus);
    assertThat(updatedVaccinationDTO.getLotNumber()).isEqualTo(newLotNumber);
    assertThat(updatedVaccinationDTO.getOrganization()).isEqualTo("My new organization AG");
  }

  @Test
  void convertVaccinationToImmunization_() {
    List<VaccinationDTO> vaccinationDTOs = vaccinationService.getAll(EPDCommunity.GAZELLE.name(),
      "1.3.6.1.4.1.21367.13.20.3000", "IHEBLUE-2599", null);
    assertThat(vaccinationDTOs.size()).isEqualTo(3);
    assertThat(vaccinationDTOs.getFirst().getOrganization()).contains("Gruppenpraxis Müller");
    assertThat(vaccinationDTOs.get(1).getTargetDiseases().getFirst().getCode()).isEqualTo("712986001");
    assertThat(vaccinationDTOs.get(2).getStatus().getCode()).isEqualTo("completed");
  }

  @Test
  void getData_existingData_GAZELLE() {
    List<VaccinationDTO> vaccinationDTOs = vaccinationService.getAll(EPDCommunity.GAZELLE.name(),
      "1.3.6.1.4.1.21367.13.20.3000", "IHEBLUE-2599", null);
    assertThat(vaccinationDTOs.size()).isEqualTo(3);
    assertThat(vaccinationDTOs.getFirst().getOrganization()).contains("Gruppenpraxis Müller");
    assertThat(vaccinationDTOs.get(1).getTargetDiseases().getFirst().getCode()).isEqualTo("712986001");
    assertThat(vaccinationDTOs.get(2).getStatus().getCode()).isEqualTo("completed");
  }

  @BeforeEach
  void setUp() {
    super.before();

    setPatientIdentifierInSession(
      new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "waldspital-Id-1234", "1.2.3.4.123456.1"));
  }

  @Test
  void vaccinationConfig() {
    assertThat(vaccinationConfig.getFormatCodes().size()).isEqualTo(2);
    assertThat(vaccinationConfig.getFormatCodes().getFirst().getCode())
      .isEqualTo("urn:che:epr:ch-vacd:immunization-administration:2022");
    assertThat(vaccinationConfig.getFormatCodes().get(1).getCode())
      .isEqualTo("urn:che:epr:ch-vacd:vaccination-record:2022");
  }

  @Test
  void validate_only_for_HCP_or_ASS() {
    validate("NotHCPAndNotASS", "HCP or ASS role required!");
    validate("HCP", "Cannot invoke \"String.equals(Object)\" because \"id\" is null");
    validate("ASS", "Cannot invoke \"String.equals(Object)\" because \"id\" is null");
  }

  private void validate(String role, String expectedExceptionMessage) {
    validate(new VaccinationDTO(), vaccinationService, role, expectedExceptionMessage);
  }
}