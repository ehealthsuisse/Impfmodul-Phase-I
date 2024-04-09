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

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.data.request.EPRDocument;
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.admin.bag.vaccination.service.cache.CacheIdentifierKey;
import ch.admin.bag.vaccination.service.husky.HuskyAdapterIfc;
import ch.admin.bag.vaccination.service.husky.HuskyUtils;
import ch.fhir.epr.adapter.FhirAdapterIfc;
import ch.fhir.epr.adapter.FhirConstants;
import ch.fhir.epr.adapter.FhirUtils;
import ch.fhir.epr.adapter.config.FhirConfig;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * BaseService contains all the business logic to handle Vaccination, Allergy and PastIllness.
 *
 */
@Slf4j
@Service
public class BaseService<T extends BaseDTO> implements BaseServiceIfc<T> {
  private static final String LOAD_DATA_FOR_FROM = "Load data for {} from";
  @Autowired
  protected LifeCycleService lifeCycleService;
  @Autowired
  protected FhirAdapterIfc fhirAdapter;
  @Autowired
  protected HuskyAdapterIfc huskyAdapter;
  @Autowired
  protected VaccinationConfig vaccinationConfig;
  @Autowired
  protected FhirConfig fhirConfig;
  @Autowired
  private Cache cache;
  @Autowired
  private ProfileConfig profileConfig;

  @Override
  public T create(String communityIdentifier, String oid, String localId,
      T newDTO, Assertion assertion) {
    log.debug("create {} {} {}", communityIdentifier, oid, localId);
    PatientIdentifier patientIdentifier = getPatientIdentifier(communityIdentifier, oid, localId);
    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    newDTO.setAuthor(author);

    Bundle createdBundle = fhirAdapter.create(patientIdentifier, newDTO);
    if (createdBundle != null) {
      return getDTOAfterParsingBundle(newDTO, assertion, createdBundle, patientIdentifier, author);
    }

    throw new TechnicalException("no bundle was created, check server logs.");
  }

  @Override
  public T delete(String communityIdentifier, String oid, String localId, String toDeleteUuid, ValueDTO confidentiality,
      Assertion assertion) {
    log.debug("delete {} {} {} {}", communityIdentifier, oid, localId, toDeleteUuid);
    PatientIdentifier patientIdentifier = getPatientIdentifier(communityIdentifier, oid, localId);

    List<EPRDocument> eprDocuments = getData(patientIdentifier, assertion);

    // Check if the deletion candidate was already deleted
    // If the EPRDocument contains the toDeleteUuid and also has the status entered-in-error, it means
    // that it was already deleted
    eprDocuments = eprDocuments.stream()
        .filter(eprDocument -> eprDocument.getJsonOrXmlFhirContent().contains(toDeleteUuid)).toList();
    Optional<T> deletedResource = eprDocuments.stream()
        .filter(eprDocument -> eprDocument.getJsonOrXmlFhirContent().contains(FhirConstants.ENTERED_IN_ERROR))
        .map(eprDocument -> fhirAdapter.unmarshallFromString(eprDocument.getJsonOrXmlFhirContent()))
        .map(alreadyDeletedBundle -> parseBundle(patientIdentifier, alreadyDeletedBundle).get(0))
        .findFirst();

    if (deletedResource.isPresent()) {
      return deletedResource.get();
    }

    for (EPRDocument doc : eprDocuments) {
      Bundle bundleToDelete = fhirAdapter.unmarshallFromString(doc.getJsonOrXmlFhirContent());

      T dto = fhirAdapter.getDTO(getDtoClass(), bundleToDelete, toDeleteUuid);
      if (dto != null) {
        AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
        if (confidentiality != null) {
          dto.setConfidentiality(confidentiality);
          dto.setAuthor(author);
        }

        Bundle deletedBundle = fhirAdapter.delete(patientIdentifier, dto, bundleToDelete, toDeleteUuid);
        if (deletedBundle != null) {
          return getDTOAfterParsingBundle(dto, assertion, deletedBundle, patientIdentifier, author);
        }
      }
    }

    throw new TechnicalException("No bundle was deleted, check server logs.");
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<T> getAll(PatientIdentifier patientIdentifier, Assertion assertion, boolean isLifecycleActive) {
    List<T> dtos = new ArrayList<>();
    List<T> dtoWithErrors = new ArrayList<>();
    List<EPRDocument> eprDocuments = getData(patientIdentifier, assertion);
    List<EPRDocument> invalid = new ArrayList<>();

    for (EPRDocument doc : eprDocuments) {
      Bundle bundle = fhirAdapter.unmarshallFromString(doc.getJsonOrXmlFhirContent());
      List<T> jsonDtos = new ArrayList<>();
      if (bundle != null) {
        jsonDtos.addAll(parseBundle(patientIdentifier, bundle));
        jsonDtos.forEach(dto -> {
          dto.setValidated(doc.isTrusted() && dto.isValidated());
          dto.setJson(doc.getJsonOrXmlFhirContent());
        });
      } else {
        dtoWithErrors.add(createFailureDto(doc.getJsonOrXmlFhirContent()));
        invalid.add(doc);
      }
      dtos.addAll(jsonDtos);
    }

    eprDocuments.removeAll(invalid);
    CacheIdentifierKey cacheIdentifier = createCacheIdentifier(patientIdentifier);
    if (cache.dataCacheMiss(cacheIdentifier)) {
      for (EPRDocument doc : eprDocuments) {
        cache.putData(cacheIdentifier, doc);
      }
    }

    // touch cache so entries are not reloaded.
    if (eprDocuments.isEmpty()) {
      cache.putData(cacheIdentifier, new EPRDocument());
    }

    List<T> result = (List<T>) lifeCycleService.handle(dtos, !isLifecycleActive);
    result.addAll(dtoWithErrors);
    return result;
  }

  @Override
  public List<T> getAll(String communityIdentifier,
      String oid, String localId, Assertion assertion) {
    return getAll(communityIdentifier, oid, localId, assertion, true);
  }

  @Override
  public List<T> getAll(String communityIdentifier, String oid, String localId, Assertion assertion,
      boolean isLifecycleActive) {
    PatientIdentifier patientIdentifier = getPatientIdentifier(communityIdentifier, oid, localId);

    if (patientIdentifier != null) {
      return getAll(patientIdentifier, assertion, isLifecycleActive);
    }

    return Collections.emptyList();
  }

  public PatientIdentifier getPatientIdentifier(String communityIdentifier, String oid, String localId) {
    PatientIdentifier patientIdentifier = new PatientIdentifier(communityIdentifier, localId, oid);
    if (!profileConfig.isLocalMode() && !HttpSessionUtils.isValidAccessToPatientInformation(patientIdentifier)) {
      return null;
    }

    return huskyAdapter.getPatientIdentifier(communityIdentifier, oid, localId);
  }

  @Override
  public T update(String communityIdentifier, String oid, String localId,
      String toUpdateUuid, T newDto, Assertion assertion) {
    log.debug("update {} {} {} {}", communityIdentifier, oid, localId, toUpdateUuid);
    PatientIdentifier patientIdentifier = getPatientIdentifier(communityIdentifier, oid, localId);

    List<EPRDocument> eprDocuments = getData(patientIdentifier, assertion);
    for (EPRDocument doc : eprDocuments) {
      Bundle bundleToUpdate = fhirAdapter.unmarshallFromString(doc.getJsonOrXmlFhirContent());

      T dto = fhirAdapter.getDTO(getDtoClass(), bundleToUpdate, toUpdateUuid);
      if (dto != null && toUpdateUuid.equals(dto.getId())) {
        AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
        newDto.setAuthor(author);
        Bundle updatedBundle =
            fhirAdapter.update(patientIdentifier, newDto, bundleToUpdate, toUpdateUuid);
        if (updatedBundle != null) {
          return getDTOAfterParsingBundle(newDto, assertion, updatedBundle, patientIdentifier, author);
        }
      }
    }

    throw new TechnicalException("no bundle was updated, check server logs.");
  }

  @Override
  public T validate(String communityIdentifier, String oid, String localId, String toUpdateUuid, T newDto,
      Assertion assertion) {
    log.debug("validate {} {} {} {}", communityIdentifier, oid, localId, toUpdateUuid);
    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    if (!fhirConfig.isPractitioner(author.getRole())) {
      throw new TechnicalException("HCP or ASS role required!");
    }

    return update(communityIdentifier, oid, localId, toUpdateUuid, newDto, assertion);
  }

  @SuppressWarnings("unchecked")
  protected Class<T> getDtoClass() {
    return (Class<T>) BaseDTO.class;
  }

  private CacheIdentifierKey createCacheIdentifier(PatientIdentifier patientIdentifier) {
    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    return new CacheIdentifierKey(patientIdentifier, author);
  }

  private EPRDocument createEPRDocument(DocumentEntry entry, Map<String, RetrievedDocument> docMap) {
    List<String> rolesMetadata = entry.getExtraMetadata().get(HuskyUtils.UPLOADER_ROLE_METADATA_KEY);
    String[] roles = rolesMetadata.get(0).split("\\^", 2);
    boolean isTrusted = Arrays.asList(HuskyUtils.HCP, HuskyUtils.ASS, HuskyUtils.TCU).contains(roles[0]);

    RetrievedDocument retrievedDoc = docMap.get(entry.getUniqueId());
    if (Objects.isNull(retrievedDoc)) {
      log.error("Document not found for unique ID: " + entry.getUniqueId());
      return null;
    }

    docMap.remove(retrievedDoc.getRequestData().getDocumentUniqueId());
    return new EPRDocument(isTrusted, retrievedDoc);
  }

  @SuppressWarnings("unchecked")
  private T createFailureDto(String json) {
    T dto;
    try {
      dto = getDtoClass().getDeclaredConstructor().newInstance();
    } catch (Exception ex) {
      log.warn("Could not create class {} during creating of failure dto.", getDtoClass().getSimpleName());
      dto = (T) new BaseDTO() {
        @Override
        public LocalDate getDateOfEvent() {
          return null;
        }
      };
    }

    dto.setId("-1");
    dto.setCreatedAt(LocalDateTime.now());
    dto.setCode(new ValueDTO("---", "---", "---"));
    dto.setHasErrors(true);
    dto.setContent(json.getBytes(Charset.forName("UTF-8")));
    return dto;
  }

  private EPRDocument createValidatedDocument(AuthorDTO author, String json) {
    boolean isTrusted = Arrays.asList(HuskyUtils.HCP, HuskyUtils.ASS, HuskyUtils.TCU).contains(author.getRole());
    return new EPRDocument(isTrusted, json, null);
  }

  private List<EPRDocument> fetchDocuments(PatientIdentifier patientIdentifier, AuthorDTO author,
      Assertion assertion, boolean useInternal) {
    List<DocumentEntry> documentEntries =
        huskyAdapter.getDocumentEntries(patientIdentifier, author, assertion, useInternal);

    List<DocumentEntry> filteredDocumentEntries =
        documentEntries.stream().filter(doc -> doc.getMimeType().toLowerCase().contains("fhir"))
            .collect(Collectors.toList());

    int numberOfFilteredEntries = documentEntries.size() - filteredDocumentEntries.size();
    if (documentEntries.size() > filteredDocumentEntries.size()) {
      log.debug("Filtered {} meta entries because of wrong mimetype, i.e. mimetype does not contain \"fhir\"",
          numberOfFilteredEntries);
    }

    List<RetrievedDocument> retrievedDocuments = huskyAdapter.getRetrievedDocuments(
        patientIdentifier, filteredDocumentEntries, author, assertion,
        useInternal);

    return processAndValidateDocuments(documentEntries, retrievedDocuments);
  }

  private List<EPRDocument> getData(PatientIdentifier patientIdentifier, Assertion assertion) {
    boolean patientIdentifierCouldNotBeResolved = patientIdentifier == null;
    if (patientIdentifierCouldNotBeResolved
        || (!profileConfig.isLocalMode() && !HttpSessionUtils.isValidAccessToPatientInformation(patientIdentifier))) {
      return Collections.emptyList();
    }

    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    CacheIdentifierKey cacheIdentifier = createCacheIdentifier(patientIdentifier);
    List<EPRDocument> eprDocuments = new ArrayList<>();

    if (cache.isEnabled() && !cache.dataCacheMiss(cacheIdentifier)) {
      log.debug(LOAD_DATA_FOR_FROM + " cache.", patientIdentifier.getPatientInfo().getFullName());
      return cache.getData(cacheIdentifier);
    }

    if (profileConfig.isLocalMode()) {
      log.debug(LOAD_DATA_FOR_FROM + " Filesystem", patientIdentifier.getPatientInfo().getFullName());
      return fhirAdapter.getLocalEntities().stream().map(localEntity -> new EPRDocument(true, localEntity, null))
          .collect(Collectors.toList());
    }
    log.debug(LOAD_DATA_FOR_FROM + " EPD Backend", patientIdentifier.getPatientInfo().getFullName());
    log.debug("Fetching documents from internal repository.");
    eprDocuments.addAll(fetchDocuments(patientIdentifier, author, assertion, true));
    log.debug("Fetching documents from external repository.");
    eprDocuments.addAll(fetchDocuments(patientIdentifier, author, assertion, false));

    for (EPRDocument doc : eprDocuments) {
      handleRetrievedDocument(doc);
    }

    return eprDocuments;
  }

  private String getDocumentData(RetrievedDocument retrievedDocument) {
    try {
      InputStream is = retrievedDocument.getDataHandler().getInputStream();
      byte[] bytesOfDocument = is.readAllBytes();

      return new String(bytesOfDocument, Charset.forName("UTF-8"));
    } catch (Exception ex) {
      log.warn("Error while retrieving document data: {}", ex.getMessage());
      return null;
    }
  }

  private T getDTOAfterParsingBundle(T newOrUpdatedDto, Assertion assertion, Bundle newOrUpdatedBundle,
      PatientIdentifier patientIdentifier,
      AuthorDTO author) {
    String updatedJson = fhirAdapter.convertBundleToJson(newOrUpdatedBundle);
    huskyAdapter.writeDocument(patientIdentifier, FhirUtils.getUuidFromBundle(newOrUpdatedBundle), updatedJson,
        newOrUpdatedDto, assertion);
    cache.putData(createCacheIdentifier(patientIdentifier), createValidatedDocument(author, updatedJson));
    return parseBundle(patientIdentifier, newOrUpdatedBundle).get(0);
  }

  private void handleRetrievedDocument(EPRDocument doc) {
    String document = getDocumentData(doc.getRetrievedDocument());
    doc.setJsonOrXmlFhirContent(document);
  }

  @SuppressWarnings("unchecked")
  private List<T> parseBundle(PatientIdentifier patientIdentifier, Bundle bundle) {
    Class<T> dtoClass = getDtoClass();

    try {
      if (BaseDTO.class != dtoClass) {
        return fhirAdapter.getDTOs(dtoClass, bundle);
      }

      // for base class, fetch all entities
      List<BaseDTO> results = new ArrayList<>();
      results.addAll(fhirAdapter.getDTOs(VaccinationDTO.class, bundle));
      results.addAll(fhirAdapter.getDTOs(AllergyDTO.class, bundle));
      results.addAll(fhirAdapter.getDTOs(MedicalProblemDTO.class, bundle));
      results.addAll(fhirAdapter.getDTOs(PastIllnessDTO.class, bundle));

      return (List<T>) results;
    } catch (Exception ex) {
      log.error("Bundle from patient with ID {} could not be parsed.", patientIdentifier.getSpidExtension());
      return List.of(createFailureDto(fhirAdapter.convertBundleToJson(bundle)));
    }
  }

  private List<EPRDocument> processAndValidateDocuments(
      List<DocumentEntry> documentEntries, List<RetrievedDocument> retrievedDocuments) {
    Map<String, RetrievedDocument> docMap = retrievedDocuments.stream()
        .collect(Collectors.toMap(retrievedDocument -> retrievedDocument.getRequestData().getDocumentUniqueId(),
            Function.identity()));

    List<EPRDocument> eprDocuments = documentEntries.stream()
        .map(entry -> createEPRDocument(entry, docMap))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    docMap.values().forEach(retrievedDoc -> eprDocuments.add(new EPRDocument(true, retrievedDoc)));
    return eprDocuments;
  }
}
