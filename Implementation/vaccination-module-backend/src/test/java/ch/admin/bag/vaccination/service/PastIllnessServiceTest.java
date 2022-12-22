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
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PastIllnessServiceTest {
  @Autowired
  private PastIllnessService pastIllnessService;
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
  void createPastIllness_EPDPLAYGROUND() {
    HumanNameDTO recorder = new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null);
    HumanNameDTO author = new HumanNameDTO("hor", "Aut", "Dr.", null, null, "HCP", "gln:1.2.3.4");

    ValueDTO illnessCode = new ValueDTO("123456789", "987654321", "testsystem");
    ValueDTO clinicalStatus = new ValueDTO("clinicalStatus", "clinicalStatus", "testsystem");
    ValueDTO verficationStatus = new ValueDTO("verficationStatus", "verficationStatus", "testsystem");
    String commentText = "BlaBla";
    CommentDTO comment = new CommentDTO(null, recorder, commentText);
    PastIllnessDTO newPastIllnessDTO = new PastIllnessDTO(null, illnessCode, clinicalStatus, verficationStatus,
        LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2001, 12, 31), author, List.of(comment),
        "My organization AG");
    newPastIllnessDTO.setAuthor(author);

    PastIllnessDTO result = pastIllnessService.create(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", newPastIllnessDTO, null);
    assertThat(result.getIllnessCode().getCode()).isEqualTo(illnessCode.getCode());
    assertThat(result.getClinicalStatus()).isEqualTo(clinicalStatus);
    assertThat(result.getVerificationStatus()).isEqualTo(verficationStatus);
    assertThat(result.getBegin()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(result.getEnd()).isEqualTo(LocalDate.of(2001, 12, 31));
    assertThat(result.getComments().get(0).getAuthor().getFullName()).isEqualTo(author.getFullName());
    assertThat(result.getComments().get(0).getText()).isEqualTo(commentText);
    assertThat(result.getComments().get(0).getDate()).isNotNull();
  }

  @Test
  void deletePastIllness_EPDPLAYGROUND() {
    PatientIdentifier patientIdentifier =
        pastIllnessService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);

    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll(patientIdentifier, null, true);
    assertThat(pastIllnesses.size()).isEqualTo(1);

    PastIllnessDTO result = pastIllnessService.delete(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234", "5f727b7b-87ae-464f-85ac-1a45d23f0897", null);

    assertThat(result.getRelatedId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(result.getVerificationStatus().getCode()).isEqualTo(FhirConverterIfc.ENTERED_IN_ERROR);
    assertThat(result.isDeleted()).isTrue();

    pastIllnesses = pastIllnessService.getAll(patientIdentifier, null, true);
    assertThat(pastIllnesses.size()).isEqualTo(0);
  }

  // @Test
  void getData_emptyData_EPDPLAYGROUND() {
    assertThat(
        pastIllnessService.getAll(EPDCommunity.EPDPLAYGROUND.name(),
            "1.2.3.4.123456.1",
            EPDCommunity.DUMMY.name(), null))
                .isEmpty();
  }

  @Test
  void getData_existingData_GAZELLE() {
    assertThat(
        pastIllnessService.getAll(EPDCommunity.GAZELLE.name(),
            "1.3.6.1.4.1.21367.13.20.3000",
            EPDCommunity.DUMMY.name(), null))
                .isNotEmpty();
    List<PastIllnessDTO> pastIllnessDTOs =
        pastIllnessService.getAll(EPDCommunity.GAZELLE.name(),
            "1.3.6.1.4.1.21367.13.20.3000",
            "IHEBLUE-2599", null);
    assertThat(pastIllnessDTOs.size()).isEqualTo(1);
    assertThat(pastIllnessDTOs.get(0).getId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(pastIllnessDTOs.get(0).getIllnessCode().getCode()).isEqualTo("38907003");
    assertThat(pastIllnessDTOs.get(0).getClinicalStatus().getCode()).isEqualTo("resolved");
    assertThat(pastIllnessDTOs.get(0).getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(pastIllnessDTOs.get(0).getJson()).contains("5f727b7b-87ae-464f-85ac-1a45d23f0897");
  }

  @Test
  void getPastIlnesses_existingData_EPDPLAYGROUND() {
    List<PastIllnessDTO> pastIllnessDTOs =
        pastIllnessService.getAll(EPDCommunity.EPDPLAYGROUND.name(),
            "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);
    assertThat(pastIllnessDTOs.size()).isGreaterThan(0);
    assertThat(pastIllnessDTOs.get(0).getClinicalStatus().getCode()).isEqualTo("resolved");
    assertThat(pastIllnessDTOs.get(0).getVerificationStatus().getCode()).isEqualTo("confirmed");
    assertThat(pastIllnessDTOs.get(0).getId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(pastIllnessDTOs.get(0).getIllnessCode().getCode()).isEqualTo("38907003");
  }

  @Test
  void updatePastIllness_EPDPLAYGROUND() {
    PatientIdentifier patientIdentifier =
        pastIllnessService.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", null);

    List<PastIllnessDTO> pastIllnesses = pastIllnessService.getAll(patientIdentifier, null, true);
    assertThat(pastIllnesses.size()).isEqualTo(1);

    HumanNameDTO recorder = new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null);
    HumanNameDTO author = new HumanNameDTO("hor", "Aut", "Dr.", null, null, "HCP", "gln:1.2.3.4");

    ValueDTO newIllnessCode = new ValueDTO("newCode", "newCode", "testsystem");
    ValueDTO newClinicalStatus = new ValueDTO("newClinicalStatus", "newClinicalStatus", "testsystem");
    ValueDTO newVerficationStatus = new ValueDTO("newVerficationStatus", "newVerficationStatus", "testsystem");
    String commentText = "BlaBla";
    CommentDTO knownComment = new CommentDTO(LocalDateTime.now().minusDays(1), recorder, "test");
    CommentDTO comment = new CommentDTO(null, author, commentText);

    PastIllnessDTO newPastIllnessDTO = new PastIllnessDTO(null, newIllnessCode, newClinicalStatus, newVerficationStatus,
        LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2001, 12, 31), recorder, List.of(knownComment, comment),
        "My new organization AG");
    newPastIllnessDTO.setAuthor(author);

    PastIllnessDTO updatedPastIllnessDTO =
        pastIllnessService.update(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234", "5f727b7b-87ae-464f-85ac-1a45d23f0897", newPastIllnessDTO, null);
    assertThat(updatedPastIllnessDTO.getRelatedId()).isEqualTo("5f727b7b-87ae-464f-85ac-1a45d23f0897");
    assertThat(updatedPastIllnessDTO.getIllnessCode()).isEqualTo(newIllnessCode);
    assertThat(updatedPastIllnessDTO.getClinicalStatus()).isEqualTo(newClinicalStatus);
    assertThat(updatedPastIllnessDTO.getVerificationStatus()).isEqualTo(newVerficationStatus);
    assertThat(updatedPastIllnessDTO.getOrganization()).isEqualTo("My new organization AG");
    assertThat(updatedPastIllnessDTO.getBegin()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(updatedPastIllnessDTO.getEnd()).isEqualTo(LocalDate.of(2001, 12, 31));
    assertThat(updatedPastIllnessDTO.getComments().get(0).getAuthor().getFullName()).isEqualTo(author.getFullName());
    assertThat(updatedPastIllnessDTO.getComments().get(0).getText()).isEqualTo(commentText);
    assertThat(updatedPastIllnessDTO.getComments().get(0).getDate()).isNotNull();

    pastIllnesses = pastIllnessService.getAll(patientIdentifier, null, true);
    assertThat(pastIllnesses.size()).isEqualTo(1);
  }

  @Test
  void validate() {
    try {
      pastIllnessService.validate(null, null, null, null, null, null);
      assertThat(true).isFalse();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("validate not supported!");
    }
  }
}
