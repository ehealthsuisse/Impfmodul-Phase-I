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
package ch.admin.bag.vaccination.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.admin.bag.vaccination.data.dto.VaccineToTargetDiseasesDTO;
import ch.admin.bag.vaccination.data.dto.ValueListDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UtilityControllerTest {

  @LocalServerPort
  private int port;

  @Value("${spring.application.version}")
  private String backendVersion;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void getAllValuesList_returnValueList() {
    ResponseEntity<ValueListDTO[]> response = restTemplate.getForEntity(
        createURL("/utility/getAllValuesLists"),
        ValueListDTO[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotEmpty();
    assertThat(response.getBody()).isInstanceOf(ValueListDTO[].class);

    ValueListDTO[] valueListDTOs = response.getBody();
    assertThat(valueListDTOs.length).isGreaterThan(16);
    boolean medicalProblemCodeFound = false;
    for (int i = 0; i < valueListDTOs.length; i++) {
      if ("medicalProblemCode".equals(valueListDTOs[i].getName())) {
        medicalProblemCodeFound = true;
        assertThat(valueListDTOs[i].getName()).isEqualTo("medicalProblemCode");
        assertThat(valueListDTOs[i].getEntries().size()).isGreaterThan(20);
        assertThat(valueListDTOs[i].getEntries().get(1).getCode()).isEqualTo("1237030002");
        assertThat(valueListDTOs[i].getEntries().get(1).getName())
            .isEqualTo("At increased risk of exposure to Bordetella pertussis");
        assertThat(valueListDTOs[i].getEntries().get(1).getSystem())
            .isEqualTo("http://snomed.info/sct");
      }
    }
    assertTrue(medicalProblemCodeFound);
  }

  @Test
  void getBackendVersion_returnsExpectedVersion() {
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/utility/backendVersion"),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(backendVersion);
  }

  @Test
  void getTargetDiseases() {
    ResponseEntity<ValueDTO[]> response = restTemplate.getForEntity(
        createURL("/utility/targetDiseases"),
        ValueDTO[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().length).isGreaterThan(25);
    assertThat(response.getBody()[0].getCode()).isEqualTo("56717001");
    assertThat(response.getBody()[0].getName()).isEqualTo("Tuberculosis");
    assertThat(response.getBody()[0].getSystem()).isEqualTo("http://snomed.info/sct");
  }

  @Test
  void getVaccinesToTargetDiseases_returnValueList() {
    ResponseEntity<VaccineToTargetDiseasesDTO[]> response = restTemplate.getForEntity(
        createURL("/utility/vaccinesToTargetDiseases"),
        VaccineToTargetDiseasesDTO[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotEmpty();
    assertThat(response.getBody()).isInstanceOf(VaccineToTargetDiseasesDTO[].class);
    assertThat(response.getBody().length).isGreaterThan(80);
    assertThat(response.getBody()[0].getVaccine().getCode()).isEqualTo("14");
    assertThat(response.getBody()[0].getVaccine().getName()).isEqualTo("MoRu-Viraten");
    assertThat(response.getBody()[0].getVaccine().getSystem())
        .isEqualTo("http://fhir.ch/ig/ch-vacd/CodeSystem/ch-vacd-myvaccines-cs");

    assertThat(response.getBody()[0].getTargetDiseases().size()).isEqualTo(2);
    assertThat(response.getBody()[0].getTargetDiseases().getFirst().getCode()).isEqualTo("36653000");
    assertThat(response.getBody()[0].getTargetDiseases().getFirst().getName())
        .isEqualTo("Rubella");
    assertThat(response.getBody()[0].getTargetDiseases().getFirst().getSystem())
        .isEqualTo("http://snomed.info/sct");
  }

  @Test
  void setLocalModeToFalse() {
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/utility/setLocalMode/false"),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("Local mode set to false");
  }

  @Test
  void setLocalModeToTrue() {
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/utility/setLocalMode/true"),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("Local mode set to true");
  }

  private String createURL(String endpoint) {
    return "http://localhost:" + port + endpoint;
  }
}
