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

import ch.admin.bag.vaccination.service.AbstractServiceTest;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VaccinationControllerTest extends AbstractServiceTest {
  @LocalServerPort
  private int port;
  @Autowired
  private TestRestTemplate restTemplate;

  @Override
  @Test
  public void testCreate() {
    HumanNameDTO performer = new HumanNameDTO("Victor", "Frankenstein2", "Dr.", null, null);

    ValueDTO vaccineCode = new ValueDTO("123456789", "123456789", "testsystem");
    ValueDTO status = new ValueDTO("completed", "completed", "testsystem");
    ValueDTO reason = new ValueDTO("codeReason", "nameReason", "systemReason");
    VaccinationDTO vaccinationDTO =
        new VaccinationDTO(null, vaccineCode, null,
            null, 3,
            LocalDate.now(), performer, null, "lotNumber", reason, status);
    List<CommentDTO> comments = new ArrayList<>();
    comments.add(createComment());
    vaccinationDTO.setComments(comments);
    AuthorDTO author = new AuthorDTO(comments.get(0).getAuthor(), "HCP", null);
    HttpEntity<VaccinationDTO> request =
        new HttpEntity<>(vaccinationDTO, getHttpHeaders(author));

    ResponseEntity<VaccinationDTO> response = restTemplate.postForEntity(
        "http://localhost:" + port
            + "/vaccination/communityIdentifier/GAZELLE/oid/1.3.6.1.4.1.21367.13.20.3000/localId/CHPAM4489",
        request,
        VaccinationDTO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    VaccinationDTO vaccination = response.getBody();

    assertThat(vaccination.getCode().getCode()).contains("123456789");
    assertThat(vaccination.getRecorder().getFullName()).contains("Frankenstein2");
    assertThat(vaccination.getReason().getCode()).contains("codeReason");
    assertThat(vaccination.getComments().size()).isEqualTo(1);
    assertThat(vaccination.getComments().get(0).getAuthor().getFullName())
        .isEqualTo(comments.get(0).getAuthor().getFullName());
    assertThat(vaccination.getAuthor().getUser().getFullName())
        .isEqualTo(comments.get(0).getAuthor().getFullName());
    assertThat(vaccination.getComments().get(0).getText()).isEqualTo(comments.get(0).getText());
  }

  @Override
  @Test
  public void testDelete() {
    AuthorDTO author = new AuthorDTO(new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null), "HCP", null);

    HttpEntity<ValueDTO> request =
        new HttpEntity<>(new ValueDTO("1141000195107", "Secret", "url"), getHttpHeaders(author));
    ResponseEntity<VaccinationDTO> response = restTemplate.exchange("http://localhost:" + port
        + "/vaccination/communityIdentifier/EPDPLAYGROUND/oid/1.2.3.4.123456.1/localId/waldspital-Id-1234/uuid/acc1f090-5e0c-45ae-b283-521d57c3aa2f",
        HttpMethod.DELETE, request, VaccinationDTO.class);

    assertThat(response.getBody().isDeleted()).isTrue();
    assertThat(response.getBody().getRelatedId()).isEqualTo("acc1f090-5e0c-45ae-b283-521d57c3aa2f");
    assertThat(response.getBody().getConfidentiality().getCode()).isEqualTo("1141000195107");
    assertThat(response.getBody().getAuthor().getUser().getFullName()).isEqualTo(author.getUser().getFullName());
    assertThat(response.getBody().getRecorder().getFullName()).isEqualTo("Dr. med. Peter Müller"); // Former performer
  }

  @Override
  @Test
  public void testGetAll() {
    // TODO
  }

  @Override
  @Test
  public void testUpdate() {
    HumanNameDTO performer = new HumanNameDTO("Victor2", "Frankenstein2", "Dr.", null, null);
    ValueDTO vaccineCode = new ValueDTO("123456789", "123456789", "testsystem");
    ValueDTO status = new ValueDTO("completed", "completed", "testsystem");
    VaccinationDTO vaccinationDTO = new VaccinationDTO(null, vaccineCode, null, null, 3,
        LocalDate.now(), performer, "organization", "lotNumber", null, status);
    AuthorDTO author = new AuthorDTO(performer, "HCP", null);
    HttpEntity<VaccinationDTO> request = new HttpEntity<>(vaccinationDTO, getHttpHeaders(author));

    ResponseEntity<VaccinationDTO> response = restTemplate.postForEntity(
        "http://localhost:" + port
            + "/vaccination/communityIdentifier/EPDPLAYGROUND/oid/1.2.3.4.123456.1/localId/waldspital-Id-1234",
        request, VaccinationDTO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getCode().getCode()).isEqualTo("123456789");
    assertThat(response.getBody().getRecorder().getLastName()).isEqualTo("Frankenstein2");
    assertThat(response.getBody().getAuthor().getUser().getLastName()).isEqualTo("Frankenstein2");
  }

  @Override
  @Test
  public void testValidate() {
    // TODO
  }

  // @Test
  void test_getVaccinations_GAZELLE() throws Exception {
    // FIXME: Does not work anymore
    assertThat(restTemplate.getForObject("http://localhost:" + port
        + "/vaccination/communityIdentifier/GAZELLE/oid/1.3.6.1.4.1.21367.13.20.3000/localId/IHEBLUE-2599",
        String.class)).contains("<name>Gruppenpraxis CH, Dr. med. Allzeit Bereit</name>");
  }

  private CommentDTO createComment() {
    HumanNameDTO author = new HumanNameDTO();
    author.setFirstName("me");
    return new CommentDTO(null, author, "BlaBla");
  }

  private HttpHeaders getHttpHeaders(AuthorDTO author) {
    HttpHeaders headers = HttpHeadersUtils.getHttpHeaders(author);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return headers;
  }
}


