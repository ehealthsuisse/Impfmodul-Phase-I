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
package ch.admin.bag.vaccination.service.husky;

import ch.admin.bag.vaccination.service.husky.config.CommunityConfig;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import java.util.List;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;
import org.projecthusky.common.model.Code;
import org.projecthusky.common.model.Identificator;
import org.projecthusky.xua.saml2.Assertion;

public interface HuskyAdapterIfc {

  /**
   * Returns the list of DocmentEntries.
   *
   * @param patientIdentifier contains all the required patientInfo
   * @param formatCodes the expected format codes
   * @param documentType the expected documentType
   * @param assertion the assertion
   * @return the list of {@link DocumentEntry}
   */
  List<DocumentEntry> getDocumentEntries(PatientIdentifier patientIdentifier,
      List<Code> formatCodes, String documentType, Assertion assertion);

  /**
   * Get the {@link PatientIdentifier}
   *
   * @param communityIdentifier he identifier of the community
   * @param localAssigningAuthorityOid localAssigningAuthorityOid
   * @param localId The local Id of the patient (within the community)
   * @param assertion the assertion
   *
   * @return The PatientIdentifier
   */
  PatientIdentifier getPatientIdentifier(String communityIdentifier,
      String localAssigningAuthorityOid,
      String localId, Assertion assertion);

  /**
   * Returns the list of Documents.
   *
   * @param communityIdentifier the identifier of the community
   * @param documentEntries list of {@link DocumentEntry}
   *
   * @return the list of {@link RetrievedDocument}
   */
  List<RetrievedDocument> getRetrievedDocuments(String communityIdentifier,
      List<DocumentEntry> documentEntries);

  /**
   * Generates a Assertion though XUA Connector
   *
   * @param idpAssertion the idpAsserstion
   * @param spid spid identificator
   * @return
   * @throws Exception if any error
   */
  Assertion getXUserAssertion(Assertion idpAssertion, Identificator spid,
      CommunityConfig communityConfig) throws Exception;

  /**
   * Write a document in EPD
   *
   * @param patientIdentifier {@link PatientIdentifier}
   * @param uuid uuid of the document
   * @param json the document as json string
   * @param author the author of the document
   * @param confidentiality Confidentiality code
   * @param assertion The SAML Assertion, can be null
   *
   * @return the status of the write SUCCESS/FAILURE/PARTIAL_SUCCESS
   */
  String writeDocument(PatientIdentifier patientIdentifier, String uuid, String json,
      HumanNameDTO author, ValueDTO confidentiality, Assertion assertion);


}
