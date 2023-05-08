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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

public class HttpHeadersUtilsTest {

  private AuthorDTO globalAuthor = new AuthorDTO(new HumanNameDTO("John", "Doe", "Mr.", null, null), //
      "ORG AG", "ROLE", "PURPOSE", "GLN", "PRINCIPAL_ID", "PRINCIPAL_NAME");

  @Test
  public void testGetAutor() {
    HttpHeaders headers = HttpHeadersUtils.getHttpHeaders(globalAuthor);
    AuthorDTO author = HttpHeadersUtils.getAuthor(headers);

    assertThat(author.getUser().getFirstName()).isEqualTo("John");
    assertThat(author.getUser().getLastName()).isEqualTo("Doe");
    assertThat(author.getUser().getPrefix()).isEqualTo("Mr.");
    assertThat(author.getOrganisation()).isNull();
    assertThat(author.getRole()).isEqualTo("ROLE");
    assertThat(author.getPurpose()).isEqualTo("PURPOSE");
    assertThat(author.getGln()).isEqualTo("GLN");
    assertThat(author.getPrincipalId()).isEqualTo("PRINCIPAL_ID");
    assertThat(author.getPrincipalName()).isEqualTo("PRINCIPAL_NAME");
  }

  @Test
  public void testGetAutor_noHeader_throwException() {
    assertThrows(TechnicalException.class, () -> HttpHeadersUtils.getAuthor(null));
  }


  @Test
  public void testGetHttpHeaders() {
    assertThat(HttpHeadersUtils.getHttpHeaders(null)).isEqualTo(new HttpHeaders());

    HttpHeaders headers = HttpHeadersUtils.getHttpHeaders(globalAuthor);

    assertThat(headers.get("ugname").get(0)).isEqualTo("John");
    assertThat(headers.get("ufname").get(0)).isEqualTo("Doe");
    assertThat(headers.get("utitle").get(0)).isEqualTo("Mr.");

    assertThat(headers.get("organisation")).isNull();
    assertThat(headers.get("role").get(0)).isEqualTo("ROLE");
    assertThat(headers.get("purpose").get(0)).isEqualTo("PURPOSE");
    assertThat(headers.get("ugln").get(0)).isEqualTo("GLN");
    assertThat(headers.get("principalId").get(0)).isEqualTo("PRINCIPAL_ID");
    assertThat(headers.get("principalName").get(0)).isEqualTo("PRINCIPAL_NAME");
  }
}
