/**
 * Copyright (c) 2024 eHealth Suisse
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
package ch.admin.bag.vaccination.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.service.SignatureService;
import java.util.List;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Helper class to add mock data to a session object for testing purposes.
 *
 * This class provides a utility method for populating a {@code HttpSession} with PatientIdentifier data,
 * facilitating testing scenarios where specific session attributes are required.
 *
 */
public abstract class MockSessionHelper {
  public List<String> enhanceSessionWithMockedData(TestRestTemplate restTemplate, SignatureService signatureService,
      int port, boolean shouldRetrieveCookies) {
    String urlQuery = "idp=GAZELLE&laaoid=1.3.6.1.4.1.12559.11.20.1&lang=EN_us&lpid=CHPAM204"
        + "&organization=Gruppenpraxis+CH"
        + "&purpose=NORM&role=PAT&timestamp=1729858049673&ufname=Max&ugname=Mustermann&utitle=Dr.Med&ugln=GLN"
        + "&sig=VK6o9r9/Kx+Q5cAdsdLnoEi4lbQZspPOr0usMBhhvRc=";
    HttpEntity<String> request = new HttpEntity<>(urlQuery);
    when(signatureService.validateQueryString(urlQuery)).thenReturn(true);
    ResponseEntity<Boolean> portalResponse =
        restTemplate.postForEntity("http://localhost:" + port + "/signature/validate", request, Boolean.class);
    assertTrue(portalResponse != null && Boolean.TRUE.equals(portalResponse.getBody()));

    return shouldRetrieveCookies ? portalResponse.getHeaders().get(HttpHeaders.SET_COOKIE) : null;
  }
}
