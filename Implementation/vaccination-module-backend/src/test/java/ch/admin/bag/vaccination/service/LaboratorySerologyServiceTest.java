/**
 * Copyright (c) 2026 eHealth Suisse
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
import ch.fhir.epr.adapter.data.dto.LaboratorySerologyDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class LaboratorySerologyServiceTest extends AbstractServiceTest {
  @Autowired
  private LaboratorySerologyService laboratorySerologyService;
  @MockitoSpyBean
  private HuskyAdapter huskyAdapter;

  @Override
  @Test
  public void testCreate() {
    PatientIdentifier patientIdentifier =
        laboratorySerologyService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<LaboratorySerologyDTO> serologies = laboratorySerologyService.getAll(patientIdentifier, null, true);
    assertThat(serologies).isNotEmpty();

    LaboratorySerologyDTO newDto = serologies.getFirst();
    newDto.setId(null);
    newDto.setAuthor(author);

    LaboratorySerologyDTO result = laboratorySerologyService.create(EPDCommunity.EPDPLAYGROUND.name(),
        "1.2.3.4.123456.1", "waldspital-Id-1234", newDto, null, false);

    assertThat(result.getCode().getCode()).isEqualTo(newDto.getCode().getCode());
    assertThat(result.getConfidentiality().getCode()).isEqualTo(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getCode());
    assertThat(result.getConfidentiality().getName()).startsWith(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getName());
    assertThat(result.getConfidentiality().getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);
  }

  @Override
  @Test
  public void testDelete() {
    PatientIdentifier patientIdentifier =
        laboratorySerologyService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<LaboratorySerologyDTO> serologies = laboratorySerologyService.getAll(patientIdentifier, null, true);
    assertThat(serologies).isNotEmpty();

    LaboratorySerologyDTO toDelete = serologies.getFirst();
    LaboratorySerologyDTO result = laboratorySerologyService.delete(EPDCommunity.EPDPLAYGROUND.name(),
        "1.2.3.4.123456.1", "waldspital-Id-1234", toDelete.getId(),
        new ValueDTO("1141000195107", "Secret", "url"), null);

    assertThat(result.getRelatedId()).isEqualTo(toDelete.getId());
    assertThat(result.getStatus().getCode()).isEqualTo(FhirConstants.ENTERED_IN_ERROR);
    assertThat(result.isDeleted()).isTrue();
    assertThat(result.getConfidentiality().getCode()).isEqualTo("1141000195107");
    assertThat(result.getConfidentiality().getName()).isEqualTo("Secret");
    assertThat(result.getConfidentiality().getSystem()).isEqualTo("url");

    serologies = laboratorySerologyService.getAll(patientIdentifier, null, true);
    assertThat(serologies).isEmpty();

    laboratorySerologyService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", toDelete.getId(), new ValueDTO("1141000195107", "Secret", "url"), null);

    verify(huskyAdapter, atMostOnce()).writeDocument(any(), anyString(), anyString(), any(), any());
  }

  @Override
  @Test
  public void testGetAll() {
    List<LaboratorySerologyDTO> serologies = laboratorySerologyService.getAll(EPDCommunity.EPDPLAYGROUND.name(),
        "1.2.3.4.123456.1", "waldspital-Id-1234", null);

    assertThat(serologies).hasSize(1);
    LaboratorySerologyDTO dto = serologies.getFirst();
    assertThat(dto.getCode().getCode()).isEqualTo("16935-9");
    assertThat(dto.getVerificationStatus().getCode()).isEqualTo("59156000");
  }

  @Override
  @Test
  public void testUpdate() {
    PatientIdentifier patientIdentifier =
        laboratorySerologyService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<LaboratorySerologyDTO> serologies = laboratorySerologyService.getAll(patientIdentifier, null, true);
    assertThat(serologies).isNotEmpty();

    LaboratorySerologyDTO toUpdate = serologies.getFirst();
    ValueDTO newCode = new ValueDTO("8888", "New Lab Code", "http://loinc.org");
    toUpdate.setCode(newCode);
    toUpdate.setAuthor(author);

    LaboratorySerologyDTO updated = laboratorySerologyService.update(EPDCommunity.EPDPLAYGROUND.name(),
        "1.2.3.4.123456.1", "waldspital-Id-1234", toUpdate.getId(), toUpdate, null);

    assertThat(updated.getRelatedId()).isEqualTo(toUpdate.getId());
    assertThat(updated.getCode()).isEqualTo(newCode);
    assertThat(updated.getVerificationStatus().getCode()).isEqualTo("59156000");
  }

  @Override
  @Test
  public void testValidate() {
    PatientIdentifier patientIdentifier =
        laboratorySerologyService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");

    List<LaboratorySerologyDTO> serologies = laboratorySerologyService.getAll(patientIdentifier, null, true);
    assertThat(serologies).isNotEmpty();

    LaboratorySerologyDTO toValidate = serologies.getFirst();
    toValidate.setAuthor(author);

    LaboratorySerologyDTO validated = laboratorySerologyService.validate(EPDCommunity.EPDPLAYGROUND.name(),
        "1.2.3.4.123456.1", "waldspital-Id-1234", toValidate.getId(), toValidate, null);

    assertThat(validated.getRelatedId()).isEqualTo(toValidate.getId());
    assertThat(validated.getCode()).isEqualTo(toValidate.getCode());
    assertThat(validated.getVerificationStatus().getCode()).isEqualTo("59156000");
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
    validate(new LaboratorySerologyDTO(), laboratorySerologyService, role, expectedExceptionMessage);
  }
}
