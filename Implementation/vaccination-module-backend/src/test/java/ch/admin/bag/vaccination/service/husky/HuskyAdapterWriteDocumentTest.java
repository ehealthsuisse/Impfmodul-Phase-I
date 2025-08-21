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
package ch.admin.bag.vaccination.service.husky;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.fhir.epr.adapter.FhirConstants;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HuskyAdapterWriteDocumentTest {
  @Autowired
  private HuskyAdapter huskyAdapter;
  @Autowired
  private ProfileConfig profileConfig;

  @Test
  void test_writeDocument() throws Exception {
    profileConfig.setHuskyLocalMode(null);
    String json = "{\"fruit\": \"Apple\",\"size\": \"Large\",\"color\": \"Red\"}";
    String uuid = UUID.randomUUID().toString();
    VaccinationDTO vaccination = createVaccinationWithAuthor();

    PatientIdentifier patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.GAZELLE.name(), "1.2.3.4", "localId");
    String ret = huskyAdapter.writeDocument(patientIdentifier, uuid, json, vaccination, null);
    assertThat(ret).isEqualTo("SUCCESS");
  }

  @Test
  void test_writeFile() throws Exception {
    profileConfig.setHuskyLocalMode(true);
    String json = "{\"fruit\": \"Apple\",\"size\": \"Large\",\"color\": \"Red\"}";
    VaccinationDTO vaccination = createVaccinationWithAuthor();
    PatientIdentifier patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.GAZELLE.name(), "1.2.3.4", "localId");

    String ret = huskyAdapter.writeDocument(patientIdentifier, "testfile", json, vaccination, null);
    Files.delete(Path.of("config", "testfiles", "json", "testfile.json"));
    assertThat(ret).isEqualTo("SUCCESS");
  }

  private VaccinationDTO createVaccinationWithAuthor() {
    AuthorDTO authorDTO = new AuthorDTO(new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null));
    VaccinationDTO vaccination = new VaccinationDTO();
    vaccination.setAuthor(authorDTO);
    vaccination.setOccurrenceDate(LocalDate.now());
    vaccination.setConfidentiality(FhirConstants.DEFAULT_CONFIDENTIALITY_CODE);
    return vaccination;
  }

}
