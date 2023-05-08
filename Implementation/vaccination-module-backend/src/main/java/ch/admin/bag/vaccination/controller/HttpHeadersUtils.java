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

import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

/**
 * Utility class to manipulate the HTTP-headers.
 *
 */
@Slf4j
public class HttpHeadersUtils {

  public static String getIdpIdentifier(HttpHeaders headers) {
    if (headers == null) {
      throw new TechnicalException("HttpHeaders have not been set.");
    }

    return getValue(headers, "idp");
  }

  protected static AuthorDTO getAuthor(HttpHeaders headers) {
    if (headers == null) {
      throw new TechnicalException("HttpHeaders have not been set.");
    }

    HumanNameDTO user = new HumanNameDTO();
    user.setFirstName(getValue(headers, "ugname"));
    user.setLastName(getValue(headers, "ufname"));
    user.setPrefix(getValue(headers, "utitle"));

    AuthorDTO author = new AuthorDTO(
        user,
        null,
        getValue(headers, "role"),
        getValue(headers, "purpose"),
        getValue(headers, "ugln"),
        getValue(headers, "principalId"),
        getValue(headers, "principalName"));

    log.info("getAuthor {}", author);
    return author;
  }

  /**
   * Used for testing purpose only.
   *
   * @param author {@link AuthorDTO}
   * @return filled headers according to specification.
   */
  protected static HttpHeaders getHttpHeaders(AuthorDTO author) {
    final HttpHeaders headers = new HttpHeaders();
    if (author == null) {
      log.warn("getHttpHeaders author is null");
      return headers;
    }

    headers.set("ugname", author.getUser().getFirstName());
    headers.set("ufname", author.getUser().getLastName());
    headers.set("utitle", author.getUser().getPrefix());
    headers.set("role", author.getRole());
    headers.set("purpose", author.getPurpose());
    headers.set("ugln", author.getGln());
    headers.set("principalId", author.getPrincipalId());
    headers.set("principalName", author.getPrincipalName());

    return headers;
  }

  private static String getValue(HttpHeaders headers, String key) {
    if (headers == null) {
      log.warn("getValue HttpHeaders null !!!");
      return null;
    }
    List<String> values = headers.get(key);
    if (values == null || values.isEmpty()) {
      log.warn("getValue value of key {}  null !!!", key);
      return null;
    }
    log.debug("getValue value of key {}  = {}", key, values.get(0));
    return values.get(0);
  }
}
