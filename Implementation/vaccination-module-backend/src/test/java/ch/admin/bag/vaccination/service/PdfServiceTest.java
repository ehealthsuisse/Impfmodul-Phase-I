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
import ch.fhir.epr.adapter.FhirAdapter;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 
 * Test {@link PdfService}
 *
 */
@SpringBootTest
@ActiveProfiles("test")
public class PdfServiceTest {

  @Autowired
  private PdfService pdfService;
  @Autowired
  private FhirAdapter fhirAdapter;

  @Test
  public void test_translation() throws Exception {
    assertThat(I18nKey.BIRTHDAY.getTranslation("en")).isEqualTo("Birthday");
    assertThat(I18nKey.BIRTHDAY.getTranslation("de")).isEqualTo("Geburtsdatum");
    assertThat(I18nKey.BIRTHDAY.getTranslation("fr")).isEqualTo("Date de naissance");
    assertThat(I18nKey.BIRTHDAY.getTranslation("it")).isEqualTo("");
    assertThat(I18nKey.BIRTHDAY.getTranslation("EnglishIfNotDefined")).isEqualTo("Birthday");
    assertThat(I18nKey.BIRTHDAY.getTranslation(null)).isEqualTo("Birthday");
  }

  @Test
  public void test_pdf_creation() throws Exception {
    List<AllergyDTO> allergyDTOs = new ArrayList<>();
    List<PastIllnessDTO> pastIllnessDTOs = new ArrayList<>();
    List<VaccinationDTO> vaccinationDTOs = new ArrayList<>();

    List<String> localJsons = fhirAdapter.getLocalEntities();
    for (String localJson : localJsons) {
      Bundle bundle = fhirAdapter.unmarshallFromString(localJson);
      allergyDTOs.addAll(fhirAdapter.getDTOs(AllergyDTO.class, bundle));
      pastIllnessDTOs.addAll(fhirAdapter.getDTOs(PastIllnessDTO.class, bundle));
      vaccinationDTOs.addAll(fhirAdapter.getDTOs(VaccinationDTO.class, bundle));
    }

    HumanNameDTO patient = new HumanNameDTO("Hans", "Mueller", "Herr", LocalDate.now(), "MALE");

    InputStream stream = pdfService.create(
        new VaccinationRecordDTO("de", patient, allergyDTOs, pastIllnessDTOs, vaccinationDTOs));
    generatePDF("vaccinationRecord_de.pdf", stream);

    stream =
        pdfService.create(
            new VaccinationRecordDTO("en", patient, allergyDTOs, pastIllnessDTOs, vaccinationDTOs));
    generatePDF("vaccinationRecord_en.pdf", stream);

    stream = pdfService.create(
        new VaccinationRecordDTO("fr", patient, allergyDTOs, pastIllnessDTOs, vaccinationDTOs));
    generatePDF("vaccinationRecord_fr.pdf", stream);
  }

  private void generatePDF(String filename, InputStream stream) throws Exception {
    File pdfFile = new File(filename);
    Files.copy(stream, pdfFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    IOUtils.closeQuietly(stream);
  }

}
