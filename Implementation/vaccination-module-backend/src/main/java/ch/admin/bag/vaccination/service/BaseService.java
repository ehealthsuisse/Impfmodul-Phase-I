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

import static ch.admin.bag.vaccination.service.husky.HuskyUtils.ASS;
import static ch.admin.bag.vaccination.service.husky.HuskyUtils.HCP;
import static ch.admin.bag.vaccination.service.husky.HuskyUtils.TCU;
import static ch.admin.bag.vaccination.service.husky.HuskyUtils.UPLOADER_ROLE_METADATA_KEY;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.data.request.EPRDocument;
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.admin.bag.vaccination.service.cache.CacheIdentifierKey;
import ch.admin.bag.vaccination.service.husky.HuskyAdapterIfc;
import ch.fhir.epr.adapter.FhirAdapterIfc;
import ch.fhir.epr.adapter.FhirUtils;
import ch.fhir.epr.adapter.config.FhirConfig;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public abstract class BaseService<T extends BaseDTO> implements BaseServiceIfc<T> {
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
      String json = fhirAdapter.convertBundleToJson(createdBundle);
      huskyAdapter.writeDocument(patientIdentifier, FhirUtils.getUuidFromBundle(createdBundle), json,
          newDTO, assertion);
      cache.putData(createCacheIdentifier(patientIdentifier), createValidatedDocument(author, json));
      return fhirAdapter.getDTOs(getDtoClass(), createdBundle).get(0);
    }

    throw new TechnicalException("no bundle was created, check server logs.");
  }

  @Override
  public T delete(String communityIdentifier, String oid, String localId, String toDeleteUuid, ValueDTO confidentiality,
      Assertion assertion) {
    log.debug("delete {} {} {} {}", communityIdentifier, oid, localId, toDeleteUuid);
    PatientIdentifier patientIdentifier = getPatientIdentifier(communityIdentifier, oid, localId);

    List<EPRDocument> eprDocuments = getData(patientIdentifier, assertion);
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
          String deletedJson = fhirAdapter.convertBundleToJson(deletedBundle);
          huskyAdapter.writeDocument(patientIdentifier, FhirUtils.getUuidFromBundle(deletedBundle), deletedJson,
              dto, assertion);
          cache.putData(createCacheIdentifier(patientIdentifier), createValidatedDocument(author, deletedJson));
          T result = fhirAdapter.getDTOs(getDtoClass(), deletedBundle).get(0);
          result.setDeleted(true);
          return result;
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
        jsonDtos.addAll(fhirAdapter.getDTOs(getDtoClass(), bundle));
        jsonDtos.forEach(dto -> {
          dto.setValidated(doc.getIsValidated() != null ? doc.getIsValidated() : dto.isValidated());
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

    return getAll(patientIdentifier, assertion, isLifecycleActive);
  }

  public PatientIdentifier getPatientIdentifier(String communityIdentifier, String oid, String localId) {
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
          String updatedJson = fhirAdapter.convertBundleToJson(updatedBundle);
          huskyAdapter.writeDocument(patientIdentifier, FhirUtils.getUuidFromBundle(updatedBundle), updatedJson,
              newDto, assertion);
          cache.putData(createCacheIdentifier(patientIdentifier), createValidatedDocument(author, updatedJson));
          return fhirAdapter.getDTOs(getDtoClass(), updatedBundle).get(0);
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

  protected abstract Class<T> getDtoClass();

  private boolean accessToPatientNotLinkedToSession(PatientIdentifier requestedPatientIdentifier) {
    String requestedLocalExtenstion = requestedPatientIdentifier.getLocalExtenstion();
    String requestedLocalAssigningAuthority = requestedPatientIdentifier.getLocalAssigningAuthority();

    PatientIdentifier identifier = HttpSessionUtils.getPatientIdentifierFromSession();
    if (identifier == null) {
      throw new TechnicalException("No patient identifier found in session.");
    }

    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    String linkedLocalExtenstion = identifier.getLocalExtenstion();
    String linkedLocalAssigningAuthority = identifier.getLocalAssigningAuthority();

    boolean isAccessInvalid = !linkedLocalExtenstion.equalsIgnoreCase(requestedLocalExtenstion)
        || !linkedLocalAssigningAuthority.equalsIgnoreCase(requestedLocalAssigningAuthority);

    if (isAccessInvalid) {
      log.warn(
          "Invalid EPD access detected. User: {} - linked Session: {} - requested access to: {}",
          author != null ? author.getFullName() : "Unknown",
          Map.of("LocalId", linkedLocalExtenstion, "LocalAssigningAuthorityOid", linkedLocalAssigningAuthority),
          Map.of("LocalId", requestedLocalExtenstion, "LocalAssigningAuthorityOid", requestedLocalAssigningAuthority));
    }

    return isAccessInvalid;
  }

  private CacheIdentifierKey createCacheIdentifier(PatientIdentifier patientIdentifier) {
    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    return new CacheIdentifierKey(patientIdentifier, author);
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

    dto.setCode(new ValueDTO("---", "---", "---"));
    dto.setHasErrors(true);
    dto.setContent(json.getBytes(Charset.forName("UTF-8")));
    return dto;
  }

  private EPRDocument createValidatedDocument(AuthorDTO author, String json) {
    boolean isValidated = Arrays.asList(HCP, ASS, TCU).contains(author.getRole());
    return new EPRDocument(isValidated, json, null);
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
    if (!profileConfig.isLocalMode() && accessToPatientNotLinkedToSession(patientIdentifier)) {
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
      return fhirAdapter.getLocalEntities().stream().map(localEntity -> new EPRDocument(null, localEntity, null))
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

  private void handleRetrievedDocument(EPRDocument doc) {
    String document = getDocumentData(doc.getRetrievedDocument());
    doc.setJsonOrXmlFhirContent(document);
  }

  private List<EPRDocument> processAndValidateDocuments(
      List<DocumentEntry> documentEntries, List<RetrievedDocument> retrievedDocuments) {
    Map<String, List<RetrievedDocument>> docMap = retrievedDocuments.stream()
        .collect(Collectors.groupingBy(retrievedDocument -> retrievedDocument.getRequestData().getDocumentUniqueId()));

    return documentEntries.stream()
        .flatMap(documentEntry -> {
          List<String> rolesMetadata = documentEntry.getExtraMetadata().get(
              UPLOADER_ROLE_METADATA_KEY);
          String[] roles = rolesMetadata.get(0).split("\\^", 2);
          boolean isValidated = Arrays.asList(HCP, ASS, TCU).contains(roles[0]);

          List<RetrievedDocument> matchingDocuments = docMap.get(documentEntry.getUniqueId());
          if (Objects.isNull(matchingDocuments) || matchingDocuments.isEmpty()) {
            throw new TechnicalException("Document not found for unique ID: " + documentEntry.getUniqueId());
          }

          return matchingDocuments.stream().map(doc -> new EPRDocument(isValidated, doc));
        })
        .collect(Collectors.toList());
  }
}
