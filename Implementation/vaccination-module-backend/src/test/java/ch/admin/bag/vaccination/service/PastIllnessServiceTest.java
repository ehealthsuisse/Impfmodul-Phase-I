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
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class PastIllnessServiceTest extends AbstractServiceTest {
  @Autowired
  private PastIllnessService pastIllnessService;
  @MockitoSpyBean
  private HuskyAdapter huskyAdapter;

  @Override
  @Test
  public void testCreate() {
    HumanNameDTO recorder = new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null);

    ValueDTO illnessCode = new ValueDTO("123456789", "987654321", "testsystem");
    ValueDTO clinicalStatus = new ValueDTO("clinicalStatus", "clinicalStatus", "testsystem");
    ValueDTO verificationStatus = new ValueDTO("verificationStatus", "verificationStatus", "testsystem");
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, recorder.getFullName(), commentText);
    PastIllnessDTO newPastIllnessDTO = new PastIllnessDTO(null, illnessCode, clinicalStatus, verificationStatus,
        LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2001, 12, 31), recorder, comment,
        "My organization AG");
    author.setRole("PAT");
    newPastIllnessDTO.setAuthor(author);

    PastIllnessDTO result = pastIllnessService.create(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", newPastIllnessDTO, null, false);
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
    assertThat(result.isValidated()).isFalse();
  }

  @Override
  @Test
  public void testDelete() {
    PatientIdentifier patientIdentifier =
        pastIllnessService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll(patientIdentifier, null, true);
    assertThat(pastIllnesses.size()).isEqualTo(1);

    PastIllnessDTO result = pastIllnessService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", "5f727b7b-87ae-464f-85ac-1a45d23f0897", new ValueDTO("1141000195107",
            "Secret", "url"), null);

    assertThat(result.getRelatedId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(result.getVerificationStatus().getCode()).isEqualTo(FhirConstants.ENTERED_IN_ERROR);
    assertThat(result.isDeleted()).isTrue();
    assertThat(result.getConfidentiality().getCode()).isEqualTo("1141000195107");
    assertThat(result.getConfidentiality().getName()).isEqualTo("Secret");
    assertThat(result.getConfidentiality().getSystem()).isEqualTo("url");

    pastIllnesses = pastIllnessService.getAll(patientIdentifier, null, true);
    assertThat(pastIllnesses.size()).isEqualTo(0);

    // delete the resource again
    pastIllnessService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1", "waldspital-Id-1234",
        "5f727b7b-87ae-464f-85ac-1a45d23f0897", new ValueDTO("1141000195107", "Secret", "url"), null);

    // check that no deletion file was created after the second deletion
    verify(huskyAdapter, atMostOnce()).writeDocument(any(), anyString(), anyString(), any(), any());
  }

  @Override
  @Test
  public void testGetAll() {
    List<PastIllnessDTO> pastIllnessDTOs =
        pastIllnessService.getAll(EPDCommunity.EPDPLAYGROUND.name(),
            "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);
    assertThat(pastIllnessDTOs.size()).isGreaterThan(0);
    assertThat(pastIllnessDTOs.getFirst().getClinicalStatus().getCode()).isEqualTo("resolved");
    assertThat(pastIllnessDTOs.getFirst().getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(pastIllnessDTOs.getFirst().getId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(pastIllnessDTOs.getFirst().getCode().getCode()).isEqualTo("38907003");
  }

  @Test
  public void testGetAll_GAZELLE() {
    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), EPDCommunity.DUMMY.name(), "1.3.6.1.4.1.21367.13.20.3000"));
    assertThat(pastIllnessService.getAll(EPDCommunity.GAZELLE.name(), "1.3.6.1.4.1.21367.13.20.3000",
        EPDCommunity.DUMMY.name(), null)).isNotEmpty();

    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), "IHEBLUE-2599", "1.3.6.1.4.1.21367.13.20.3000"));
    List<PastIllnessDTO> pastIllnessDTOs = pastIllnessService.getAll(EPDCommunity.GAZELLE.name(),
        "1.3.6.1.4.1.21367.13.20.3000", "IHEBLUE-2599", null);
    assertThat(pastIllnessDTOs.size()).isEqualTo(1);
    assertThat(pastIllnessDTOs.getFirst().getId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(pastIllnessDTOs.getFirst().getCode().getCode()).isEqualTo("38907003");
    assertThat(pastIllnessDTOs.getFirst().getClinicalStatus().getCode()).isEqualTo("resolved");
    assertThat(pastIllnessDTOs.getFirst().getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(pastIllnessDTOs.getFirst().getJson()).contains("5f727b7b-87ae-464f-85ac-1a45d23f0897");
  }

  @Override
  @Test
  public void testUpdate() {
    PatientIdentifier patientIdentifier =
        pastIllnessService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll(patientIdentifier, null, true);
    assertThat(pastIllnesses.size()).isEqualTo(1);

    HumanNameDTO recorder = new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null);

    ValueDTO newIllnessCode = new ValueDTO("newCode", "newCode", "testsystem");
    ValueDTO newClinicalStatus = new ValueDTO("newClinicalStatus", "newClinicalStatus", "testsystem");
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);

    PastIllnessDTO newPastIllnessDTO = new PastIllnessDTO(null, newIllnessCode, newClinicalStatus, null,
        LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2001, 12, 31), recorder, comment,
        "My new organization AG");
    newPastIllnessDTO.setAuthor(author);

    PastIllnessDTO updatedPastIllnessDTO =
        pastIllnessService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "5f727b7b-87ae-464f-85ac-1a45d23f0897", newPastIllnessDTO, null);
    assertThat(updatedPastIllnessDTO.getRelatedId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(updatedPastIllnessDTO.getCode()).isEqualTo(newIllnessCode);
    assertThat(updatedPastIllnessDTO.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(updatedPastIllnessDTO.getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));
    assertThat(updatedPastIllnessDTO.getOrganization()).isEqualTo("My new organization AG");
    assertThat(updatedPastIllnessDTO.getBegin()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(updatedPastIllnessDTO.getEnd()).isEqualTo(LocalDate.of(2001, 12, 31));
    assertThat(updatedPastIllnessDTO.getComment().getAuthor())
        .isEqualTo(author.getUser().getFullName());
    assertThat(updatedPastIllnessDTO.getComment().getText()).isEqualTo("BlaBla");
    assertThat(updatedPastIllnessDTO.getComment().getDate()).isNotNull();

    pastIllnesses = pastIllnessService.getAll(patientIdentifier, null, true);
    assertThat(pastIllnesses.size()).isEqualTo(1);
  }

  @Test
  public void testUpdate_authorAsREP_onlyCommentChanged_verificationStatusRemainsTheSame() {
    PatientIdentifier patientIdentifier =
        pastIllnessService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll(patientIdentifier, null, true);
    assertThat(pastIllnesses.size()).isEqualTo(1);
    assertThat(pastIllnesses.getFirst().getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));

    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);

    PastIllnessDTO newPastIllnessDTO = pastIllnesses.getFirst();
    author.setRole("REP");
    newPastIllnessDTO.setAuthor(author);
    newPastIllnessDTO.setComment(comment);

    PastIllnessDTO updatedPastIllnessDTO =
        pastIllnessService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "5f727b7b-87ae-464f-85ac-1a45d23f0897", newPastIllnessDTO, null);
    assertThat(updatedPastIllnessDTO.getRelatedId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(updatedPastIllnessDTO.getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));
    assertThat(updatedPastIllnessDTO.getComment().getAuthor())
        .isEqualTo(author.getUser().getFullName());
    assertThat(updatedPastIllnessDTO.getComment().getText()).isEqualTo("BlaBla");
    assertThat(updatedPastIllnessDTO.getComment().getDate()).isNotNull();
  }

  @Override
  @Test
  public void testValidate() {
    HumanNameDTO recorder = new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null);

    ValueDTO newIllnessCode = new ValueDTO("newCode", "newCode", "testsystem");
    ValueDTO newClinicalStatus = new ValueDTO("newClinicalStatus", "newClinicalStatus", "testsystem");
    ValueDTO newVerificationStatus = new ValueDTO("newVerificationStatus", "newVerificationStatus", "testsystem");

    PastIllnessDTO newPastIllnessDTO = new PastIllnessDTO(null, newIllnessCode, newClinicalStatus, newVerificationStatus,
        LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2001, 12, 31), recorder, null,
        "My new organization AG");

    PastIllnessDTO updatedPastIllnessDTO =
        pastIllnessService.validate(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "5f727b7b-87ae-464f-85ac-1a45d23f0897", newPastIllnessDTO, null);
    assertThat(updatedPastIllnessDTO.getRelatedId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(updatedPastIllnessDTO.getCode()).isEqualTo(newIllnessCode);
    assertThat(updatedPastIllnessDTO.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(updatedPastIllnessDTO.getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));
    assertThat(updatedPastIllnessDTO.getOrganization()).isEqualTo("My new organization AG");
    assertThat(updatedPastIllnessDTO.getBegin()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(updatedPastIllnessDTO.getEnd()).isEqualTo(LocalDate.of(2001, 12, 31));
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
    validate(new PastIllnessDTO(), pastIllnessService, role, expectedExceptionMessage);
  }
}
