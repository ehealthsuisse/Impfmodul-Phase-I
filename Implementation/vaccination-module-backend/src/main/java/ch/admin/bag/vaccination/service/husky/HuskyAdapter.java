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

import static ch.admin.bag.vaccination.service.husky.HuskyUtils.UPLOADER_ROLE_METADATA_KEY;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.VaccinationConfig;
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.admin.bag.vaccination.service.husky.config.CommunitiesConfig;
import ch.admin.bag.vaccination.service.husky.config.CommunityConfig;
import ch.admin.bag.vaccination.service.husky.config.EPDRepository;
import ch.admin.bag.vaccination.service.husky.config.RepositoryConfig;
import ch.admin.bag.vaccination.service.husky.config.SenderConfig;
import ch.admin.bag.vaccination.service.saml.SAMLUtils;
import ch.fhir.epr.adapter.FhirAdapter;
import ch.fhir.epr.adapter.FhirConstants;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.openehealth.ipf.commons.core.OidGenerator;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.opensaml.core.config.InitializationService;
import org.opensaml.soap.wstrust.Claims;
import org.projecthusky.common.basetypes.NameBaseType;
import org.projecthusky.common.communication.AffinityDomain;
import org.projecthusky.common.communication.AtnaConfig;
import org.projecthusky.common.communication.Destination;
import org.projecthusky.common.communication.DocumentMetadata;
import org.projecthusky.common.communication.SubmissionSetMetadata;
import org.projecthusky.common.enums.DocumentDescriptor;
import org.projecthusky.common.enums.EhcVersions;
import org.projecthusky.common.enums.LanguageCode;
import org.projecthusky.common.model.Author;
import org.projecthusky.common.model.Code;
import org.projecthusky.common.model.Identificator;
import org.projecthusky.common.model.Name;
import org.projecthusky.common.model.Patient;
import org.projecthusky.communication.ConvenienceCommunication;
import org.projecthusky.communication.ConvenienceMasterPatientIndexV3;
import org.projecthusky.communication.DocumentRequest;
import org.projecthusky.communication.MasterPatientIndexQuery;
import org.projecthusky.communication.MasterPatientIndexQueryResponse;
import org.projecthusky.communication.xd.storedquery.FindDocumentsQuery;
import org.projecthusky.xua.communication.clients.XuaClient;
import org.projecthusky.xua.communication.clients.impl.ClientFactory;
import org.projecthusky.xua.communication.config.XuaClientConfig;
import org.projecthusky.xua.communication.config.impl.XuaClientConfigBuilderImpl;
import org.projecthusky.xua.communication.xua.RequestType;
import org.projecthusky.xua.communication.xua.TokenType;
import org.projecthusky.xua.communication.xua.XUserAssertionResponse;
import org.projecthusky.xua.communication.xua.impl.AppliesToBuilderImpl;
import org.projecthusky.xua.communication.xua.impl.XUserAssertionRequestBuilderImpl;
import org.projecthusky.xua.communication.xua.impl.ch.XUserAssertionRequestBuilderChImpl;
import org.projecthusky.xua.hl7v3.CE;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

@Slf4j
@Service
public class HuskyAdapter implements HuskyAdapterIfc {

  private static final String GAZELLE = "GAZELLE";

  @Autowired
  private Cache cache;

  @Autowired
  private SenderConfig senderConfig;

  @Autowired
  private CommunitiesConfig communitiesConfig;

  @Autowired
  private VaccinationConfig vaccinationConfig;

  @Autowired
  private ConvenienceMasterPatientIndexV3 convenienceMasterPatientIndexV3Client;

  @Autowired
  private ConvenienceCommunication convenienceCommunication;

  @Autowired
  private ProfileConfig profileConfig;

  // init once only
  private XuaClient xuaClient;

  @Override
  public List<DocumentEntry> getDocumentEntries(PatientIdentifier patientIdentifier, AuthorDTO author,
      Assertion assertion, boolean useInternal) {
    return getDocumentEntries(patientIdentifier, vaccinationConfig.getFormatCodes(),
        vaccinationConfig.getDocumentType(), author, assertion, useInternal);
  }

  @Override
  public List<DocumentEntry> getDocumentEntries(PatientIdentifier patientIdentifier,
      List<Code> formatCodes, String documentType, AuthorDTO author, Assertion assertion, boolean useInternal) {
    log.debug("Load {} document entries for {}", useInternal ? "local" : "cross-community", patientIdentifier);
    var communityConfig = getCommunityConfig(patientIdentifier.getCommunityIdentifier());
    var repositoryConfig = communityConfig.getRepositoryConfig(
        useInternal ? EPDRepository.InternalRegistryStoredQuery : EPDRepository.ExternalRegistryStoredQuery);

    if (repositoryConfig.getUri() == null) {
      log.debug("No URL was specified for cross community repository.");
      return Collections.emptyList();
    }

    return getDocumentEntries(communityConfig, repositoryConfig, patientIdentifier,
        formatCodes, documentType, author, assertion);
  }

  @Override
  public PatientIdentifier getPatientIdentifier(String communityIdentifier, String oid, String localId) {
    checkPatientParameter(communityIdentifier, oid, localId);
    PatientIdentifier patientIdentifier = cache.getPatientIdentifier(oid, localId);

    if (patientIdentifier != null) {
      return patientIdentifier;
    }

    if (Boolean.FALSE.equals(profileConfig.getHuskyLocalMode())) {
      patientIdentifier = getPatientIdentifierFromEPD(communityIdentifier, oid, localId);
    } else {
      patientIdentifier = createDummyPatientIdentifier(communityIdentifier, oid, localId);
    }

    cache.putPatientIdentifier(patientIdentifier);

    return patientIdentifier;
  }

  @Override
  public List<RetrievedDocument> getRetrievedDocuments(PatientIdentifier patientIdentifier,
      List<DocumentEntry> documentEntries, AuthorDTO author, Assertion assertion, boolean useInternal) {
    log.debug("Retrieve documents for {}", patientIdentifier);
    if (documentEntries.isEmpty()) {
      log.debug("No metadata entries were provided.");
      return Collections.emptyList();
    }

    var communityConfig = getCommunityConfig(patientIdentifier.getCommunityIdentifier());
    var repositoryConfig = communityConfig.getRepositoryConfig(
        useInternal ? EPDRepository.InternalRetrieveDocumentSet : EPDRepository.ExternalRetrieveDocumentSet);

    if (repositoryConfig.getUri() == null) {
      log.debug("No URL was specified for cross community repository.");
      return Collections.emptyList();
    }

    return getRetrievedDocuments(patientIdentifier, communityConfig, repositoryConfig, documentEntries, author,
        assertion);
  }

  @Override
  public Assertion getXUserAssertion(AuthorDTO author, Assertion idpAssertion, Identificator spid,
      CommunityConfig communityConfig, String uriToAccess) throws Exception {
    if (idpAssertion == null) {
      log.warn("IdP assertion is missing. In production environment, this is a major issue.");
      if (!allowEmptyIdPAssertion(communityConfig)) {
        throw new TechnicalException(
            "Identity Provider Assertion is missing, stopping XUA request");
      }

      return null;
    }

    log.debug("Retrieving XUA Token");
    try {
      RepositoryConfig repository = communityConfig.getRepositoryConfig(EPDRepository.XUA);
      if (repository == null || repository.getXua() == null) {
        log.error("XUA for {} not supported", communityConfig.getIdentifier());
        throw new TechnicalException("XUA clientKeystore not configured. Check husky.yml configuration.");
      }

      XuaClient client = initXuaClient(repository);

      // define the attributes for the X-User Assertion request
      CE role = HuskyUtils.getCodedRole(author.getRole());
      CE purposeOfUse = HuskyUtils.getCodedPurposeOfUse(author.getPurpose());

      String spidEprNamespace =
          communityConfig.getSpidEprNamespace() != null ? communityConfig.getSpidEprNamespace() : "";
      String resourceId = spid.getExtension() + "^^^" + spidEprNamespace + "&" + spid.getRoot() + "&ISO";

      // build the X-User Assertion request
      XUserAssertionRequestBuilderChImpl chImpl = new XUserAssertionRequestBuilderChImpl();
      chImpl.principal(author.getPrincipalId(), author.getPrincipalName());
      overwriteDialect(chImpl);

      var assertionRequest = chImpl
          .requestType(RequestType.WST_ISSUE)
          .tokenType(TokenType.OASIS_WSS_SAML_PROFILE_11_SAMLV20)
          .purposeOfUse(purposeOfUse)
          .appliesTo(new AppliesToBuilderImpl().address(uriToAccess).create())
          .subjectRole(role)
          .resourceId(resourceId)
          .create();

      // query the X-User Assertion
      List<XUserAssertionResponse> response = client.send(idpAssertion, assertionRequest);
      return SAMLUtils.getXuaAssertionFromResponse(response);
    } catch (Exception ex) {
      log.error("Error retrieving XUA token.\nError message: {}", ex);
      throw ex;
    }
  }

  @PostConstruct
  public void init() throws Exception {
    InitializationService.initialize();
    log.debug("initialize done...");
  }

  @Override
  public String writeDocument(PatientIdentifier patientIdentifier, String uuid, String json,
      BaseDTO dto, Assertion assertion) {
    return writeDocument(patientIdentifier, uuid, json, dto, assertion, false);
  }

  @Override
  public String writeDocument(PatientIdentifier patientIdentifier, String uuid, String json,
      BaseDTO dto, Assertion assertion, boolean isVaccinationRecord) {
    log.debug("Writing new document for {}", patientIdentifier);
    try {
      var communityConfig = getCommunityConfig(patientIdentifier.getCommunityIdentifier());

      Identificator globalId = new Identificator(patientIdentifier.getGlobalAuthority(),
          patientIdentifier.getGlobalExtension());
      Identificator spid = new Identificator(patientIdentifier.getSpidRootAuthority(),
          patientIdentifier.getSpidExtension());
      Identificator localIdentifier =
          new Identificator(patientIdentifier.getLocalAssigningAuthority(),
              patientIdentifier.getLocalExtenstion());

      var repositoryConfig = communityConfig.getRepositoryConfig(EPDRepository.SubmitDocument);

      Destination dest = getDestination(repositoryConfig);
      AffinityDomain affinityDomain = getAffinityDomain(dest);
      convenienceCommunication.setAffinityDomain(affinityDomain);
      convenienceCommunication.clearDocuments();

      boolean success = false;
      String returnValue = "TEST";
      while (!success) {
        returnValue = writeConsideringConfidentiality(uuid, json, dto, assertion,
            isVaccinationRecord, communityConfig, globalId, spid, localIdentifier, repositoryConfig);
        success = Status.SUCCESS.name().equals(returnValue);
        if (!success) {
          increaseConfidentiality(dto);
        }
      }
      return returnValue;
    } catch (Exception e) {
      log.warn("Exception during write: {}", e.getMessage());
      throw new TechnicalException(e.getMessage());
    }

  }

  /**
   * Empty assertion is by default only allowed in test mode. For testing purposes, it can be
   * overwritten to allow empty assertions during exchange with Gazelle or EPDPlayground.
   *
   * @return true if an empty identity provider assertion is allowed.
   */
  protected boolean allowEmptyIdPAssertion(CommunityConfig communityConfig) {
    boolean isInTestMode = profileConfig.getHuskyLocalMode() == null ||
        GAZELLE.equals(Objects.nonNull(communityConfig) ? communityConfig.getIdentifier() : null);
    return profileConfig.isLocalMode() || isInTestMode;
  }

  protected List<RetrievedDocument> getRetrievedDocuments(PatientIdentifier patientIdentifier,
      CommunityConfig communityConfig, RepositoryConfig repositoryConfig, List<DocumentEntry> documentEntries,
      AuthorDTO author, Assertion assertion) {
    log.debug("getRetrievedDocuments {} {} {}", communityConfig, repositoryConfig, documentEntries);

    Destination dest = getDestination(repositoryConfig);
    AffinityDomain affinityDomain = getAffinityDomain(dest);
    convenienceCommunication.setAffinityDomain(affinityDomain);

    Identificator spid = new Identificator(patientIdentifier.getSpidRootAuthority(),
        patientIdentifier.getSpidExtension());

    List<DocumentRequest> documentRequestList = new ArrayList<>();
    documentEntries.forEach(documentEntry -> {
      var documentRequest = new DocumentRequest(
          documentEntry.getRepositoryUniqueId(),
          null,
          documentEntry.getUniqueId(),
          documentEntry.getHomeCommunityId());
      documentRequestList.add(documentRequest);
    });

    try {
      Assertion xuaAssertion = getXUserAssertion(author, assertion, spid, communityConfig, repositoryConfig.getUri());
      log.debug("Retrieve entries for following documents {}", documentRequestList);
      RetrievedDocumentSet response =
          convenienceCommunication.retrieveDocuments(documentRequestList.toArray(new DocumentRequest[0]), xuaAssertion,
              null);

      log.debug("Found {} documents.", response.getDocuments().size());
      return response.getDocuments();
    } catch (Exception ex) {
      log.warn("Error while retrieving document data: {}", ex.getMessage());
      return Arrays.asList();
    }
  }

  protected void setDocumentMetadata(DocumentMetadata metadata,
      Identificator localIdentifier, Identificator globalIdentifier, BaseDTO dto,
      boolean isVaccinationRecord) {
    addAuthorToMetadata(metadata, dto);
    metadata.setClassCode(HuskyUtils.PATIENT_RECORD_CLASS_CODE);
    Code confidentialityCode = toCode(HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE);
    if (dto.getConfidentiality() != null) {
      confidentialityCode = toCode(dto.getConfidentiality());
    }
    metadata.addConfidentialityCode(confidentialityCode);
    metadata.setFormatCode(isVaccinationRecord ? HuskyUtils.VACCINATION_RECORD_DOCUMENT_FORMAT_CODE
        : HuskyUtils.IMMUNIZATION_ADMINISTRATION_DOCUMENT_FORMAT_CODE);
    metadata.setHealthcareFacilityTypeCode(
        vaccinationConfig.getDoctor().getHealthCareFacilityTypeCode());
    metadata.setCodedLanguage(LanguageCode.ENGLISH_CODE);
    metadata.setMimeType("application/fhir+json");
    metadata.setPracticeSettingCode(vaccinationConfig.getDoctor().getPracticeSettingCode());
    metadata.setTypeCode(HuskyUtils.IMMUNIZATION_TYPE_CODE);
    metadata.setDestinationPatientId(globalIdentifier);
    metadata.setSourcePatientId(localIdentifier);

    setTitle(metadata, dto);
    // Add original provider role to indicate exclusively which role has uploaded the data.
    // This information cannot be changed later on in the EPR
    String code = dto.getAuthor().getRole() + "^^^&2.16.756.5.30.1.127.3.10.6&ISO";
    DocumentEntry xDoc = metadata.getXDoc();
    Map<String, List<String>> extraMetadata = new HashMap<>();
    List<String> values = List.of(code);
    extraMetadata.put(UPLOADER_ROLE_METADATA_KEY, values);
    xDoc.setExtraMetadata(extraMetadata);
  }

  void setSubmissionSetMetadata(SubmissionSetMetadata metadata, Identificator globalIdentifier, AuthorDTO authorDTO) {
    metadata.setContentTypeCode(HuskyUtils.DEFAULT_CONTENT_TYPE_CODE);
    Author author = toAuthor(authorDTO);
    metadata.setUniqueId(OidGenerator.uniqueOid().toString());
    metadata.setSourceId(EhcVersions.getCurrentVersion().getOid());
    metadata.setEntryUUID(FhirConstants.DEFAULT_ID_PREFIX + UUID.randomUUID());
    metadata.addAuthor(author);
    metadata.setDestinationPatientId(globalIdentifier);
  }

  private void addAuthorToMetadata(DocumentMetadata metadata, BaseDTO dto) {
    Author author = toAuthor(dto.getAuthor());
    metadata.addAuthor(author);
  }

  private void checkPatientParameter(String communityIdentifier, String oid, String localId) {
    Objects.requireNonNull(communityIdentifier, "Community identifier must not be null.");
    Objects.requireNonNull(oid, "Local assigning authority must not be null.");
    Objects.requireNonNull(localId, "Local patient id must not be null.");
  }

  private PatientIdentifier createDummyPatientIdentifier(String communityIdentifier, String oid, String localId) {
    PatientIdentifier dummy = new PatientIdentifier(communityIdentifier, localId, oid);
    dummy.setPatientInfo(
        new HumanNameDTO("Max", "Mustermann", null, LocalDate.of(1900, 1, 1), "MALE"));
    dummy.setGlobalAuthority("global authority");
    dummy.setGlobalExtension("global extension");
    dummy.setSpidExtension("spid extension");
    dummy.setSpidRootAuthority("urn:oid:1.2.3.4");

    return dummy;
  }

  private void fillPatientIdentifierByResult(PatientIdentifier patientIdentifier,
      final MasterPatientIndexQueryResponse response) {
    List<Identificator> resultIdentificators = response.getPatients().get(0).getIds();
    log.debug("Found patient with following IDs {}.", resultIdentificators);
    Identificator firstId = resultIdentificators.get(0);
    Identificator secondId = resultIdentificators.get(1);
    if (firstId.getRoot().equals(patientIdentifier.getGlobalAuthority())) {
      patientIdentifier.setGlobalExtension(firstId.getExtension());
      patientIdentifier.setSpidExtension(secondId.getExtension());
    } else {
      patientIdentifier.setGlobalExtension(secondId.getExtension());
      patientIdentifier.setSpidExtension(firstId.getExtension());
    }
  }

  private boolean filterAndLogDocumentType(String documentType, DocumentEntry entry) {
    boolean flag = entry.getTypeCode().getCode().equals(documentType);
    if (!flag) {
      log.debug("Filter metadata {}", entry.getEntryUuid());
    }

    return flag;
  }

  private AffinityDomain getAffinityDomain(Destination dest) {
    var affinityDomain = new AffinityDomain();
    affinityDomain.setRegistryDestination(dest);
    affinityDomain.setRepositoryDestination(dest);

    return affinityDomain;
  }

  private CommunityConfig getCommunityConfig(String communityIdentifier) {
    var communityConfig = communitiesConfig.getCommunityConfig(communityIdentifier);
    if (communityConfig == null) {
      log.warn("Community config for {} not found.", communityIdentifier);
      throw new TechnicalException("community.not.found");
    }

    return communityConfig;
  }

  private Destination getDestination(RepositoryConfig repositoryConfig) {
    var dest = new Destination();

    dest.setUri(URI.create(repositoryConfig.getUri()));
    dest.setSenderApplicationOid(senderConfig.getSender().getApplicationOid());
    dest.setSenderFacilityOid(senderConfig.getSender().getFacilityOid());
    dest.setReceiverApplicationOid(repositoryConfig.getReceiver().getApplicationOid());
    dest.setReceiverFacilityOid(repositoryConfig.getReceiver().getFacilityOid());

    return dest;
  }

  private List<DocumentEntry> getDocumentEntries(CommunityConfig communityConfig,
      RepositoryConfig repositoryConfig, PatientIdentifier patientIdentifier,
      List<Code> formatCodes, String documentType, AuthorDTO author, Assertion assertion) {
    Destination dest = getDestination(repositoryConfig);
    AffinityDomain affinityDomain = getAffinityDomain(dest);
    convenienceCommunication.setAffinityDomain(affinityDomain);

    Identificator globalId = new Identificator(patientIdentifier.getGlobalAuthority(),
        patientIdentifier.getGlobalExtension());
    Identificator spid = new Identificator(patientIdentifier.getSpidRootAuthority(),
        patientIdentifier.getSpidExtension());

    FindDocumentsQuery findDocumentsQuery = new FindDocumentsQuery(globalId, null, null,
        null, null, null, formatCodes, null,
        AvailabilityStatus.APPROVED);

    try {
      Assertion xUserAssertion = getXUserAssertion(author, assertion, spid, communityConfig, repositoryConfig.getUri());
      log.debug("Request approved document entries for identificator {} and format codes {}", globalId, formatCodes);
      QueryResponse response =
          convenienceCommunication.queryDocuments(findDocumentsQuery, xUserAssertion, null);

      List<DocumentEntry> result = response.getDocumentEntries();
      log.debug("Found metadata for {} documents.", result.size());
      if (!result.isEmpty() && documentType != null) {
        log.debug("Filtering results on documentType {}", documentType);
        result = result.stream().filter(entry -> filterAndLogDocumentType(documentType, entry))
            .collect(Collectors.toList());
        if (!result.isEmpty()) {
          log.debug("Found {} metadata with matching document type.", result.size());
          return result;
        }
        log.debug("No metadata matched the document type, return all metadata (for testing purposes).");
      }

      return response.getDocumentEntries();
    } catch (Exception ex) {
      log.warn("Error while retrieving document entries: {}", ex.getMessage());
      return Arrays.asList();
    }
  }

  private PatientIdentifier getPatientIdentifierFromEPD(String communityIdentifier,
      String localAssigningAuthorityOid, String localId) {
    var communityConfig = getCommunityConfig(communityIdentifier);
    var repositoryConfig = communityConfig.getRepositoryConfig(EPDRepository.PDQ);

    Destination dest = getDestination(repositoryConfig);

    AffinityDomain affinityDomain = new AffinityDomain();
    affinityDomain.setPdqDestination(dest);

    final MasterPatientIndexQuery query =
        new MasterPatientIndexQuery(affinityDomain.getPdqDestination());

    if (!localAssigningAuthorityOid.equals(communityConfig.getGlobalAssigningAuthorityOid()) &&
        !localAssigningAuthorityOid.equals(communityConfig.getSpidEprOid())) {
      query.addDomainToReturn(communityConfig.getGlobalAssigningAuthorityOid());
      query.addDomainToReturn(communityConfig.getSpidEprOid());
    }

    PatientIdentifier patientIdentifier =
        new PatientIdentifier(communityIdentifier, localId, localAssigningAuthorityOid);
    patientIdentifier.setGlobalAuthority(communityConfig.getGlobalAssigningAuthorityOid());
    patientIdentifier.setSpidRootAuthority(communityConfig.getSpidEprOid());

    final Identificator identificator = new Identificator(localAssigningAuthorityOid, localId);
    query.addPatientIdentificator(identificator);

    final MasterPatientIndexQueryResponse response = convenienceMasterPatientIndexV3Client
        .queryPatientDemographics(
            query,
            affinityDomain, null, null);

    log.debug("getPatientIdentifier response:{} {}", response.getSuccess(),
        response.getPatients() == null ? 0 : response.getPatients().size());
    if (!response.getSuccess() || (response.getPatients() == null) || (response.getPatients().size() != 1)) {
      throw new TechnicalException("Patient was not found. Details: " + patientIdentifier);
    }

    Patient patient = response.getPatients().get(0);
    log.info("Found patient {} communityId: {}, local Assigning Authority {}, local extension: {}",
        patient.getCompleteName(), communityIdentifier, localAssigningAuthorityOid, localId);
    fillPatientIdentifierByResult(patientIdentifier, response);

    HumanNameDTO patientInfo = new HumanNameDTO(
        patient.getName().getGiven(),
        patient.getName().getFamily(),
        patient.getName().getPrefix(),
        patient.getBirthday() != null ? LocalDate.ofInstant(
            patient.getBirthday().toInstant(), ZoneId.systemDefault()) : null,
        patient.getAdministrativeGenderCode().name());
    patientIdentifier.setPatientInfo(patientInfo);

    return patientIdentifier;
  }

  private void increaseConfidentiality(BaseDTO dto) {
    ValueDTO confidentiality = dto.getConfidentiality();

    confidentiality = switch (confidentiality.getCode()) {
      case "17621005": // Normal
        yield HuskyUtils.RESTRICTED_CONFIDENTIALITY_CODE;
      case "263856008": // Restricted
        yield HuskyUtils.SECRET_CONFIDENTIALITY_CODE;
      default:
        throw new TechnicalException("Writing failed after increasing confidentiality");
    };

    dto.setConfidentiality(confidentiality);
  }

  private synchronized XuaClient initXuaClient(RepositoryConfig repository) {
    if (xuaClient == null) {
      // initialize XUA client to query XUA assertion
      XuaClientConfig xuaClientConfig = new XuaClientConfigBuilderImpl()
          .clientKeyStore(repository.getXua().getClientKeyStore())
          .clientKeyStorePassword(repository.getXua().getClientKeyStorePass())
          .clientKeyStoreType(repository.getXua().getClientKeyStoreType())
          .url(repository.getUri())
          .create();

      xuaClient = ClientFactory.getXuaClient(xuaClientConfig);
    }

    return xuaClient;
  }

  // Husky Library uses the wrong dialect for the CH reference implementation. As setter is ignoring
  // the parameter, we have to manually overwrite the dialect.
  private void overwriteDialect(XUserAssertionRequestBuilderChImpl chImpl) throws Exception {
    // according to Annex 5, attachment 1
    String claimDialect = "http://www.bag.admin.ch/epr/2017/annex/5/amendment/2";
    Field claimsField = XUserAssertionRequestBuilderImpl.class.getDeclaredField("claims");
    claimsField.setAccessible(true);
    Claims claims = (Claims) ReflectionUtils.getField(claimsField, chImpl);
    claims.setDialect(claimDialect);
  }

  private void setTitle(DocumentMetadata metadata, BaseDTO dto) {
    String type = "Vaccination";
    if (dto instanceof AllergyDTO) {
      type = "Adverse Event";
    } else if (dto instanceof MedicalProblemDTO) {
      type = "Medical Problem";
    } else if (dto instanceof PastIllnessDTO) {
      type = "Infectious Disease";
    } else if (dto instanceof VaccinationRecordDTO) {
      type = "Vaccination Record";
    }

    String name = dto.getCode() != null ? dto.getCode().getName() : "";
    metadata.setTitle(type + " - " + name);
  }

  private Author toAuthor(AuthorDTO authorDto) {
    if (authorDto == null) {
      authorDto =
          new AuthorDTO(new HumanNameDTO("testfirstname", "testlastname", "testprefix", LocalDate.now(), "MALE"),
              "HCP", "gln:1.2.3.4");
    }
    Author author = new Author();
    Name name = new Name(new NameBaseType());
    name.setGiven(authorDto.getUser().getFirstName());
    name.setFamily(authorDto.getUser().getLastName());
    name.setPrefix(authorDto.getUser().getPrefix());
    author.addName(name);

    boolean isPatient = "PAT".equalsIgnoreCase(authorDto.getRole()) || "REP".equalsIgnoreCase(authorDto.getRole());
    author.setSpeciality(isPatient ? vaccinationConfig.getPatient().getAuthorSpeciality()
        : vaccinationConfig.getDoctor().getAuthorSpeciality());
    author
        .setRoleFunction(new Code(authorDto.getRole(), "2.16.756.5.30.1.127.3.10.6", authorDto.getRole()));
    return author;
  }

  private Code toCode(ValueDTO valueDTO) {
    if (valueDTO == null) {
      log.warn("Code is null!");
      return null;
    }
    return new Code(valueDTO.getCode(), valueDTO.getSystem(), valueDTO.getName());
  }

  private String writeConsideringConfidentiality(String uuid, String json, BaseDTO dto, Assertion assertion,
      boolean isVaccinationRecord,
      CommunityConfig communityConfig, Identificator globalId, Identificator spid, Identificator localIdentifier,
      RepositoryConfig repositoryConfig) throws Exception, FileNotFoundException {
    log.debug("Add document with confidentiality {} to the EPD {}.", dto.getConfidentiality().getName(), json);
    DocumentMetadata metadata = convenienceCommunication.addDocument(DocumentDescriptor.FHIR_JSON,
        new ByteArrayInputStream(json.getBytes()));

    SubmissionSetMetadata subSet = new SubmissionSetMetadata();
    setDocumentMetadata(metadata, localIdentifier, globalId, dto, isVaccinationRecord);
    setSubmissionSetMetadata(subSet, globalId, dto.getAuthor());

    // submit added documents
    if (Boolean.FALSE.equals(profileConfig.getHuskyLocalMode())) {
      Assertion xUserAssertion =
          getXUserAssertion(dto.getAuthor(), assertion, spid, communityConfig, repositoryConfig.getUri());

      convenienceCommunication.setAtnaConfig(AtnaConfig.AtnaConfigMode.SECURE);

      Response response = convenienceCommunication.submit(subSet, xUserAssertion, null);
      log.debug("writeDocument status {}", response);
      if (Status.FAILURE.equals(response.getStatus()) || Status.PARTIAL_SUCCESS.equals(response.getStatus())) {
        log.error("Error codes occured during writing: {}", response.getErrors());
      }

      return response.getStatus().name();
    }
    if (Boolean.TRUE.equals(profileConfig.getHuskyLocalMode())) {
      uuid = uuid.replace(FhirConstants.DEFAULT_ID_PREFIX, "");
      String filepath = Paths.get(FhirAdapter.CONFIG_TESTFILES_JSON, uuid + ".json").toString();
      try (PrintStream out = new PrintStream(new FileOutputStream(filepath))) {
        out.print(json);
      }
    }

    return Status.SUCCESS.name();
  }
}
