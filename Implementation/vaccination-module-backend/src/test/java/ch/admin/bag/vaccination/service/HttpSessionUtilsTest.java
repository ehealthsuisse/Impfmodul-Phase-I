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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.admin.bag.vaccination.utils.HttpSessionUtils;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.exception.TechnicalException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootTest
@ActiveProfiles("test")
public class HttpSessionUtilsTest {

  private static final String SAML_ART = "artifact-123";
  private static final String FORWARD_ARTIFACT_TO_CLIENT_PATH = "/saml-acs";
  private static final String HTTPS_FRONTEND_HOST = "develop-vaccination-module.apps.ocp4.innershift.sodigital.io";
  private static final String HTTP_FRONTEND_HOST = "localhost:9000";
  private static final String IDP_IDENTIFIER = "GAZELLE";

  @AfterEach
  void resetHttpsActivated() {
    HttpSessionUtils.setHttpsActivated(true);
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void testValidPatientIdentifier_shouldReturnPatientIdentifierFromSession() {
    PatientIdentifier validIdentifier =
        new PatientIdentifier(EPDCommunity.GAZELLE.name(), "otherLocalId", "otherOid");
    mockHttpServletRequest(validIdentifier);

    PatientIdentifier patientIdentifierFromSession = HttpSessionUtils.getPatientIdentifierFromSession();

    assertEquals(EPDCommunity.GAZELLE.name(), patientIdentifierFromSession.getCommunityIdentifier());
    assertEquals("otherLocalId", patientIdentifierFromSession.getLocalExtension());
    assertEquals("otherOid", patientIdentifierFromSession.getLocalAssigningAuthority());
  }

  @Test
  void testGetNullPatientIdentifierFromSession_shouldThrowTechnicalException() {
    mockHttpServletRequest(null);
    assertThrows(TechnicalException.class, HttpSessionUtils::getPatientIdentifierFromSession);
  }

  @Test
  void getFrontendHostFromRelayState_shouldReturnFrontendHost() {
    String relayState = HTTPS_FRONTEND_HOST + ";;" + IDP_IDENTIFIER;
    String frontendHost = HttpSessionUtils.getFrontendHostFromRelayState(relayState);

    assertEquals(HTTPS_FRONTEND_HOST, frontendHost);
  }

  @Test
  void getIdpIdentifierFromRelayState_shouldReturnIdpIdentifier() {
    String relayState = HTTPS_FRONTEND_HOST + ";;" + IDP_IDENTIFIER;
    String idpIdentifier = HttpSessionUtils.getIdpIdentifierFromRelayState(relayState);

    assertEquals(IDP_IDENTIFIER, idpIdentifier);
  }

  @Test
  void getForwardArtifactLocationUrl_shouldCreateHttpsUrlUsingFrontendProvidedHost() {
    HttpSessionUtils.setHttpsActivated(true);
    String relayState = HTTPS_FRONTEND_HOST + ";;" + IDP_IDENTIFIER;
    String locationUrl = HttpSessionUtils.getForwardArtifactLocationUrl(relayState, FORWARD_ARTIFACT_TO_CLIENT_PATH,
        SAML_ART);

    assertEquals("https://" + HTTPS_FRONTEND_HOST + FORWARD_ARTIFACT_TO_CLIENT_PATH + "?SAMLart=" + SAML_ART +
        "&RelayState=" + IDP_IDENTIFIER, locationUrl);
  }

  @Test
  void getForwardArtifactLocationUrl_shouldCreateHttpUrlUsingFrontendProvidedHost() {
    HttpSessionUtils.setHttpsActivated(false);
    String relayState = HTTP_FRONTEND_HOST + ";;" + IDP_IDENTIFIER;
    String locationUrl = HttpSessionUtils.getForwardArtifactLocationUrl(relayState, FORWARD_ARTIFACT_TO_CLIENT_PATH,
        SAML_ART);

    assertEquals("http://" + HTTP_FRONTEND_HOST + FORWARD_ARTIFACT_TO_CLIENT_PATH + "?SAMLart=" + SAML_ART +
        "&RelayState=" + IDP_IDENTIFIER, locationUrl);
  }

  private void mockHttpServletRequest(PatientIdentifier validIdentifier) {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpSession mockSession = mock(HttpSession.class);
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockRequest.getSession(false).getAttribute(HttpSessionUtils.CACHE_PATIENT_IDENTIFIER))
        .thenReturn(validIdentifier);

    // Mock the RequestContextHolder
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
  }
}
