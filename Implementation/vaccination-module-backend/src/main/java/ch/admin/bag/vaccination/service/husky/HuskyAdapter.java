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

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.VaccinationConfig;
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.admin.bag.vaccination.service.husky.config.CommunitiesConfig;
import ch.admin.bag.vaccination.service.husky.config.CommunityConfig;
import ch.admin.bag.vaccination.service.husky.config.EPDRepository;
import ch.admin.bag.vaccination.service.husky.config.RepositoryConfig;
import ch.admin.bag.vaccination.service.husky.config.SenderConfig;
import ch.fhir.epr.adapter.FhirAdapter;
import ch.fhir.epr.adapter.FhirConverter;
import ch.fhir.epr.adapter.FhirUtils;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.projecthusky.xua.communication.xua.impl.ch.XUserAssertionRequestBuilderChImpl;
import org.projecthusky.xua.hl7v3.CE;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HuskyAdapter implements HuskyAdapterIfc {

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

  @Override
  public List<DocumentEntry> getDocumentEntries(PatientIdentifier patientIdentifier,
      List<Code> formatCodes, String documentType, AuthorDTO author, Assertion assertion) {
    log.debug("getDocumentEntries {}", patientIdentifier);
    var communityConfig = getCommunityConfig(patientIdentifier.getCommunityIdentifier());
    var repositoryConfig =
        getRepositoryConfig(communityConfig, EPDRepository.RegistryStoredQuery);

    return getDocumentEntries(communityConfig, repositoryConfig, patientIdentifier,
        formatCodes, documentType, author, assertion);
  }

  @Override
  public PatientIdentifier getPatientIdentifier(String communityIdentifier, String oid, String localId) {
    checkPatientParameter(communityIdentifier, oid, localId);
    PatientIdentifier patientIdentifier = cache.getPatientIdentifier(communityIdentifier, oid, localId);

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
  public List<RetrievedDocument> getRetrievedDocuments(String communityIdentifier,
      List<DocumentEntry> documentEntries) {
    log.debug("getRetrievedDocuments {} {}", communityIdentifier, documentEntries);
    var communityConfig = getCommunityConfig(communityIdentifier);
    var repositoryConfig =
        getRepositoryConfig(communityConfig, EPDRepository.RetrieveDocumentSet);

    return getRetrievedDocuments(communityConfig, repositoryConfig, documentEntries);
  }

  @Override
  public Assertion getXUserAssertion(AuthorDTO author, Assertion idpAssertion, Identificator spid,
      CommunityConfig communityConfig, String uriToAccess) throws Exception {

    RepositoryConfig repository = communityConfig.getRepositoryConfig(EPDRepository.XUA);
    if (repository == null || repository.getXua() == null) {
      log.error("XUA for {} not supported", communityConfig.getIdentifier());
      throw new TechnicalException("XUA clientKeystore not configured. Check husky.yml configuration.");
    }

    // initialize XUA client to query XUA assertion
    XuaClientConfig xuaClientConfig = new XuaClientConfigBuilderImpl()
        .clientKeyStore(repository.getXua().getClientKeyStore())
        .clientKeyStorePassword(repository.getXua().getClientKeyStorePass())
        .clientKeyStoreType(repository.getXua().getClientKeyStoreType())
        .url(repository.getUri())
        .create();

    XuaClient client = ClientFactory.getXuaClient(xuaClientConfig);

    // define the attributes for the X-User Assertion request
    CE role = HuskyUtils.getCodedRole(author.getRole());
    CE purposeOfUse = HuskyUtils.getCodedPurposeOfUse(author.getPurpose());

    String spidEprNamespace =
        communityConfig.getSpidEprNamespace() != null ? communityConfig.getSpidEprNamespace() : "";
    String resourceId = spid.getExtension() + "^^^" + spidEprNamespace + "&" + spid.getRoot() + "&ISO";

    // build the X-User Assertion request
    XUserAssertionRequestBuilderChImpl chImpl = new XUserAssertionRequestBuilderChImpl();
    chImpl.principal(author.getPrincipalId(), author.getPrincipalName());

    var assertionRequest = chImpl
        .requestType(RequestType.WST_ISSUE)
        .tokenType(TokenType.OASIS_WSS_SAML_PROFILE_11_SAMLV20)
        .purposeOfUse(purposeOfUse)
        .appliesTo(new AppliesToBuilderImpl().address(uriToAccess).create())
        .subjectRole(role)
        .resourceId(resourceId)
        .create();

    if (idpAssertion == null) {
      log.warn("IdP assertion is missing. In production environment, this is a major issue.");
      if (!allowEmptyIdPAssertion()) {
        throw new TechnicalException("Identity Provider Assertion is missing, stopping XUA request for " + resourceId);
      }

      return null;
    }

    // query the X-User Assertion
    List<XUserAssertionResponse> response = client.send(idpAssertion, assertionRequest);
    Assertion xuaAssertion = response.get(0).getAssertion();

    return xuaAssertion;
  }

  @PostConstruct
  public void init() throws Exception {
    InitializationService.initialize();
    log.debug("initialize done...");
  }

  @Override
  public String writeDocument(PatientIdentifier patientIdentifier, String uuid, String json,
      AuthorDTO author, ValueDTO confidentiality, Assertion assertion) {

    try {
      var communityConfig = getCommunityConfig(patientIdentifier.getCommunityIdentifier());

      Identificator globalId = new Identificator(patientIdentifier.getGlobalAuthority(),
          patientIdentifier.getGlobalExtension());
      Identificator spid = new Identificator(patientIdentifier.getSpidRootAuthority(),
          patientIdentifier.getSpidRootAuthority());
      Identificator localIdentifier =
          new Identificator(patientIdentifier.getLocalAssigningAuthority(),
              patientIdentifier.getLocalExtenstion());

      var repositoryConfig = getRepositoryConfig(communityConfig, EPDRepository.SubmitDocument);

      Destination dest = getDestination(repositoryConfig);
      AffinityDomain affinityDomain = getAffinityDomain(dest);
      convenienceCommunication.setAffinityDomain(affinityDomain);
      convenienceCommunication.clearDocuments();

      DocumentMetadata metadata =
          convenienceCommunication.addDocument(DocumentDescriptor.FHIR_JSON,
              new ByteArrayInputStream(json.getBytes()));

      SubmissionSetMetadata subSet = new SubmissionSetMetadata();

      setDocumentMetadata(metadata, localIdentifier, globalId, author, confidentiality);
      setSubmissionSetMetadata(subSet, globalId, uuid, author);

      // submit added documents
      if (Boolean.FALSE.equals(profileConfig.getHuskyLocalMode())) {

        // Get the X-User Assertion to authorize the Document Submission.
        Assertion xUserAssertion =
            getXUserAssertion(author, assertion, spid, communityConfig, repositoryConfig.getUri());

        convenienceCommunication.setAtnaConfig(AtnaConfig.AtnaConfigMode.SECURE);

        Response response = convenienceCommunication.submit(subSet, xUserAssertion, null);
        log.debug("writeDocument status {}", response);
        return response.getStatus().name();
      }
      if (Boolean.TRUE.equals(profileConfig.getHuskyLocalMode())) {
        uuid = uuid.replace(FhirConverter.DEFAULT_ID_PREFIX, "");
        String filepath = Paths.get(FhirAdapter.CONFIG_TESTFILES_JSON, uuid + ".json").toString();
        try (PrintStream out = new PrintStream(new FileOutputStream(filepath))) {
          out.print(json);
        }
        return Status.SUCCESS.name();
      }
      return "TEST";
    } catch (Exception e) {
      log.warn("Exception:{}", e);
      return Status.FAILURE.name();
    }

  }

  /**
   * Empty assertion is by default only allowed in test mode. For testing purposes, it can be
   * overwritten to allow empty assertions during exchange with Gazelle or EPDPlayground.
   *
   * @return true if an empty identity provider assertion is allowed.
   */
  protected boolean allowEmptyIdPAssertion() {
    return profileConfig.isLocalMode();
  }

  protected List<RetrievedDocument> getRetrievedDocuments(CommunityConfig communityConfig,
      RepositoryConfig repositoryConfig, List<DocumentEntry> documentEntries) {

    log.debug("getRetrievedDocuments {} {} {}", communityConfig, repositoryConfig, documentEntries);

    Destination dest = getDestination(repositoryConfig);
    AffinityDomain affinityDomain = getAffinityDomain(dest);
    convenienceCommunication.setAffinityDomain(affinityDomain);

    List<DocumentRequest> documentRequestList = new ArrayList<>();
    documentEntries.forEach(documentEntry -> {
      var documentRequest = new DocumentRequest(
          documentEntry.getRepositoryUniqueId(),
          null,
          documentEntry.getUniqueId(),
          repositoryConfig.getHomeCommunityOid());
      documentRequestList.add(documentRequest);
    });

    try {
      RetrievedDocumentSet response =
          convenienceCommunication.retrieveDocuments(documentRequestList.toArray(new DocumentRequest[0]), null, null);

      return response.getDocuments();
    } catch (Exception ex) {
      log.warn("Error while retrieving document data: {}", ex.getMessage());
      return Arrays.asList();
    }
  }

  protected void setDocumentMetadata(DocumentMetadata metadata,
      Identificator localIdentifier, Identificator globalIdentifier, AuthorDTO authorDTO, ValueDTO confidentiality) {
    Author author = toAuthor(authorDTO);
    metadata.addAuthor(author);
    metadata.setClassCode(
        new Code("184216000", "2.16.840.1.113883.6.96", "Patient record type (record artifact)"));
    Code confidentialityCode = toCode(FhirUtils.defaultConfidentialityCode);
    if (confidentiality != null) {
      confidentialityCode = toCode(confidentiality);
    }
    metadata.addConfidentialityCode(confidentialityCode);
    metadata.setFormatCode(new Code("urn:che:epr:ch-vacd:immunization-administration:2022",
        "1.3.6.1.4.1.19376.1.2.3", "CH VACD Immunization Administration"));
    metadata.setHealthcareFacilityTypeCode(
        vaccinationConfig.getDoctor().getHealthCareFacilityTypeCode());
    metadata.setCodedLanguage(LanguageCode.ENGLISH_CODE);
    metadata.setMimeType("application/fhir+json");
    metadata.setPracticeSettingCode(vaccinationConfig.getDoctor().getPracticeSettingCode());
    metadata.setTypeCode(
        new Code("41000179103", "2.16.840.1.113883.6.96", "Immunization Record (record artifact)"));

    metadata.setDestinationPatientId(globalIdentifier);
    metadata.setSourcePatientId(localIdentifier);

    metadata.setTitle("Vaccination");
    // add a extra metadata attribute
    String key = "urn:e-health-suisse:2020:originalProviderRole";
    String code = "HCP^^^&2.16.756.5.30.1.127.3.10.6&ISO";
    DocumentEntry xDoc = metadata.getXDoc();
    Map<String, List<String>> extraMetadata = new HashMap<>();
    List<String> values = List.of(code);
    extraMetadata.put(key, values);
    xDoc.setExtraMetadata(extraMetadata);
  }

  void setSubmissionSetMetadata(SubmissionSetMetadata metadata,
      Identificator globalIdentifier, String uuid,
      AuthorDTO authorDTO) {
    metadata.setContentTypeCode(
        new Code("71388002", "2.16.840.1.113883.6.96", "Procedure (procedure)"));
    Author author = toAuthor(authorDTO);
    metadata.setUniqueId(OidGenerator.uniqueOid().toString());
    metadata.setSourceId(EhcVersions.getCurrentVersion().getOid());
    metadata.setEntryUUID(uuid);
    metadata.addAuthor(author);
    metadata.setDestinationPatientId(globalIdentifier);
  }

  private void checkPatientParameter(String communityIdentifier, String oid, String localId) {
    Objects.requireNonNull(communityIdentifier, "Community identifier must not be null.");
    Objects.requireNonNull(oid, "Local assigning authority must not be null.");
    Objects.requireNonNull(localId, "Local patient id must not be null.");
  }

  private PatientIdentifier createDummyPatientIdentifier(String communityIdentifier, String oid, String localId) {
    PatientIdentifier dummy = new PatientIdentifier(communityIdentifier, localId, oid);
    dummy.setPatientInfo(
        new HumanNameDTO("Max", "Mustermann", null, LocalDate.now(), "MALE"));
    dummy.setGlobalAuthority("global authority");
    dummy.setGlobalExtension("global extension");
    dummy.setSpidExtension("spid extension");
    dummy.setSpidRootAuthority("spid authority");

    return dummy;
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
      log.warn("Community config for {} not found", communityIdentifier);
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
    log.debug("getDocumentEntries {} {} {}", communityConfig, repositoryConfig,
        patientIdentifier);

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
      QueryResponse response =
          convenienceCommunication.queryDocuments(findDocumentsQuery, xUserAssertion, null);

      if (documentType != null) {
        return response.getDocumentEntries().stream()
            .filter(entry -> entry.getTypeCode().getCode().equals(documentType))
            .collect(Collectors.toList());
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
    var repositoryConfig = getRepositoryConfig(communityConfig, EPDRepository.PDQ);

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
    patientIdentifier.setGlobalExtension(response.getPatients().get(0).getIds().get(0).getExtension());
    patientIdentifier.setSpidExtension(response.getPatients().get(0).getIds().get(1).getExtension());
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

  private RepositoryConfig getRepositoryConfig(CommunityConfig communityConfig,
      EPDRepository repository) {
    var repositoryConfig = communityConfig.getRepositoryConfig(repository.name());
    if (repositoryConfig == null) {
      log.warn("Repository Config for {} not found", repository.name());
      throw new TechnicalException("repository.not.found");
    }

    return repositoryConfig;
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
}
