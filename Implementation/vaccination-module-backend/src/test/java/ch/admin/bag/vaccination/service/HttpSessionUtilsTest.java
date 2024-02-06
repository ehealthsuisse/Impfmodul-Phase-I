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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootTest
@ActiveProfiles("test")
public class HttpSessionUtilsTest {

  @Mock
  private HttpServletRequest mockRequest;

  @Test
  void isValidAccessToPatientInformation_returnTrueOrFalseBasedOnValidIdentifier_trueForValidIdentifier() {
    // Create a mock PatientIdentifier
    PatientIdentifier validIdentifier = new PatientIdentifier(EPDCommunity.GAZELLE.name(), "localId", "oid");
    PatientIdentifier invalidIdentifier =
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), "otherLocalId", "otherOid");

    mockHttpServletRequest(validIdentifier);

    // Test with valid access
    boolean resultValidAccess = HttpSessionUtils.isValidAccessToPatientInformation(validIdentifier);
    assertTrue(resultValidAccess);

    // Test with invalid access
    boolean resultInvalidAccess = HttpSessionUtils.isValidAccessToPatientInformation(invalidIdentifier);
    assertFalse(resultInvalidAccess);
  }

  private void mockHttpServletRequest(PatientIdentifier validIdentifier) {
    HttpSession mockSession = mock(HttpSession.class);
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockRequest.getSession(false).getAttribute(HttpSessionUtils.CACHE_PATIENT_IDENTIFIER))
        .thenReturn(validIdentifier);

    // Mock the RequestContextHolder
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
  }
}
