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
import ch.admin.bag.vaccination.utils.JsonFieldExtractor;
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
import ch.fhir.epr.adapter.utils.ValidationUtils;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
      T newDTO, Assertion assertion, boolean forceImmunizationAdministrationDocument) {
    log.debug("create {} {} {}", communityIdentifier, oid, localId);

    PatientIdentifier patientIdentifier = getPatientIdentifier(communityIdentifier, oid, localId);
    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    newDTO.setAuthor(author);
    Bundle createdBundle = forceImmunizationAdministrationDocument ?
        fhirAdapter.createImmunizationAdministrationDocument(patientIdentifier, newDTO) :
        fhirAdapter.create(patientIdentifier, newDTO);

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
        .map(alreadyDeletedBundle -> parseBundle(patientIdentifier, alreadyDeletedBundle).getFirst())
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
    CacheIdentifierKey cacheIdentifier = cache.createCacheIdentifier(patientIdentifier);
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
        modifyVerificationStatus(dto, newDto);
        Bundle updatedBundle = fhirAdapter.update(patientIdentifier, newDto, bundleToUpdate, toUpdateUuid);
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

  private EPRDocument createEPRDocument(DocumentEntry entry, Map<String, RetrievedDocument> docMap) {
    List<String> rolesMetadata = entry.getExtraMetadata().get(HuskyUtils.UPLOADER_ROLE_METADATA_KEY);
    String[] roles = rolesMetadata.getFirst().split("\\^", 2);
    boolean isTrusted = Arrays.asList(HuskyUtils.HCP, HuskyUtils.ASS, HuskyUtils.TCU).contains(roles[0]);

    RetrievedDocument retrievedDoc = docMap.get(entry.getUniqueId());
    if (Objects.isNull(retrievedDoc)) {
      log.error("Document not found for unique ID: " + entry.getUniqueId());
      return null;
    }

    docMap.remove(retrievedDoc.getRequestData().getDocumentUniqueId());
    return new EPRDocument(isTrusted, retrievedDoc, LocalDateTime.now());
  }

  private List<EPRDocument> createEPRDocuments(List<DocumentEntry> allDocumentEntries,
      PatientIdentifier patientIdentifier, List<DocumentEntry> filteredDocumentEntries, AuthorDTO author,
      Assertion assertion, boolean useInternal) {

    List<RetrievedDocument> retrievedDocuments = huskyAdapter.getRetrievedDocuments(patientIdentifier,
        filteredDocumentEntries, author, assertion, useInternal);
    return processAndValidateDocuments(allDocumentEntries, retrievedDocuments);
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
    dto.setContent(json.getBytes(StandardCharsets.UTF_8));
    return dto;
  }

  private EPRDocument createValidatedDocument(AuthorDTO author, String json) {
    boolean isTrusted = Arrays.asList(HuskyUtils.HCP, HuskyUtils.ASS, HuskyUtils.TCU).contains(author.getRole());
    return new EPRDocument(isTrusted, json, null, LocalDateTime.now());
  }

  /**
   * Updates the verification status of the given DTO based on the author role
   * and record validation rules.
   * <p>
   * The verification status is determined as follows:
   * <ul>
   *  *   <li>If the <b>previous record (oldDto)</b> has a {@code null} verification status,
   *  *       a new verification status will be assigned based on the author role.</li>
   *  *   <li>If the <b>author</b> has a GFP role
   *  *       ({@link HuskyUtils#HCP}, {@link HuskyUtils#ASS}, or {@link HuskyUtils#TCU}),
   *  *       the record is always marked as <b>Confirmed</b>.</li>
   *  *   <li>If the <b>author</b> is a PAT (patient) modifying a record created by a GFP,
   *  *       and the modification consists only of adding or updating a comment,
   *  *       the record will <b>remain confirmed</b> and will not be downgraded.</li>
   *  *   <li>If the author is not GFP:
   *  *     <ul>
   *  *       <li>If {@link ValidationUtils#shouldRecordBeValidated(BaseDTO, BaseDTO)}
   *  *           returns {@code true}, the record is marked as <b>Confirmed</b>.</li>
   *  *       <li>Otherwise, the record is marked as <b>Not confirmed</b> / <b>Unconfirmed</b>,
   *  *           depending on the DTO type.</li>
   *  *     </ul>
   *  *   </li>
   *  * </ul>
   * <p>
   * The mapping of DTO type to verification status is:
   * <ul>
   *   <li>{@code VaccinationDTO} → SNOMED code <i>59156000 (Confirmed)</i> or <i>76104008 (Not confirmed)</i></li>
   *   <li>{@code AllergyDTO}, {@code PastIllnessDTO}, {@code MedicalProblemDTO} →
   *       "confirmed"/"unconfirmed" textual codes</li>
   * </ul>
   *
   * @param oldDto     the previous state of the DTO, used for validation checks
   * @param updatedDto the current DTO whose verification status will be modified
   */
  private void modifyVerificationStatus(T oldDto, T updatedDto) {
    boolean isAuthorGFP = Arrays.asList(HuskyUtils.HCP, HuskyUtils.ASS, HuskyUtils.TCU)
        .contains(updatedDto.getAuthor().getRole());
    boolean shouldConfirm = ValidationUtils.shouldRecordBeValidated(oldDto, updatedDto) || isAuthorGFP;
    ValueDTO status = switch (updatedDto) {
      case VaccinationDTO v -> shouldConfirm
          ? new ValueDTO("59156000", "Confirmed", "http://snomed.info/sct")
          : new ValueDTO("76104008", "Not confirmed", "http://snomed.info/sct");
      case AllergyDTO a -> shouldConfirm
          ? new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
          : new ValueDTO("unconfirmed", "Unconfirmed", "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification");
      case PastIllnessDTO p -> shouldConfirm
          ? new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status")
          : new ValueDTO("unconfirmed", "Unconfirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status");
      case MedicalProblemDTO m -> shouldConfirm
          ? new ValueDTO("confirmed", "Confirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status")
          : new ValueDTO("unconfirmed", "Unconfirmed", "http://terminology.hl7.org/CodeSystem/condition-ver-status");
      default -> updatedDto.getVerificationStatus();
    };
    updatedDto.setVerificationStatus(status);
  }

  private List<EPRDocument> fetchDocuments(PatientIdentifier patientIdentifier, AuthorDTO author,
      Assertion assertion, boolean useInternal) {
    List<DocumentEntry> documentEntries =
        huskyAdapter.getDocumentEntries(patientIdentifier, author, assertion, useInternal);
    List<DocumentEntry> immunizationRecords = documentEntries.stream()
        .filter(doc -> doc.getFormatCode().getCode().equals(
            FhirConstants.COMPOSITION_IMMUNIZATON_CATEGORY.getFirst().getCoding().getFirst().getCode())).toList();

    if (!immunizationRecords.isEmpty()) {
      List<DocumentEntry> fhirImmunizationDocs = filterByMimeType(immunizationRecords);
      return createEPRDocuments(immunizationRecords, patientIdentifier, fhirImmunizationDocs, author, assertion, useInternal);
    }

    List<DocumentEntry> vaccinationRecords = documentEntries.stream()
        .filter(doc -> doc.getFormatCode().getCode().equals(
            FhirConstants.COMPOSITION_RECORD_CATEGORY.getFirst().getCoding().getFirst().getCode()))
        .max(Comparator.comparing(entry -> entry.getCreationTime().getDateTime().toInstant()))
        .map(Collections::singletonList)
        .orElse(Collections.emptyList());
    List<DocumentEntry> fhirVaccinations = filterByMimeType(vaccinationRecords);

    if (!vaccinationRecords.isEmpty()) {
      List<EPRDocument> eprDocuments = createEPRDocuments(vaccinationRecords, patientIdentifier, fhirVaccinations, author,
          assertion,useInternal);

      CacheIdentifierKey cacheIdentifier = cache.createCacheIdentifier(patientIdentifier);
      if (cache.dataCacheMiss(cacheIdentifier)) {
        for (EPRDocument doc : eprDocuments) {
          cache.putData(cacheIdentifier, doc, Cache.VACCINATION_RECORDS_CACHE_NAME);
        }
      }
    }
    return Collections.emptyList();
  }

  private List<DocumentEntry> filterByMimeType(List<DocumentEntry> documentEntries) {
    List<DocumentEntry> filteredFhirDocuments =
        documentEntries.stream().filter(doc -> doc.getMimeType().toLowerCase().contains("fhir")).toList();
    int numberOfFilteredEntries = documentEntries.size() - filteredFhirDocuments.size();
    if (numberOfFilteredEntries > 0) {
      log.debug("Filtered {} meta entries because of wrong mimetype, i.e. mimetype does not contain \"fhir\"",
          numberOfFilteredEntries);
    }

    return filteredFhirDocuments;
  }

  private List<EPRDocument> getData(PatientIdentifier patientIdentifier, Assertion assertion) {
    boolean patientIdentifierCouldNotBeResolved = patientIdentifier == null;
    if (patientIdentifierCouldNotBeResolved) {
      return Collections.emptyList();
    }

    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    CacheIdentifierKey cacheIdentifier = new CacheIdentifierKey(patientIdentifier, author);
    List<EPRDocument> eprDocuments = new ArrayList<>();

    if (cache.isEnabled() && !cache.dataCacheMiss(cacheIdentifier)) {
      log.debug(LOAD_DATA_FOR_FROM + " cache.", patientIdentifier.getPatientInfo().getFullName());
      return cache.getData(cacheIdentifier);
    }

    if (Boolean.FALSE.equals(profileConfig.getHuskyLocalMode())) {
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

    log.debug(LOAD_DATA_FOR_FROM + " Filesystem", patientIdentifier.getPatientInfo().getFullName());
    return fhirAdapter.getLocalEntities().stream().map(localEntity -> new EPRDocument(true, localEntity, null,
            JsonFieldExtractor.extractFieldAsLocalDateTime(localEntity, "timestamp")))
        .collect(Collectors.toList());
  }

  private String getDocumentData(RetrievedDocument retrievedDocument) {
    try {
      InputStream is = retrievedDocument.getDataHandler().getInputStream();
      byte[] bytesOfDocument = is.readAllBytes();

      return new String(bytesOfDocument, StandardCharsets.UTF_8);
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
    cache.putData(cache.createCacheIdentifier(patientIdentifier), createValidatedDocument(author, updatedJson));
    return parseBundle(patientIdentifier, newOrUpdatedBundle).getFirst();
  }

  private void handleRetrievedDocument(EPRDocument doc) {
    String document = getDocumentData(doc.getRetrievedDocument());
    doc.setJsonOrXmlFhirContent(document);
    doc.setCreationDate(JsonFieldExtractor.extractFieldAsLocalDateTime(document, "timestamp"));
  }

  @SuppressWarnings("unchecked")
  protected List<T> parseBundle(PatientIdentifier patientIdentifier, Bundle bundle) {
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
