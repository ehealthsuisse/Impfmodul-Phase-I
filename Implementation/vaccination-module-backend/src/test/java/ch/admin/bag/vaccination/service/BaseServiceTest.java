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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.data.request.EPRDocument;
import ch.admin.bag.vaccination.service.husky.HuskyUtils;
import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.openehealth.ipf.commons.ihe.xds.core.requests.DocumentReference;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Base Service Test using allergy service as example
 */
@SpringBootTest
@ActiveProfiles("test")
class BaseServiceTest {

  @Autowired
  private AllergyService allergyService;
  @Autowired
  private ProfileConfig profileConfig;
  @Autowired
  private VaccinationService vaccinationService;
  @Autowired
  private BaseService<VaccinationDTO> baseService;
  @Mock
  private HttpServletRequest mockRequest;

  @BeforeEach
  void before() {
    profileConfig.setLocalMode(false);
    profileConfig.setHuskyLocalMode(null);
  }

  @Test
  void create_invalidPatientInformation_shouldThrowNullPointerException() {
    assertThrows(NullPointerException.class, () -> allergyService.getAll(null, null, null, null));
    assertThrows(NullPointerException.class, () -> allergyService.getAll("communityId", null, null, null));
    assertThrows(NullPointerException.class, () -> allergyService.getAll("communityId", "laaoid", null, null));
  }

  @Test
  void getPatientIdentifier_sameIDAsInSession_filledPatientInfo() {
    PatientIdentifier patientIdentifier = mockPatientIdentifier();

    HttpSession mockSession = mock(HttpSession.class);
    when(mockRequest.getSession()).thenReturn(mockSession);

    patientIdentifier = allergyService.getPatientIdentifier(patientIdentifier.getCommunityIdentifier(),
        patientIdentifier.getLocalAssigningAuthority(), patientIdentifier.getLocalExtenstion());

    assertNotNull(patientIdentifier);
    assertNotNull(patientIdentifier.getPatientInfo());
  }

  @Test
  void processAndValidateDocuments_documentsUnprocessed_isTrustedIsTrue() {
    RetrievedDocument newRetrieveDoc = new RetrievedDocument();
    newRetrieveDoc.setRequestData(new DocumentReference(null, "test", null));

    List<EPRDocument> eprDocuments = ReflectionTestUtils.invokeMethod(baseService, "processAndValidateDocuments",
        Collections.EMPTY_LIST, List.of(newRetrieveDoc));

    assertEquals(1, eprDocuments.size());
    assertTrue(eprDocuments.stream().anyMatch(EPRDocument::isTrusted));
  }

  @Test
  void readEPDdataAndValidateDocumentBasedOnRole() {
    PatientIdentifier patientIdentifier = mockPatientIdentifier();

    HttpSession mockSession = mock(HttpSession.class);
    when(mockRequest.getSession()).thenReturn(mockSession);

    patientIdentifier = vaccinationService.getPatientIdentifier(patientIdentifier.getCommunityIdentifier(),
        patientIdentifier.getLocalAssigningAuthority(), patientIdentifier.getLocalExtenstion());

    List<VaccinationDTO> vaccinations = vaccinationService.getAll(patientIdentifier, null, true);

    assertThat(vaccinations).isNotEmpty();
    vaccinations.forEach(vacc -> assertEquals(HuskyUtils.HCP.equals(vacc.getAuthor().getRole()), vacc.isValidated()));
  }

  private void mockHttpServletRequest(PatientIdentifier validIdentifier) {
    HttpSession mockSession = mock(HttpSession.class);
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockRequest.getSession(false).getAttribute(HttpSessionUtils.CACHE_PATIENT_IDENTIFIER))
    .thenReturn(validIdentifier);

    // Mock the RequestContextHolder
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
  }

  private PatientIdentifier mockPatientIdentifier() {
    String communityIdentifier = EPDCommunity.GAZELLE.name();
    String oid = "1.3.6.1.4.1.12559.11.20.1";
    String localId = "CHPAM9810";
    PatientIdentifier patientIdentifier = new PatientIdentifier(communityIdentifier, localId, oid);
    mockHttpServletRequest(patientIdentifier);

    return patientIdentifier;
  }
}
