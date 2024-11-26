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
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

class AllergyServiceTest extends AbstractServiceTest {
  @Autowired
  private AllergyService allergyService;
  @SpyBean
  private HuskyAdapter huskyAdapter;

  @Override
  @Test
  public void testCreate() {
    HumanNameDTO recorder = new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null);

    ValueDTO allergyCode = new ValueDTO("123456789", "987654321", "testsystem");
    ValueDTO criticality = new ValueDTO("low", "lowName", "testsystem");
    ValueDTO clinicalStatus = new ValueDTO("clinicalStatus", "clinicalStatusName", "testsystem");
    ValueDTO verficationStatus = new ValueDTO("verficationStatus", "verficationStatusName", "testsystem");
    ValueDTO type = new ValueDTO(AllergyIntoleranceType.ALLERGY.toCode(),
        AllergyIntoleranceType.ALLERGY.getDisplay(), AllergyIntoleranceType.ALLERGY.getSystem());
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);
    AllergyDTO newAllergyDTO = new AllergyDTO(null, LocalDate.now(), allergyCode, criticality,
        clinicalStatus, verficationStatus, type, recorder, List.of(comment), "My organization AG");
    author.setRole("REP");
    AllergyDTO result =
        allergyService.create(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", newAllergyDTO, null);
    assertThat(result.getCode().getCode()).isEqualTo(allergyCode.getCode());
    assertThat(result.getCriticality().getCode()).isEqualTo(criticality.getCode());
    assertThat(result.getClinicalStatus()).isEqualTo(clinicalStatus);
    assertThat(result.getVerificationStatus()).isEqualTo(verficationStatus);
    assertThat(result.getType()).isEqualTo(type);
    assertThat(result.getConfidentiality().getCode()).isEqualTo(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getCode());
    assertThat(result.getConfidentiality().getName()).isEqualTo(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getName());
    assertThat(result.getConfidentiality().getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);
    assertThat(result.getComments().get(0).getAuthor()).isEqualTo(author.getUser().getFullName());
    assertThat(result.getComments().get(0).getText()).isEqualTo(commentText);
    assertThat(result.getComments().get(0).getDate()).isNotNull();
    assertThat(result.isValidated()).isFalse();
  }

  @Override
  @Test
  public void testDelete() {
    PatientIdentifier patientIdentifier =
        allergyService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<AllergyDTO> allergies = allergyService.getAll(patientIdentifier, null, true);
    assertThat(allergies.size()).isEqualTo(1);

    AllergyDTO result =
        allergyService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "00476f5f-f3b7-4e49-9b52-5ec88d65c18e",
            new ValueDTO("1141000195107", "Secret", "url"), null);
    assertThat(result.getRelatedId()).isEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(result.getVerificationStatus().getCode()).isEqualTo(FhirConstants.ENTERED_IN_ERROR);
    assertThat(result.isDeleted()).isTrue();
    assertThat(result.getConfidentiality().getCode()).isEqualTo("1141000195107");
    assertThat(result.getConfidentiality().getName()).isEqualTo("Secret");
    assertThat(result.getConfidentiality().getSystem()).isEqualTo("url");

    allergies = allergyService.getAll(patientIdentifier, null, true);

    assertThat(allergies.size()).isEqualTo(0);

    // delete the resource again
    allergyService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", "00476f5f-f3b7-4e49-9b52-5ec88d65c18e",
        new ValueDTO("1141000195107", "Secret", "url"), null);

    // check that no deletion file was created after the second deletion
    verify(huskyAdapter, atMostOnce()).writeDocument(any(), anyString(), anyString(), any(), any());
  }

  @Override
  @Test
  public void testGetAll() {
    List<AllergyDTO> allergyDTOs =
        allergyService.getAll(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);
    assertThat(allergyDTOs.size()).isEqualTo(1);
    assertThat(allergyDTOs.get(0).getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(allergyDTOs.get(0).getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(allergyDTOs.get(0).getId()).isEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(allergyDTOs.get(0).getCode().getCode()).isEqualTo("293109003");
    assertThat(allergyDTOs.get(0).getJson()).contains("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
  }

  @Test
  public void testGetAll_GAZELLE() {
    List<AllergyDTO> allergyDTOs = allergyService.getAll(EPDCommunity.GAZELLE.name(), "1.3.6.1.4.1.21367.13.20.3000",
            "IHEBLUE-2599", null);
    assertThat(allergyDTOs.size()).isEqualTo(1);
    assertThat(allergyDTOs.get(0).getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(allergyDTOs.get(0).getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(allergyDTOs.get(0).getId()).isEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(allergyDTOs.get(0).getCode().getCode()).isEqualTo("293109003");
  }

  @Override
  @Test
  public void testUpdate() {

    PatientIdentifier patientIdentifier =
        allergyService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");
    List<AllergyDTO> allergies = allergyService.getAll(patientIdentifier, null, true);
    assertThat(allergies.size()).isEqualTo(1);

    HumanNameDTO recorder = new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null);

    ValueDTO newAllergyCode = new ValueDTO("newCode", "newName", "testsystem");
    ValueDTO newCriticality = new ValueDTO("low", "lowName", "testsystem");
    ValueDTO newClinicalStatus = new ValueDTO("newClinicalStatus", "newClinicalStatus", "testsystem");
    ValueDTO newVerficationStatus = new ValueDTO("newVerficationStatus", "newVerficationStatus", "testsystem");
    ValueDTO newType = new ValueDTO(AllergyIntoleranceType.INTOLERANCE.toCode(),
        AllergyIntoleranceType.INTOLERANCE.getDisplay(), AllergyIntoleranceType.INTOLERANCE.getSystem());
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);
    AllergyDTO newAllergyDTO = new AllergyDTO(null, LocalDate.now(), newAllergyCode, newCriticality,
        newClinicalStatus, newVerficationStatus, newType, recorder, List.of(comment), "My organization AG");
    newAllergyDTO.setAuthor(author);

    AllergyDTO updatedAllergy =
        allergyService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "00476f5f-f3b7-4e49-9b52-5ec88d65c18e", newAllergyDTO, null);
    assertThat(updatedAllergy.getCode()).isEqualTo(newAllergyCode);
    assertThat(updatedAllergy.getCriticality().getCode()).isEqualTo(newCriticality.getCode());
    assertThat(updatedAllergy.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(updatedAllergy.getVerificationStatus()).isEqualTo(newVerficationStatus);
    assertThat(updatedAllergy.getType()).isEqualTo(newType);
    assertThat(updatedAllergy.getComments().size()).isEqualTo(1);
    assertThat(updatedAllergy.getComments().get(0).getAuthor()).isEqualTo(author.getUser().getFullName());
    assertThat(updatedAllergy.getComments().get(0).getText()).isEqualTo(commentText);
    assertThat(updatedAllergy.getComments().get(0).getDate()).isNotNull();

    allergies = allergyService.getAll(patientIdentifier, null, true);
    assertThat(allergies.size()).isEqualTo(1);
  }

  @Override
  @Test
  public void testValidate() {
    HumanNameDTO recorder = new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null);

    ValueDTO newAllergyCode = new ValueDTO("newCode", "newName", "testsystem");
    ValueDTO newCriticality = new ValueDTO("low", "lowName", "testsystem");
    ValueDTO newClinicalStatus = new ValueDTO("newClinicalStatus", "newClinicalStatus", "testsystem");
    ValueDTO newVerficationStatus = new ValueDTO("newVerficationStatus", "newVerficationStatus", "testsystem");
    ValueDTO newType = new ValueDTO(AllergyIntoleranceType.INTOLERANCE.toCode(),
        AllergyIntoleranceType.INTOLERANCE.getDisplay(), AllergyIntoleranceType.INTOLERANCE.getSystem());
    AllergyDTO newAllergyDTO = new AllergyDTO(null, LocalDate.now(), newAllergyCode, newCriticality,
        newClinicalStatus, newVerficationStatus, newType, recorder, List.of(), "My organization AG");

    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "waldspital-Id-1234", "1.2.3.4.123456.1"));
    AllergyDTO updatedAllergy =
        allergyService.validate(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "00476f5f-f3b7-4e49-9b52-5ec88d65c18e", newAllergyDTO, null);
    assertThat(updatedAllergy.getCode()).isEqualTo(newAllergyCode);
    assertThat(updatedAllergy.getCriticality().getCode()).isEqualTo(newCriticality.getCode());
    assertThat(updatedAllergy.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(updatedAllergy.getVerificationStatus()).isEqualTo(newVerficationStatus);
    assertThat(updatedAllergy.getType()).isEqualTo(newType);
  }

  // @Test
  void getData_emptyData_EPDPLAYGROUND() {
    assertThat(
        allergyService.getAll(EPDCommunity.EPDPLAYGROUND.name(),
            "1.2.3.4.123456.1",
            EPDCommunity.DUMMY.name(), null))
                .isEmpty();
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
    validate(new AllergyDTO(), allergyService, role, expectedExceptionMessage);
  }

}
