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
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.fhir.epr.adapter.FhirConverterIfc;
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
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AllergyServiceTest {
  @Autowired
  private AllergyService allergyService;
  @Autowired
  private ProfileConfig profileConfig;
  @Autowired
  private Cache cache;

  @BeforeEach
  void before() {
    profileConfig.setLocalMode(true);
    profileConfig.setHuskyLocalMode(null);
    cache.clear();
  }

  @Test
  void createAllergy_EPDPLAYGROUND() {
    HumanNameDTO recorder = new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null);
    HumanNameDTO author = new HumanNameDTO("hor", "Aut", "Dr.", null, null, "HCP", "gln:1.2.3.4");

    ValueDTO allergyCode = new ValueDTO("123456789", "987654321", "testsystem");
    ValueDTO criticality = new ValueDTO("low", "lowName", "testsystem");
    ValueDTO clinicalStatus = new ValueDTO("clinicalStatus", "clinicalStatusName", "testsystem");
    ValueDTO verficationStatus = new ValueDTO("verficationStatus", "verficationStatusName", "testsystem");
    ValueDTO type = new ValueDTO(AllergyIntoleranceType.ALLERGY.toCode(),
        AllergyIntoleranceType.ALLERGY.getDisplay(), AllergyIntoleranceType.ALLERGY.getSystem());
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author, commentText);
    AllergyDTO newAllergyDTO = new AllergyDTO(null, LocalDate.now(), allergyCode, criticality,
        clinicalStatus, verficationStatus, type, recorder, List.of(comment), "My organization AG");
    newAllergyDTO.setAuthor(author);

    AllergyDTO result =
        allergyService.create(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", newAllergyDTO, null);
    assertThat(result.getAllergyCode().getCode()).isEqualTo(allergyCode.getCode());
    assertThat(result.getCriticality().getCode()).isEqualTo(criticality.getCode());
    assertThat(result.getClinicalStatus()).isEqualTo(clinicalStatus);
    assertThat(result.getVerificationStatus()).isEqualTo(verficationStatus);
    assertThat(result.getType()).isEqualTo(type);
    assertThat(result.getComments().get(0).getAuthor().getFullName()).isEqualTo(author.getFullName());
    assertThat(result.getComments().get(0).getText()).isEqualTo(commentText);
    assertThat(result.getComments().get(0).getDate()).isNotNull();
  }

  @Test
  void deleteAllergy_EPDPLAYGROUND() {
    PatientIdentifier patientIdentifier =
        allergyService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);

    List<AllergyDTO> allergies = allergyService.getAll(patientIdentifier, null, true);
    assertThat(allergies.size()).isEqualTo(1);

    AllergyDTO result =
        allergyService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "00476f5f-f3b7-4e49-9b52-5ec88d65c18e", null);
    assertThat(result.getRelatedId()).isEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(result.getVerificationStatus().getCode()).isEqualTo(FhirConverterIfc.ENTERED_IN_ERROR);
    assertThat(result.isDeleted()).isTrue();

    allergies = allergyService.getAll(patientIdentifier, null, true);

    assertThat(allergies.size()).isEqualTo(0);
  }

  @Test
  void getAllergies_existingData_EPDPLAYGROUND() {
    List<AllergyDTO> allergyDTOs =
        allergyService.getAll(EPDCommunity.EPDPLAYGROUND.name(),
            "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);
    assertThat(allergyDTOs.size()).isEqualTo(1);
    assertThat(allergyDTOs.get(0).getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(allergyDTOs.get(0).getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(allergyDTOs.get(0).getId()).isEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(allergyDTOs.get(0).getAllergyCode().getCode()).isEqualTo("294659004");
    assertThat(allergyDTOs.get(0).getJson()).contains("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
  }

  // @Test
  void getData_emptyData_EPDPLAYGROUND() {
    assertThat(
        allergyService.getAll(EPDCommunity.EPDPLAYGROUND.name(),
            "1.2.3.4.123456.1",
            EPDCommunity.DUMMY.name(), null))
                .isEmpty();
  }

  @Test
  void getData_existingData_GAZELLE() {
    profileConfig.setLocalMode(false);
    assertThat(
        allergyService.getAll(EPDCommunity.GAZELLE.name(),
            "1.3.6.1.4.1.21367.13.20.3000",
            EPDCommunity.DUMMY.name(), null))
                .isEmpty();
    profileConfig.setLocalMode(true);
    List<AllergyDTO> allergyDTOs =
        allergyService.getAll(EPDCommunity.GAZELLE.name(),
            "1.3.6.1.4.1.21367.13.20.3000",
            "IHEBLUE-2599", null);
    assertThat(allergyDTOs.size()).isEqualTo(1);
    assertThat(allergyDTOs.get(0).getClinicalStatus().getCode()).isEqualTo("active");
    assertThat(allergyDTOs.get(0).getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(allergyDTOs.get(0).getId()).isEqualTo("00476f5f-f3b7-4e49-9b52-5ec88d65c18e");
    assertThat(allergyDTOs.get(0).getAllergyCode().getCode()).isEqualTo("294659004");
  }

  @Test
  void updateAllergy_EPDPLAYGROUND() {

    PatientIdentifier patientIdentifier =
        allergyService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);
    List<AllergyDTO> allergies = allergyService.getAll(patientIdentifier, null, true);
    assertThat(allergies.size()).isEqualTo(1);

    HumanNameDTO recorder = new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null);
    HumanNameDTO author = new HumanNameDTO("hor", "Aut", "Dr.", null, null, "HCP", "gln:1.2.3.4");

    ValueDTO newAllergyCode = new ValueDTO("newCode", "newName", "testsystem");
    ValueDTO newCriticality = new ValueDTO("low", "lowName", "testsystem");
    ValueDTO newClinicalStatus = new ValueDTO("newClinicalStatus", "newClinicalStatus", "testsystem");
    ValueDTO newVerficationStatus = new ValueDTO("newVerficationStatus", "newVerficationStatus", "testsystem");
    ValueDTO newType = new ValueDTO(AllergyIntoleranceType.INTOLERANCE.toCode(),
        AllergyIntoleranceType.INTOLERANCE.getDisplay(), AllergyIntoleranceType.INTOLERANCE.getSystem());
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, author, commentText);
    AllergyDTO newAllergyDTO = new AllergyDTO(null, LocalDate.now(), newAllergyCode, newCriticality,
        newClinicalStatus, newVerficationStatus, newType, recorder, List.of(comment), "My organization AG");
    newAllergyDTO.setAuthor(author);

    AllergyDTO updatedAllergy =
        allergyService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "00476f5f-f3b7-4e49-9b52-5ec88d65c18e", newAllergyDTO, null);
    assertThat(updatedAllergy.getAllergyCode()).isEqualTo(newAllergyCode);
    assertThat(updatedAllergy.getCriticality().getCode()).isEqualTo(newCriticality.getCode());
    assertThat(updatedAllergy.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(updatedAllergy.getVerificationStatus()).isEqualTo(newVerficationStatus);
    assertThat(updatedAllergy.getType()).isEqualTo(newType);
    assertThat(updatedAllergy.getComments().size()).isEqualTo(1);
    assertThat(updatedAllergy.getComments().get(0).getAuthor().getFullName()).isEqualTo(author.getFullName());
    assertThat(updatedAllergy.getComments().get(0).getText()).isEqualTo(commentText);
    assertThat(updatedAllergy.getComments().get(0).getDate()).isNotNull();

    allergies = allergyService.getAll(patientIdentifier, null, true);
    assertThat(allergies.size()).isEqualTo(1);
  }

  @Test
  void validate() {
    try {
      allergyService.validate(null, null, null, null, null, null);
      assertThat(true).isFalse();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("validate not supported!");
    }
  }

}
