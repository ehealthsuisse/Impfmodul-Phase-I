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
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import java.util.List;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;
import org.projecthusky.common.model.Code;
import org.projecthusky.common.model.Identificator;
import org.projecthusky.xua.saml2.Assertion;

public interface HuskyAdapterIfc {

  /**
   * Returns the list of DocumentEntries.
   *
   * @param patientIdentifier contains all the required patientInfo
   * @param author Author of the transaction
   * @param assertion the assertion
   * @param useInternal flag to distinguish between internal and external repository, external
   *        repository is used for cross community communication.
   * @return the list of {@link DocumentEntry}
   */
  List<DocumentEntry> getDocumentEntries(PatientIdentifier patientIdentifier, AuthorDTO author,
      Assertion assertion, boolean useInternal);

  /**
   * Returns the list of DocumentEntries.
   *
   * @param patientIdentifier contains all the required patientInfo
   * @param formatCodes the expected format codes
   * @param documentType the expected documentType
   * @param author Author of the transaction
   * @param assertion the assertion
   * @param useInternal flag to distinguish between internal and external repository, external
   *        repository is used for cross community communication.
   * @return the list of {@link DocumentEntry}
   */
  List<DocumentEntry> getDocumentEntries(PatientIdentifier patientIdentifier,
      List<Code> formatCodes, String documentType, AuthorDTO author, Assertion assertion, boolean useInternal);

  /**
   * Get the {@link PatientIdentifier}
   *
   * @param communityIdentifier he identifier of the community
   * @param localAssigningAuthorityOid localAssigningAuthorityOid
   * @param localId The local Id of the patient (within the community)
   *
   * @return The PatientIdentifier
   */
  PatientIdentifier getPatientIdentifier(String communityIdentifier,
      String localAssigningAuthorityOid, String localId);

  /**
   * Returns the list of Documents.
   *
   * @param patientIdentifier {@link PatientIdentifier}
   * @param documentEntries list of {@link DocumentEntry}
   * @param author Author of the transaction
   * @param assertion the assertion
   * @param useInternal flag to distinguish between internal and external repository, external
   *        repository is used for cross community communication.
   *
   * @return the list of {@link RetrievedDocument}
   */
  List<RetrievedDocument> getRetrievedDocuments(PatientIdentifier patientIdentifier,
      List<DocumentEntry> documentEntries, AuthorDTO author, Assertion assertion, boolean useInternal);

  /**
   * Generates a Assertion though XUA Connector
   *
   * @param author Author of the transaction
   * @param idpAssertion the idpAsserstion
   * @param spid spid identificator
   * @param communityConfig {@link CommunityConfig}
   * @param uriToAccess uri for which this xua token is requested for
   * @return {@link Assertion}
   * @throws Exception if any error
   */
  Assertion getXUserAssertion(AuthorDTO author, Assertion idpAssertion, Identificator spid,
      CommunityConfig communityConfig, String uriToAccess) throws Exception;

  /**
   * Write a document in EPD
   *
   * @param patientIdentifier {@link PatientIdentifier}
   * @param uuid uuid of the document
   * @param json the document as json string
   * @param dto {@link BaseDTO} used for information like author or confidentiality
   * @param assertion The SAML Assertion, can be null
   *
   * @return the status of the write SUCCESS/FAILURE/PARTIAL_SUCCESS
   */
  String writeDocument(PatientIdentifier patientIdentifier, String uuid, String json,
      BaseDTO dto, Assertion assertion);

  /**
   * Write a document in EPD
   *
   * @param patientIdentifier {@link PatientIdentifier}
   * @param uuid uuid of the document
   * @param json the document as json string
   * @param dto {@link BaseDTO} used for information like author or confidentiality
   * @param assertion The SAML Assertion, can be null
   * @param isVaccinationRecord flag to indicate vaccination record which needs a different metadata
   *
   * @return the status of the write SUCCESS/FAILURE/PARTIAL_SUCCESS
   */
  String writeDocument(PatientIdentifier patientIdentifier, String uuid, String json,
      BaseDTO dto, Assertion assertion, boolean isVaccinationRecord);
}
