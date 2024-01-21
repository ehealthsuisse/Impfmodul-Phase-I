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

import ch.admin.bag.vaccination.service.husky.HuskyUtils;
import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.fhir.epr.adapter.FhirConstants;
import ch.fhir.epr.adapter.FhirConverterIfc;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class VaccinationServiceTest extends AbstractServiceTest {
  @Autowired
  private VaccinationService vaccinationService;
  @Autowired
  private VaccinationConfig vaccinationConfig;

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
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);
    VaccinationDTO newVaccinationDTO = new VaccinationDTO(null, vaccineCode, null, List.of(comment), 3,
        LocalDate.now(), performer, null, "lotNumber", null, status);
    newVaccinationDTO.setAuthor(author);

    VaccinationDTO result = vaccinationService.create(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", newVaccinationDTO, null);
    assertThat(result.getCode().getCode()).isEqualTo(vaccineCode.getCode());
    assertThat(result.getStatus()).isEqualTo(status);
    assertThat(result.getRecorder().getFullName()).isEqualTo(performer.getFullName());
    assertThat(result.getLotNumber()).isEqualTo("lotNumber");
    assertThat(result.isValidated()).isTrue();
    assertThat(result.getConfidentiality().getCode()).isEqualTo(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getCode());
    assertThat(result.getConfidentiality().getName()).isEqualTo(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getName());
    assertThat(result.getConfidentiality().getSystem()).isEqualTo(FhirConstants.SNOMED_SYSTEM_URL);
    assertThat(result.getComments().get(0).getAuthor()).isEqualTo(author.getUser().getFullName());
    assertThat(result.getComments().get(0).getText()).isEqualTo(commentText);
    assertThat(result.getComments().get(0).getDate()).isNotNull();

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
    assertThat(result.getStatus().getCode()).isEqualTo(FhirConverterIfc.ENTERED_IN_ERROR);
    assertThat(result.isDeleted()).isTrue();
    assertThat(result.getConfidentiality().getCode()).isEqualTo("1141000195107");
    assertThat(result.getConfidentiality().getName()).isEqualTo("Secret");
    assertThat(result.getConfidentiality().getSystem()).isEqualTo("url");

    vaccinations = vaccinationService.getAll(patientIdentifier, null, true);

    assertThat(vaccinations.size()).isEqualTo(2);
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
    assertThat(vaccinationDTOs.get(2).getTargetDiseases().get(0).getCode()).isEqualTo("40468003");
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
    String newOrga = "My new organization AG";
    String newLotNumber = "newLotNumber";
    String commentText = "BlaBla";
    CommentDTO knownComment = new CommentDTO(LocalDateTime.now(), performer.getFullName(), "test");
    CommentDTO comment = new CommentDTO(null, author.getUser().getFullName(), commentText);
    VaccinationDTO newVaccinationDTO = new VaccinationDTO(null, vaccineCode, null, List.of(comment, knownComment), 3,
        LocalDate.now(), performer, newOrga, newLotNumber, null, newStatus);
    newVaccinationDTO.setAuthor(author);

    VaccinationDTO updatedVaccinationDTO =
        vaccinationService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "acc1f090-5e0c-45ae-b283-521d57c3aa2f", newVaccinationDTO, null);
    assertThat(updatedVaccinationDTO.getRelatedId()).isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    assertThat(updatedVaccinationDTO.getCode()).isEqualTo(vaccineCode);
    assertThat(updatedVaccinationDTO.getStatus()).isEqualTo(newStatus);
    assertThat(updatedVaccinationDTO.getLotNumber()).isEqualTo(newLotNumber);
    assertThat(updatedVaccinationDTO.getOrganization()).isEqualTo("My new organization AG");
    assertThat(updatedVaccinationDTO.getComments().get(0).getAuthor())
        .isEqualTo(author.getUser().getFullName());
    assertThat(updatedVaccinationDTO.getComments().get(0).getText()).isEqualTo(commentText);
    assertThat(updatedVaccinationDTO.getComments().get(0).getDate()).isNotNull();

    vaccinations = vaccinationService.getAll(patientIdentifier, null, true);
    assertThat(vaccinations.size()).isEqualTo(3);
  }

  @Override
  @Test
  public void testValidate() {
    ValueDTO vaccineCode = new ValueDTO("987654321", "123456789", "testsystem");
    ValueDTO newStatus = new ValueDTO("entered-in-error", "entered-in-error", "http://hl7.org/fhir/event-status");
    String newOrga = "My new organization AG";
    String newLotNumber = "newLotNumber";
    VaccinationDTO newVaccinationDTO = new VaccinationDTO(null, vaccineCode, null, null, 3,
        LocalDate.now(), performer, newOrga, newLotNumber, null, newStatus);
    newVaccinationDTO.setAuthor(author);

    VaccinationDTO updatedVaccinationDTO =
        vaccinationService.validate(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "acc1f090-5e0c-45ae-b283-521d57c3aa2f", newVaccinationDTO, null);
    assertThat(updatedVaccinationDTO.getRelatedId()).isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    assertThat(updatedVaccinationDTO.getCode()).isEqualTo(vaccineCode);
    assertThat(updatedVaccinationDTO.getStatus()).isEqualTo(newStatus);
    assertThat(updatedVaccinationDTO.getLotNumber()).isEqualTo(newLotNumber);
    assertThat(updatedVaccinationDTO.getOrganization()).isEqualTo("My new organization AG");
  }

  @Test
  void getData_emptyData_EPDPLAYGROUND() {
    profileConfig.setLocalMode(false);
    assertThat(vaccinationService.getAll(EPDCommunity.EPDPLAYGROUND.name(),
        "1.2.3.4.123456.1", EPDCommunity.DUMMY.name(), null)).isEmpty();
  }

  // @Test
  void getData_existingData_GAZELLE() {
    profileConfig.setLocalMode(false);
    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), EPDCommunity.DUMMY.name(), "1.3.6.1.4.1.21367.13.20.3000"));
    assertThat(vaccinationService.getAll(EPDCommunity.GAZELLE.name(),
        "1.3.6.1.4.1.21367.13.20.3000", EPDCommunity.DUMMY.name(), null)).isEmpty();

    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), "IHEBLUE-2599", "1.3.6.1.4.1.21367.13.20.3000"));
    List<VaccinationDTO> vaccinationDTOs = vaccinationService.getAll(EPDCommunity.GAZELLE.name(),
        "1.3.6.1.4.1.21367.13.20.3000", "IHEBLUE-2599", null);
    assertThat(vaccinationDTOs.size()).isEqualTo(1);
    assertThat(vaccinationDTOs.get(0).getOrganization())
        .contains("<name>Gruppenpraxis CH, Dr. med. Allzeit Bereit</name>");
  }

  @BeforeEach
  void setUp() {
    super.before();

    setPatientIdentifierInSession(
        new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "waldspital-Id-1234", "1.2.3.4.123456.1"));
  }

  @Test
  void vaccinationConfig() {
    assertThat(vaccinationConfig.getFormatCodes().size()).isEqualTo(1);
    assertThat(vaccinationConfig.getFormatCodes().get(0).getCode())
        .isEqualTo("urn:che:epr:ch-vacd:immunization-administration:2022");
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


