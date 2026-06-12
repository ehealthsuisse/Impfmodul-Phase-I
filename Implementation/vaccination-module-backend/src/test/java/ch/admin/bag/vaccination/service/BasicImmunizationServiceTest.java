/**
 * Copyright (c) 2026 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
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
import ch.fhir.epr.adapter.data.dto.BasicImmunizationDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class BasicImmunizationServiceTest extends AbstractServiceTest {
  @Autowired
  private BasicImmunizationService basicImmunizationService;
  @MockitoSpyBean
  private HuskyAdapter huskyAdapter;

  @Test
  @Override
  public void testCreate() {
    ValueDTO code = new ValueDTO("bi-dtpa",
        "Received basic immunization against DTPa in childhood.",
        "http://fhir.ch/ig/ch-vacd/CodeSystem/ch-vacd-basic-immunization-cs");
    ValueDTO clinicalStatus = new ValueDTO("active", "Active", "testsystem");
    ValueDTO verificationStatus = new ValueDTO("confirmed", "Confirmed", "testsystem");
    String commentText = "Basic immunization test comment";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);
    BasicImmunizationDTO dto = new BasicImmunizationDTO(null, code, null, clinicalStatus,
        verificationStatus, LocalDate.now().minusYears(20), LocalDate.now(), comment);

    BasicImmunizationDTO result = basicImmunizationService.create(EPDCommunity.EPDPLAYGROUND.name(),
        "1.2.3.4.123456.1", "waldspital-Id-1234", dto, null, false);
    assertThat(result.getCode().getCode()).isEqualTo(code.getCode());
    assertThat(result.getClinicalStatus()).isEqualTo(clinicalStatus);
    assertThat(result.getVerificationStatus()).isEqualTo(verificationStatus);
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
        basicImmunizationService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<BasicImmunizationDTO> dtos = basicImmunizationService.getAll(patientIdentifier, null, true);
    assertThat(dtos.size()).isEqualTo(1);
    assertThat(dtos.getFirst().getId()).isEqualTo("db472869-3689-4cfc-9ec7-5d13988575a6");

    BasicImmunizationDTO result = basicImmunizationService.delete(EPDCommunity.EPDPLAYGROUND.name(),
        "1.2.3.4.123456.1", "waldspital-Id-1234", "db472869-3689-4cfc-9ec7-5d13988575a6",
        new ValueDTO("1141000195107", "Secret", "url"), null);

    assertThat(result.getRelatedId()).isEqualTo("db472869-3689-4cfc-9ec7-5d13988575a6");
    assertThat(result.getVerificationStatus().getCode()).isEqualTo(FhirConstants.ENTERED_IN_ERROR);
    assertThat(result.isDeleted()).isTrue();
    assertThat(result.getConfidentiality().getCode()).isEqualTo("1141000195107");
    assertThat(result.getConfidentiality().getName()).isEqualTo("Secret");
    assertThat(result.getConfidentiality().getSystem()).isEqualTo("url");

    dtos = basicImmunizationService.getAll(patientIdentifier, null, true);
    assertThat(dtos.size()).isEqualTo(0);

    // delete the resource again
    basicImmunizationService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1", "waldspital-Id-1234",
        "db472869-3689-4cfc-9ec7-5d13988575a6", new ValueDTO("1141000195107", "Secret", "url"), null);

    // check that no deletion file was created after the second deletion
    verify(huskyAdapter, atMostOnce()).writeDocument(any(), anyString(), anyString(), any(), any());
  }

  @Override
  @Test
  public void testGetAll() {
    List<BasicImmunizationDTO> dtos =
        basicImmunizationService.getAll(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);
    assertThat(dtos.size()).isGreaterThan(0);
    assertThat(dtos.getFirst().getClinicalStatus().getCode()).isNull();
    assertThat(dtos.getFirst().getVerificationStatus().getCode()).isEqualTo("unconfirmed");
    assertThat(dtos.getFirst().getId()).isEqualTo("db472869-3689-4cfc-9ec7-5d13988575a6");
    assertThat(dtos.getFirst().getCode().getCode()).isEqualTo("bi-hib");
  }

  @Test
  public void testGetAll_GAZELLE() {
    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), EPDCommunity.DUMMY.name(), "1.3.6.1.4.1.21367.13.20.3000"));
    assertThat(basicImmunizationService.getAll(EPDCommunity.GAZELLE.name(), "1.3.6.1.4.1.21367.13.20.3000",
        EPDCommunity.DUMMY.name(), null)).isNotEmpty();

    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), "IHEBLUE-2599", "1.3.6.1.4.1.21367.13.20.3000"));
    List<BasicImmunizationDTO> dtos =
        basicImmunizationService.getAll(EPDCommunity.GAZELLE.name(), "1.3.6.1.4.1.21367.13.20.3000",
            "IHEBLUE-2599", null);
    assertThat(dtos.size()).isEqualTo(1);
    assertThat(dtos.getFirst().getId()).isEqualTo("db472869-3689-4cfc-9ec7-5d13988575a6");
    assertThat(dtos.getFirst().getCode().getCode()).isEqualTo("bi-hib");
    assertThat(dtos.getFirst().getClinicalStatus().getCode()).isNull();
    assertThat(dtos.getFirst().getVerificationStatus().getCode()).isEqualTo("unconfirmed");
    assertThat(dtos.getFirst().getJson()).contains("db472869-3689-4cfc-9ec7-5d13988575a6");
  }

  @Override
  @Test
  public void testUpdate() {
    PatientIdentifier patientIdentifier =
        basicImmunizationService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<BasicImmunizationDTO> dtos = basicImmunizationService.getAll(patientIdentifier, null, true);
    assertThat(dtos.size()).isEqualTo(1);

    ValueDTO newCode = new ValueDTO("bi-polio",
        "Received basic immunization against poliomyelitis in childhood.",
        "http://fhir.ch/ig/ch-vacd/CodeSystem/ch-vacd-basic-immunization-cs");
    ValueDTO newClinicalStatus = new ValueDTO("inactive", "Inactive", "testsystem");
    ValueDTO newVerificationStatus = new ValueDTO("confirmed", "Confirmed", "testsystem");
    String commentText = "Updated comment";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);

    BasicImmunizationDTO newDto = new BasicImmunizationDTO(null, newCode, null, newClinicalStatus,
        newVerificationStatus, LocalDate.now().minusYears(20), LocalDate.now(), comment);
    newDto.setAuthor(author);

    BasicImmunizationDTO dto =
        basicImmunizationService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "db472869-3689-4cfc-9ec7-5d13988575a6", newDto, null);
    assertThat(dto.getRelatedId()).isEqualTo("db472869-3689-4cfc-9ec7-5d13988575a6");
    assertThat(dto.getCode()).isEqualTo(newCode);
    assertThat(dto.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(dto.getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));
    assertThat(dto.getComment().getAuthor()).isEqualTo(author.getUser().getFullName());
    assertThat(dto.getComment().getText()).isEqualTo(commentText);
    assertThat(dto.getComment().getDate()).isNotNull();

    dtos = basicImmunizationService.getAll(patientIdentifier, null, true);
    assertThat(dtos.size()).isEqualTo(1);
  }

  @Override
  @Test
  public void testValidate() {
    ValueDTO newCode = new ValueDTO("bi-dtpa",
        "Received basic immunization against DTPa in childhood.",
        "http://fhir.ch/ig/ch-vacd/CodeSystem/ch-vacd-basic-immunization-cs");
    ValueDTO newClinicalStatus = new ValueDTO("active", "Active", "testsystem");

    BasicImmunizationDTO newDto = new BasicImmunizationDTO(null, newCode, null, newClinicalStatus, null,
        LocalDate.now().minusYears(20), LocalDate.now(), null);
    newDto.setAuthor(author);

    BasicImmunizationDTO dto =
        basicImmunizationService.validate(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "db472869-3689-4cfc-9ec7-5d13988575a6", newDto, null);
    assertThat(dto.getRelatedId()).isEqualTo("db472869-3689-4cfc-9ec7-5d13988575a6");
    assertThat(dto.getCode()).isEqualTo(newCode);
    assertThat(dto.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(dto.getVerificationStatus())
        .isEqualTo(new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status"));
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
    validate(new BasicImmunizationDTO(), basicImmunizationService, role, expectedExceptionMessage);
  }
}
