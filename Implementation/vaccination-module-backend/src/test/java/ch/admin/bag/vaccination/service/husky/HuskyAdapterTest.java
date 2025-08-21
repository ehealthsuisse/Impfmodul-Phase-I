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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.admin.bag.vaccination.service.husky.config.CommunitiesConfig;
import ch.admin.bag.vaccination.service.husky.config.CommunityConfig;
import ch.admin.bag.vaccination.service.husky.config.EPDCommunity;
import ch.admin.bag.vaccination.service.husky.config.OidConfig;
import ch.admin.bag.vaccination.service.husky.config.RepositoryConfig;
import ch.admin.bag.vaccination.service.husky.config.XuaConfig;
import ch.admin.bag.vaccination.service.saml.SAMLXmlTestUtils;
import ch.admin.bag.vaccination.utils.SSLValidation;
import ch.fhir.epr.adapter.FhirAdapter;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;
import org.projecthusky.common.communication.DocumentMetadata;
import org.projecthusky.common.communication.SubmissionSetMetadata;
import org.projecthusky.common.enums.LanguageCode;
import org.projecthusky.common.model.Code;
import org.projecthusky.common.model.Identificator;
import org.projecthusky.xua.saml2.Assertion;
import org.projecthusky.xua.saml2.impl.AssertionBuilderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HuskyAdapterTest {
  private static final ValueDTO ANY_CODE = new ValueDTO("code", "name", "system");

  @Autowired
  private HuskyAdapter huskyAdapter;

  @Autowired
  private FhirAdapter fhirAdapter;

  @Autowired
  private CommunitiesConfig communities;

  @Autowired
  private ProfileConfig profileConfig;

  @Autowired
  private Cache cache;

  @Autowired
  private LocalSyslogServer syslogServer;

  @BeforeAll
  void beforeAll() throws Exception {
    SSLValidation.disableChecks();
    syslogServer.startup();
  }

  @BeforeEach
  void beforeEach() {
    syslogServer.getLastMessage(); // Eatup the message
    assertThat(syslogServer.getLastMessage()).isNull();
    cache.clear();
    profileConfig.setLocalMode(true);
    profileConfig.setHuskyLocalMode(Boolean.FALSE);
  }

  @Test
  @Disabled
  void getDocumentEntries_existingDocumentEntries_EPDPLAYGROUND() throws Exception {
    PatientIdentifier patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");
    AuthorDTO author = new AuthorDTO(new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null));
    List<DocumentEntry> documentEntries =
        huskyAdapter.getDocumentEntries(patientIdentifier, null, null, author, null, true);

    assertThat(documentEntries.size()).isGreaterThanOrEqualTo(0);

    CommunityConfig communityConfig = new CommunityConfig();
    RepositoryConfig repositoryConfig = new RepositoryConfig();
    OidConfig receiver = new OidConfig();
    receiver.setApplicationOid("2.16.840.1.113883.3.72.6.5.100.1399");
    receiver.setFacilityOid("Waldpsital Bern");
    repositoryConfig
        .setUri("https://epdplayground.i4mi.bfh.ch:6443/Repository/services/RepositoryService");
    repositoryConfig.setReceiver(receiver);
    repositoryConfig.setHomeCommunityOid("urn:oid:1.1.1");

    List<RetrievedDocument> retrievedDocuments = huskyAdapter.getRetrievedDocuments(patientIdentifier, communityConfig,
        repositoryConfig, documentEntries, author, null);

    try (var is = retrievedDocuments.getFirst().getDataHandler().getInputStream()) {
      byte[] bytesOfDocument = is.readAllBytes();
      if (bytesOfDocument != null) {
        // log.debug("{}", new String(bytesOfDocument));
        Bundle bundle = fhirAdapter.unmarshallFromString(new String(bytesOfDocument));
        List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
        assertThat(vaccinations.size()).isEqualByComparingTo(1);
        VaccinationDTO vaccinationDTO = vaccinations.getFirst();
        log.debug("{}", vaccinationDTO);
        assertThat(vaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
        assertThat(syslogServer.getLastMessage()).contains("Retrieve Document Set");
      }
    }
  }

  // @Test
  void getDocumentEntries_existingDocumentEntries_GAZELLE() throws Exception {
    PatientIdentifier patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");
    List<DocumentEntry> documentEntries = huskyAdapter.getDocumentEntries(patientIdentifier, null, null,
        new AuthorDTO(new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null)), null, true);
    assertThat(documentEntries.size()).isEqualTo(42);

    for (DocumentEntry documentEntry : documentEntries) {
      assertThat(documentEntry.getAuthors().size()).isEqualTo(1);
      if ("Allzeit"
          .equals(documentEntry.getAuthors().getFirst().getAuthorPerson().getName().getGivenName()) &&
          "Bereit".equals(
              documentEntry.getAuthors().getFirst().getAuthorPerson().getName().getFamilyName())) {

        assertEquals("41000179103", documentEntry.getTypeCode().getCode());
        assertEquals("Immunization record",
            documentEntry.getTypeCode().getDisplayName().getValue());
        assertEquals("2.16.840.1.113883.6.96", documentEntry.getTypeCode().getSchemeName());
        assertEquals("1.2.820.99999.18508463736145106181926975526539403561455330316563",
            documentEntry.getUniqueId());
      }
    }
  }

  @Test
  void getPatient_existingPatient_EPDPLAYGROUND() {
    PatientIdentifier patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "waldspital-Id-1234");
    assertThat(patientIdentifier.getSpidExtension()).isEqualTo("761337637673823141");
    assertThat(patientIdentifier.getGlobalExtension())
        .isEqualTo("2dc7a783-78b1-4627-94fb-610a23135c42");
    assertThat(syslogServer.getLastMessage()).contains("Patient Demographics Query");
  }

  /**
   * Test <a href="https://jira.e-health-suisse.ch/browse/IMAW-299#value">IMAW-299</a>
   */
  @Test
  @Disabled("Disabled until the Gazelle Patient Manager application is operational again")
  void getPatient_existingPatient_GAZELLE() {
    PatientIdentifier patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.GAZELLE.name(), "2.16.756.5.30.1.127.3.10.3",
            "761337610410555888");
    assertThat(patientIdentifier.getPatientInfo().getLastName()).isEqualTo("Maiden");
    assertThat(patientIdentifier.getPatientInfo().getFirstName()).isEqualTo("Alice");
    assertThat(patientIdentifier.getSpidExtension()).isEqualTo("761337610410555888");
    assertThat(patientIdentifier.getGlobalExtension()).isEqualTo("CHPAM203");

    patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.GAZELLE.name(), "2.16.756.5.30.1.127.3.10.3",
            "761337610410555999");
    assertThat(patientIdentifier.getPatientInfo().getLastName()).isEqualTo("Ehrxyzmedical");
    assertThat(patientIdentifier.getPatientInfo().getFirstName()).isEqualTo("Alessandra Pauline");
    assertThat(patientIdentifier.getSpidExtension()).isEqualTo("761337610410555999");
    assertThat(patientIdentifier.getGlobalExtension()).isEqualTo("CHPAM204");
    assertThat(syslogServer.getLastMessage()).contains("Patient Demographics Query");
  }

  @Test
  void getPatient_noPatientInformation_throwNullPointerException() {
    assertThrows(NullPointerException.class,
        () -> huskyAdapter.getPatientIdentifier(EPDCommunity.DUMMY.name(), null, null));
    assertThat(syslogServer.getLastMessage()).isNull();
  }

  @Test
  void getPatientIdentifier_existing_patient() {
    PatientIdentifier patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(),
            "1.2.3.4.123456.1", "waldspital-Id-1234");
    assertThat(patientIdentifier.getLocalAssigningAuthority()).isEqualTo("1.2.3.4.123456.1");
    assertThat(patientIdentifier.getLocalExtenstion()).isEqualTo("waldspital-Id-1234");
    assertThat(patientIdentifier.getGlobalAuthority()).isEqualTo("1.1.1.99.1");
    assertThat(patientIdentifier.getGlobalExtension())
        .isEqualTo("2dc7a783-78b1-4627-94fb-610a23135c42");
    assertThat(patientIdentifier.getSpidRootAuthority()).isEqualTo("2.16.756.5.30.1.127.3.10.3");
    assertThat(patientIdentifier.getSpidExtension()).isEqualTo("761337637673823141");

    assertThat(patientIdentifier.getPatientInfo().getPrefix()).isEqualTo("");
    assertThat(patientIdentifier.getPatientInfo().getFirstName()).isEqualTo("Eliane");
    assertThat(patientIdentifier.getPatientInfo().getLastName()).isEqualTo("Piazza-Baumann");
    assertThat(patientIdentifier.getPatientInfo().getBirthday()).isEqualTo("1967-01-15");
    assertThat(patientIdentifier.getPatientInfo().getGender()).isEqualTo("FEMALE");
    assertThat(syslogServer.getLastMessage()).contains("Patient Demographics Query");
  }

  @Test
  void getPatientIdentifier_unknown_patient() {
    assertThrows(TechnicalException.class,
        () -> huskyAdapter.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
            "dummy"));
    assertThat(syslogServer.getLastMessage()).contains("Patient Demographics Query");
  }

  @Test
  @Disabled
  void getRetrievedDocuments_existingDocument_EPDPLAYGROUND() throws Exception {
    org.opensaml.saml.saml2.core.Assertion osAssertion = SAMLXmlTestUtils.createAssertion("saml/Assertion.xml");
    org.projecthusky.xua.saml2.Assertion huskyAssertion = new AssertionBuilderImpl().create(osAssertion);

    PatientIdentifier patientIdentifier = new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234");
    DocumentEntry documentEntry = new DocumentEntry();
    documentEntry.setRepositoryUniqueId("1.1.1.2.31");
    documentEntry.setUniqueId("2.25.90799173491713586491471839779315544798"); // 2.25.253445961889251413523507992196901058285

    AuthorDTO author = new AuthorDTO(new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null));
    author.setRole("HCP");
    author.setPurpose("NORM");

    CommunityConfig communityConfig = new CommunityConfig();
    RepositoryConfig repositoryConfig = new RepositoryConfig();
    repositoryConfig.setIdentifier("InternalRetrieveDocumentSet");
    repositoryConfig.setHomeCommunityOid("urn:oid:1.1.1");
    OidConfig receiver = new OidConfig();
    receiver.setApplicationOid("2.16.840.1.113883.3.72.6.5.100.1399");
    receiver.setFacilityOid("Waldpsital Bern");
    repositoryConfig
        .setUri("https://epdplayground.i4mi.bfh.ch:6443/Repository/services/RepositoryService");
    repositoryConfig.setReceiver(receiver);

    RepositoryConfig xuaRepositoryConfig = new RepositoryConfig();
    xuaRepositoryConfig.setIdentifier("XUA");
    xuaRepositoryConfig.setUri("https://ehealthsuisse.ihe-europe.net:10443/STS?wsdl");
    OidConfig xuaReceiver = new OidConfig();
    xuaReceiver.setApplicationOid("1.3.6.1.4.1.21367.2017.2.5.97");
    xuaReceiver.setFacilityOid("1.3.6.1.4.1.21367.2017.2.7.127");
    xuaRepositoryConfig.setReceiver(xuaReceiver);
    XuaConfig xuaConfig = new XuaConfig();
    xuaConfig.setClientKeyStore("config/GazelleKeystore.p12");
    xuaConfig.setClientKeyStorePass("changeit");
    xuaConfig.setClientKeyStoreType("PKCS12");
    xuaRepositoryConfig.setXua(xuaConfig);

    communityConfig.setRepositories(List.of(repositoryConfig, xuaRepositoryConfig));

    List<RetrievedDocument> retrievedDocuments = huskyAdapter.getRetrievedDocuments(patientIdentifier, communityConfig,
        repositoryConfig, List.of(documentEntry), author, huskyAssertion);

    assertEquals("application/fhir+json", retrievedDocuments.getFirst().getMimeType());

    try (var is = retrievedDocuments.getFirst().getDataHandler().getInputStream()) {
      byte[] bytesOfDocument = is.readAllBytes();
      assertNotNull(bytesOfDocument);
      log.debug("{}", new String(bytesOfDocument));
      Bundle bundle = fhirAdapter.unmarshallFromString(new String(bytesOfDocument));
      List<VaccinationDTO> vaccinations = fhirAdapter.getDTOs(VaccinationDTO.class, bundle);
      assertThat(vaccinations.size()).isEqualByComparingTo(1);
      VaccinationDTO vaccinationDTO = vaccinations.getFirst();
      log.debug("{}", vaccinationDTO);
      assertThat(vaccinationDTO.getLotNumber()).isEqualTo("AHAVB946A");
      assertThat(syslogServer.getLastMessage()).contains("Retrieve Document Set");
    }
  }

  @Test
  void getRetrievedDocuments_existingDocument_EPRPLAYGROUND() {
    PatientIdentifier patientIdentifier = new PatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(), "1.2.3.4.123456.1",
        "waldspital-Id-1234");
    DocumentEntry documentEntry = new DocumentEntry();
    documentEntry.setRepositoryUniqueId("1.1.1.2.31");
    documentEntry.setUniqueId("5b73b737-617c-41cc-86f4-449718d41556");

    List<RetrievedDocument> retrievedDocuments =
        huskyAdapter.getRetrievedDocuments(patientIdentifier, List.of(documentEntry), null, null, true);
    assertThat(retrievedDocuments.size()).isGreaterThanOrEqualTo(0);
    assertThat(syslogServer.getLastMessage()).contains("Retrieve Document Set");
  }

  // @Test
  void getRetrievedDocuments_existingDocument_GAZELLE() throws Exception {
    PatientIdentifier patientIdentifier = new PatientIdentifier(EPDCommunity.GAZELLE.name(), null, null);
    DocumentEntry documentEntry = new DocumentEntry();
    documentEntry.setRepositoryUniqueId("1.1.4567332.1.75");
    documentEntry.setUniqueId("1.2.820.99999.18508463736145106181926975526539403561455330316563");

    List<RetrievedDocument> retrievedDocuments =
        huskyAdapter.getRetrievedDocuments(patientIdentifier, List.of(documentEntry), null, null, true);
    assertThat(retrievedDocuments.size()).isEqualTo(1);

    assertEquals("text/xml", retrievedDocuments.getFirst().getMimeType());

    try (var is = retrievedDocuments.getFirst().getDataHandler().getInputStream()) {
      byte[] bytesOfDocument = is.readAllBytes();
      assertNotNull(bytesOfDocument);
    }
  }

  @Test
  void getXUserAssertion() throws Exception {
    org.opensaml.saml.saml2.core.Assertion osAssertion = SAMLXmlTestUtils.createAssertion("saml/Assertion.xml");
    org.projecthusky.xua.saml2.Assertion huskyAssertion = new AssertionBuilderImpl().create(osAssertion);
    AuthorDTO author = new AuthorDTO(new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null));
    author.setRole("HCP");
    author.setPurpose("NORM");
    Identificator spid = new Identificator("2.16.756.5.30.1.127.3.10.3", "761337610411265304");

    Assertion xUserAssertion = huskyAdapter.getXUserAssertion(author, huskyAssertion, spid,
        communities.getCommunityConfig(EPDCommunity.GAZELLE.name()), null);

    assertThat(xUserAssertion).isNotNull();
    assertEquals("https://ehealthsuisse.ihe-europe.net/STS",
        ((org.opensaml.saml.saml2.core.Assertion) xUserAssertion.getWrappedObject()).getIssuer().getValue());
    assertThat(syslogServer.getLastMessage()).isNull();
  }

  @Test
  void getXUserAssertion_localMode_returnNullUponEmptyAssertion() throws Exception {
    assertNull(huskyAdapter.getXUserAssertion(null, null, null, null, null));
  }

  @Test
  void getXUserAssertion_noLocalMode_nullAssertion_throwException() {
    profileConfig.setLocalMode(false);
    profileConfig.setHuskyLocalMode(false);
    assertThrows(TechnicalException.class, () -> huskyAdapter.getXUserAssertion(null, null, null, null, null));
  }

  @Test
  void getXUserAssertion_noXuaConfigured_throwException() {
    Assertion mockAssertion = mock(Assertion.class);
    CommunityConfig repository = new CommunityConfig();
    profileConfig.setLocalMode(false);
    profileConfig.setHuskyLocalMode(false);
    assertThrows(TechnicalException.class,
        () -> huskyAdapter.getXUserAssertion(null, mockAssertion, null, repository, null));
  }

  @Test
  void setDocumentMetadata() {
    VaccinationDTO vaccination = createVaccination();

    DocumentMetadata metadata = huskyAdapter.setDocumentMetadata(new Identificator("root", "ext"),
        new Identificator("root2", "ext2"),
        vaccination, false);

    assertThat(metadata.getAuthors().getFirst().getName().getFamily()).isEqualTo("Frankenstein");
    assertThat(metadata.getAuthors().getFirst().getName().getGiven()).isEqualTo("Victor");
    assertThat(metadata.getAuthors().getFirst().getName().getPrefix()).isEqualTo("Dr.");
    assertThat(metadata.getAuthors().getFirst().getSpeciality()).isEqualTo(
        new Code("1050", "2.16.756.5.30.1.127.3.5", "Other"));
    assertThat(metadata.getClassCode()).isEqualTo(
        new Code("184216000", "2.16.840.1.113883.6.96", "Patient record type (record artifact)"));
    assertThat(metadata.getConfidentialityCodes().getFirst())
        .isEqualTo(new Code("17621005", HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE.getSystem(), "Normal"));
    assertThat(metadata.getFormatCode()).isEqualTo(new Code("urn:che:epr:ch-vacd:immunization-administration:2022",
        "2.16.756.5.30.1.127.3.10.10", "CH VACD Immunization Administration"));
    assertThat(metadata.getHealthcareFacilityTypeCode())
        .isEqualTo(new Code("43741000", "2.16.840.1.113883.6.96", "Site of Care (environment)"));

    assertThat(metadata.getCodedLanguage()).isEqualTo(LanguageCode.ENGLISH_CODE);
    assertThat(metadata.getMimeType()).isEqualTo("application/fhir+json");
    assertThat(metadata.getPracticeSettingCode())
        .isEqualTo(new Code("394802001", "2.16.840.1.113883.6.96",
            "General medicine (qualifier value)"));
    assertThat(metadata.getTypeCode()).isEqualTo(
        new Code("41000179103", "2.16.840.1.113883.6.96", "Immunization Record (record artifact)"));

    // assertThat(metadata.getDes()).isEqualTo(new Code("184216000", "2.16.840.1.113883.6.96",
    // "Patient record type (record artifact)"));
    assertThat(metadata.getSourcePatientId()).isEqualTo(new Identificator("root", "ext"));
    assertThat(metadata.getDocumentEntry().getPatientId().getAssigningAuthority().getUniversalId())
        .isEqualTo("root2");
    assertThat(metadata.getDocumentEntry().getPatientId().getId()).isEqualTo("ext2");
  }

  @Test
  void setDocumentMetadataWithConfidentiality() {
    VaccinationDTO vaccination = createVaccination();

    DocumentMetadata metadata = huskyAdapter.setDocumentMetadata(new Identificator("root", "ext"),
        new Identificator("root2", "ext2"), vaccination, false);

    ValueDTO cc = HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE;
    assertThat(metadata.getConfidentialityCodes().getFirst())
        .isEqualTo(new Code(cc.getCode(), cc.getSystem(), cc.getName()));
  }

  @Test
  void setSubmissionSetMetadata() {
    SubmissionSetMetadata metadata = new SubmissionSetMetadata();
    AuthorDTO authorDTO = new AuthorDTO(new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null));
    huskyAdapter.setSubmissionSetMetadata(metadata, new Identificator("root", "ext"), authorDTO);

    assertThat(metadata.getContentTypeCode())
        .isEqualTo(new Code("71388002", "2.16.840.1.113883.6.96", "Procedure (procedure)"));
    assertThat(metadata.getAuthor().size()).isEqualTo(1);
    assertThat(metadata.getAuthor().getFirst().getName().getFamily()).isEqualTo("Frankenstein");
    assertThat(metadata.getAuthor().getFirst().getName().getGiven()).isEqualTo("Victor");
    assertThat(metadata.getAuthor().getFirst().getName().getPrefix()).isEqualTo("Dr.");
    assertThat(metadata.getIpfSubmissionSet().getPatientId().getId()).isEqualTo("ext");
    assertThat(
        metadata.getIpfSubmissionSet().getPatientId().getAssigningAuthority().getUniversalId())
            .isEqualTo("root");
  }

  @Test
  void testTitle_setTitleRightBasedOnDto() {
    VaccinationDTO vaccination = new VaccinationDTO();
    checkMetadata("Vaccination", vaccination);

    AllergyDTO allergy = new AllergyDTO();
    checkMetadata("Adverse Event", allergy);

    MedicalProblemDTO problem = new MedicalProblemDTO();
    checkMetadata("Medical Problem", problem);

    PastIllnessDTO illness = new PastIllnessDTO();
    checkMetadata("Infectious Disease", illness);
  }

  @Test
  void unknownCommunity_throwTechnicalException() {
    PatientIdentifier patientIdentifier = new PatientIdentifier("unknownCommunity", null, null);
    assertThrows(TechnicalException.class,
        () -> huskyAdapter.getDocumentEntries(patientIdentifier, null, null, null, null, true));
  }

  @Test
  void writeDocument_EPDPLAYGROUND() {
    PatientIdentifier patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.EPDPLAYGROUND.name(),
            "1.2.3.4.123456.1", "waldspital-Id-1234");
    String json = "{\"fruit\": \"Apple\",\"size\": \"Large\",\"color\": \"Red\"}";
    String uuid = UUID.randomUUID().toString();
    VaccinationDTO vaccination = createVaccination();

    profileConfig.setHuskyLocalMode(null); // Disable the write
    huskyAdapter.writeDocument(patientIdentifier, uuid, json, vaccination, null);
  }

  @Test
  @Disabled("Disabled until the Gazelle Patient Manager application is operational again")
  void writeDocument_GAZELLE() throws Exception {
    PatientIdentifier patientIdentifier =
        huskyAdapter.getPatientIdentifier(EPDCommunity.GAZELLE.name(), "2.16.756.5.30.1.127.3.10.3",
            "761337610411265304");
    String json = "{\"fruit\": \"Apple\",\"size\": \"Large\",\"color\": \"Red\"}";
    String uuid = UUID.randomUUID().toString();

    AuthorDTO author = new AuthorDTO(new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null));
    author.setRole("HCP");
    author.setPurpose("NORM");

    VaccinationDTO vaccination = createVaccination();
    vaccination.setAuthor(author);

    org.opensaml.saml.saml2.core.Assertion osAssertion = SAMLXmlTestUtils.createAssertion("saml/Assertion.xml");
    org.projecthusky.xua.saml2.Assertion huskyAssertion = new AssertionBuilderImpl().create(osAssertion);

    profileConfig.setHuskyLocalMode(null);
    huskyAdapter.writeDocument(patientIdentifier, uuid, json, vaccination, huskyAssertion);
  }

  private void checkMetadata(String titleCategory, BaseDTO baseDto) {
    DocumentMetadata metadata = new DocumentMetadata();
    baseDto.setCode(ANY_CODE);

    ReflectionTestUtils.invokeMethod(huskyAdapter, "setTitle", metadata, baseDto);
    assertEquals(titleCategory + " - name", metadata.getTitle());
  }

  private VaccinationDTO createVaccination() {
    AuthorDTO authorDTO = new AuthorDTO(new HumanNameDTO("Victor", "Frankenstein", "Dr.", null, null));
    ValueDTO confidentiality = HuskyUtils.DEFAULT_CONFIDENTIALITY_CODE;
    VaccinationDTO vaccination = new VaccinationDTO();
    vaccination.setAuthor(authorDTO);
    vaccination.setConfidentiality(confidentiality);
    vaccination.setOccurrenceDate(LocalDate.now());

    return vaccination;
  }
}
