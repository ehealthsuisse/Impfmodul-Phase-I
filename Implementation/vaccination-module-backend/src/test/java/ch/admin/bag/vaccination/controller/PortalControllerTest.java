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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.service.SignatureService;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PortalControllerTest {

  private static final String ANY_CONTENT = "anyContent";

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @MockitoBean
  private SignatureService signatureService;

  @Test
  void validateQueryParams_noInput_returnFalse() {
    // Test with valid parameters
    Map<String, String> queryMap = createMap();
    assertWebServiceResult(queryMap, null, null, true);

    // Test with missing GLN (set to null) for HCP role
    assertWebServiceResult(queryMap, "ugln", null, false);

    // Test with missing organization (set to empty string) for HCP role
    assertWebServiceResult(queryMap, "organization", "", false);

    // Test with valid parameters and role set to ASS
    queryMap.put("role", "ASS"); // Set the role to ASS for this test
    queryMap.put("ugln", ANY_CONTENT);
    queryMap.put("principalid", ANY_CONTENT);
    queryMap.put("principalname", ANY_CONTENT);
    queryMap.put("organization", ANY_CONTENT);
    assertWebServiceResult(queryMap, null, null, true);

    // Test with missing principalId (set to null) for ASS role
    assertWebServiceResult(queryMap, "principalid", null, false);

    // Test with missing organization (set to null) for ASS role
    assertWebServiceResult(queryMap, "organization", null, false);
  }

  @Test
  void validateQueryString_anyContent_forwardToService() {
    sendContentToWebservice(ANY_CONTENT, false);

    verify(signatureService).validateQueryString(ANY_CONTENT);
  }

  private void assertWebServiceResult(Map<String, String> queryMap, String parameterName, String parameterValue,
      boolean expectedResult) {
    if (Objects.nonNull(parameterName)) {
      queryMap.put(parameterName, parameterValue);
    }

    boolean result = sendContentToWebservice(queryMap);
    if (expectedResult) {
      assertTrue(result);
    } else {
      assertFalse(result);
    }
  }

  private Map<String, String> createMap() {
    // Create a test input parameters map
    Map<String, String> params = new HashMap<>();
    params.put("idp", ANY_CONTENT);
    params.put("laaoid", ANY_CONTENT);
    params.put("lpid", ANY_CONTENT);
    params.put("lang", ANY_CONTENT);
    params.put("ugln", ANY_CONTENT);
    params.put("purpose", ANY_CONTENT);
    params.put("role", "HCP");
    params.put("timestamp", ANY_CONTENT);
    params.put("ufname", ANY_CONTENT);
    params.put("ugname", ANY_CONTENT);
    params.put("organization", ANY_CONTENT);

    return params;
  }

  private String createURL(String inEndpoint) {
    return "http://localhost:" + port + inEndpoint;
  }

  private boolean sendContentToWebservice(Object content) {
    return sendContentToWebservice(content, true);
  }

  private boolean sendContentToWebservice(Object content, boolean isValid) {
    String contentString = content.toString();
    String endpoint = createURL(PortalController.ENDPOINT_VALIDATE);
    if (content instanceof Map<?, ?> map) {
      contentString = "";
      for (Entry<?, ?> entry : map.entrySet()) {
        if (entry.getValue() != null) {
          contentString += "&" + entry.getKey() + "=" + entry.getValue();
        }
      }

      // remove first &
      contentString = contentString.substring(1);
    }

    when(signatureService.validateQueryString(anyString())).thenReturn(isValid);
    return restTemplate.postForObject(endpoint, contentString, boolean.class);
  }
}
