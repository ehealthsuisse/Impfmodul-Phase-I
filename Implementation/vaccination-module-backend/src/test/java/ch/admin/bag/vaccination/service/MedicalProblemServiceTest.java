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
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class MedicalProblemServiceTest extends AbstractServiceTest {
  @Autowired
  private MedicalProblemService medicalProblemService;
  @MockitoSpyBean
  private HuskyAdapter huskyAdapter;

  @Test
  @Override
  public void testCreate() {
    HumanNameDTO recorder = new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null);

    ValueDTO illnessCode = new ValueDTO("123456789", "987654321", "testsystem");
    ValueDTO clinicalStatus = new ValueDTO("clinicalStatus", "clinicalStatus", "testsystem");
    ValueDTO verificationStatus = new ValueDTO("verificationStatus", "verificationStatus", "testsystem");
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, recorder.getFullName(), commentText);
    MedicalProblemDTO dto = new MedicalProblemDTO(null, illnessCode, clinicalStatus, verificationStatus,
        LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2001, 12, 31), recorder, comment,
        "My organization AG");

    MedicalProblemDTO result = medicalProblemService.create(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", dto, null, false);
    assertThat(result.getCode().getCode()).isEqualTo(illnessCode.getCode());
    assertThat(result.getClinicalStatus()).isEqualTo(clinicalStatus);
    assertThat(result.getVerificationStatus()).isEqualTo(verificationStatus);
    assertThat(result.getBegin()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(result.getEnd()).isEqualTo(LocalDate.of(2001, 12, 31));
    assertThat(result.getConfidentiality().getCode()).isEqualTo(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getCode());
    assertThat(result.getConfidentiality().getName()).isEqualTo(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getName());
    assertThat(result.getConfidentiality().getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);
    assertThat(result.getComment().getAuthor()).isEqualTo(author.getUser().getFullName());
    assertThat(result.getComment().getText()).isEqualTo(commentText);
    assertThat(result.getComment().getDate()).isNotNull();
    assertThat(result.isValidated()).isTrue();
  }

  @Override
  @Test
  public void testDelete() {
    PatientIdentifier patientIdentifier =
        medicalProblemService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<MedicalProblemDTO> dtos = medicalProblemService.getAll(patientIdentifier, null, true);
    assertThat(dtos.size()).isEqualTo(1);
    assertThat(dtos.getFirst().getId()).isEqualTo("30327ea1-6893-4c65-896e-c32c394f1ec6");

    MedicalProblemDTO result = medicalProblemService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", "30327ea1-6893-4c65-896e-c32c394f1ec6", new ValueDTO("1141000195107", "Secret", "url"),
        null);

    assertThat(result.getRelatedId()).isEqualTo("30327ea1-6893-4c65-896e-c32c394f1ec6");
    assertThat(result.getVerificationStatus().getCode()).isEqualTo(FhirConstants.ENTERED_IN_ERROR);
    assertThat(result.isDeleted()).isTrue();
    assertThat(result.getConfidentiality().getCode()).isEqualTo("1141000195107");
    assertThat(result.getConfidentiality().getName()).isEqualTo("Secret");
    assertThat(result.getConfidentiality().getSystem()).isEqualTo("url");

    dtos = medicalProblemService.getAll(patientIdentifier, null, true);
    assertThat(dtos.size()).isEqualTo(0);

    // delete the resource again
    medicalProblemService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1", "waldspital-Id-1234",
        "30327ea1-6893-4c65-896e-c32c394f1ec6", new ValueDTO("1141000195107", "Secret", "url"), null);

    // check that no deletion file was created after the second deletion
    verify(huskyAdapter, atMostOnce()).writeDocument(any(), anyString(), anyString(), any(), any());
  }

  @Override
  @Test
  public void testGetAll() {
    List<MedicalProblemDTO> dtos =
        medicalProblemService.getAll(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);
    assertThat(dtos.size()).isGreaterThan(0);
    assertThat(dtos.getFirst().getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(dtos.getFirst().getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(dtos.getFirst().getId()).isEqualTo("30327ea1-6893-4c65-896e-c32c394f1ec6");
    assertThat(dtos.getFirst().getCode().getCode()).isEqualTo("402196005");
  }

  @Test
  public void testGetAll_GAZELLE() {
    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), EPDCommunity.DUMMY.name(), "1.3.6.1.4.1.21367.13.20.3000"));
    assertThat(medicalProblemService.getAll(EPDCommunity.GAZELLE.name(), "1.3.6.1.4.1.21367.13.20.3000",
        EPDCommunity.DUMMY.name(), null)).isNotEmpty();

    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), "IHEBLUE-2599", "1.3.6.1.4.1.21367.13.20.3000"));
    List<MedicalProblemDTO> dtos =
        medicalProblemService.getAll(EPDCommunity.GAZELLE.name(), "1.3.6.1.4.1.21367.13.20.3000",
            "IHEBLUE-2599", null);
    assertThat(dtos.size()).isEqualTo(1);
    assertThat(dtos.getFirst().getId()).isEqualTo("30327ea1-6893-4c65-896e-c32c394f1ec6");
    assertThat(dtos.getFirst().getCode().getCode()).isEqualTo("402196005");
    assertThat(dtos.getFirst().getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(dtos.getFirst().getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(dtos.getFirst().getJson()).contains("30327ea1-6893-4c65-896e-c32c394f1ec6");
  }

  @Override
  @Test
  public void testUpdate() {
    PatientIdentifier patientIdentifier =
        medicalProblemService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<MedicalProblemDTO> dtos = medicalProblemService.getAll(patientIdentifier, null, true);
    assertThat(dtos.size()).isEqualTo(1);

    HumanNameDTO recorder = new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null);

    ValueDTO newIllnessCode = new ValueDTO("newCode", "newCode", "testsystem");
    ValueDTO newClinicalStatus = new ValueDTO("newClinicalStatus", "newClinicalStatus", "testsystem");
    ValueDTO newVerificationStatus = new ValueDTO("newVerificationStatus", "newVerificationStatus", "testsystem");
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);

    MedicalProblemDTO newDto = new MedicalProblemDTO(null, newIllnessCode, newClinicalStatus,
        newVerificationStatus,
        LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2001, 12, 31), recorder, comment,
        "My new organization AG");
    newDto.setAuthor(author);

    MedicalProblemDTO dto =
        medicalProblemService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "30327ea1-6893-4c65-896e-c32c394f1ec6", newDto, null);
    assertThat(dto.getRelatedId()).isEqualTo("30327ea1-6893-4c65-896e-c32c394f1ec6");
    assertThat(dto.getCode()).isEqualTo(newIllnessCode);
    assertThat(dto.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(dto.getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));
    assertThat(dto.getOrganization()).isEqualTo("My new organization AG");
    assertThat(dto.getBegin()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(dto.getEnd()).isEqualTo(LocalDate.of(2001, 12, 31));
    assertThat(dto.getComment().getAuthor()).isEqualTo(author.getUser().getFullName());
    assertThat(dto.getComment().getText()).isEqualTo(commentText);
    assertThat(dto.getComment().getDate()).isNotNull();

    dtos = medicalProblemService.getAll(patientIdentifier, null, true);
    assertThat(dtos.size()).isEqualTo(1);
  }

  @Test
  public void testUpdate_authorAsNewHCP_clinicalStatusHasChanged_verificationStatusShouldRemainUnchanged() {
    PatientIdentifier patientIdentifier =
        medicalProblemService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<MedicalProblemDTO> dtos = medicalProblemService.getAll(patientIdentifier, null, true);
    assertThat(dtos.size()).isEqualTo(1);
    assertThat(dtos.getFirst().getAuthor().getRole()).isEqualTo("HCP");
    assertThat(dtos.getFirst().getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));

    ValueDTO newClinicalStatus = new ValueDTO("newClinicalStatus", "newClinicalStatus", "testsystem");

    MedicalProblemDTO newDto = dtos.getFirst();
    newDto.setAuthor(author);
    newDto.setClinicalStatus(newClinicalStatus);

    MedicalProblemDTO dto =
        medicalProblemService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "30327ea1-6893-4c65-896e-c32c394f1ec6", newDto, null);
    assertThat(dto.getRelatedId()).isEqualTo("30327ea1-6893-4c65-896e-c32c394f1ec6");
    assertThat(dto.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(dto.getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));
  }

  @Override
  @Test
  public void testValidate() {
    HumanNameDTO recorder = new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null);
    ValueDTO newIllnessCode = new ValueDTO("newCode", "newCode", "testsystem");
    ValueDTO newClinicalStatus = new ValueDTO("newClinicalStatus", "newClinicalStatus", "testsystem");

    MedicalProblemDTO newDto = new MedicalProblemDTO(null, newIllnessCode, newClinicalStatus, null,
        LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2001, 12, 31), recorder, null,
        "My new organization AG");
    newDto.setAuthor(author);

    MedicalProblemDTO dto =
        medicalProblemService.validate(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "30327ea1-6893-4c65-896e-c32c394f1ec6", newDto, null);
    assertThat(dto.getRelatedId()).isEqualTo("30327ea1-6893-4c65-896e-c32c394f1ec6");
    assertThat(dto.getCode()).isEqualTo(newIllnessCode);
    assertThat(dto.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(dto.getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));
    assertThat(dto.getOrganization()).isEqualTo("My new organization AG");
    assertThat(dto.getBegin()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(dto.getEnd()).isEqualTo(LocalDate.of(2001, 12, 31));
  }

  // @Test
  void getData_emptyData_EPDPLAYGROUND() {
    assertThat(
        medicalProblemService.getAll(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            EPDCommunity.DUMMY.name(), null)).isEmpty();
  }

  @BeforeEach
  void setUp() {
    super.before();

    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "waldspital-Id-1234", "1.2.3.4.123456.1"));
  }

  @Test
  void validate_only_for_HCP_or_ASS() {
    validate("NotHCPAndNotASS", "HCP or ASS role required!");
    validate("HCP", "Cannot invoke \"String.equals(Object)\" because \"id\" is null");
    validate("ASS", "Cannot invoke \"String.equals(Object)\" because \"id\" is null");
  }

  private void validate(String role, String expectedExceptionMessage) {
    validate(new MedicalProblemDTO(), medicalProblemService, role, expectedExceptionMessage);
  }
}
