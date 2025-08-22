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
import ch.admin.bag.vaccination.service.husky.HuskyAdapterIfc;
import ch.fhir.epr.adapter.FhirAdapterIfc;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.projecthusky.xua.saml2.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * VaccinationRecordService contains all the business logic to handle a {@link VaccinationRecordDTO}
 * or rather the list of vaccinations, allergies and past illnesses.
 *
 */
@Slf4j
@Service
public class VaccinationRecordService {
  private static final String LOAD_DATA_FOR_FROM = "Load data for {} from";
  private final Map<Integer, PatientIdentifier> patientIdentifierByHash = new ConcurrentHashMap<>();

  @Autowired
  protected HuskyAdapterIfc huskyAdapter;

  @Autowired
  protected FhirAdapterIfc fhirAdapter;

  @Autowired
  private BaseService<BaseDTO> baseService;

  @Autowired
  private ProfileConfig profileConfig;

  @Autowired
  private Cache cache;

  /**
   * Converts a {@link VaccinationRecordDTO} object to an Immunization Administration document
   * <p>
   * This method ensures thread-safe processing by synchronizing on a unique identifier (patients full-name)
   * associated with the given {@link PatientIdentifier}. The method follows these steps:
   * <ul>
   *   <li>Retrieves cached documents {@link EPRDocument} with the following format code <code>urn:che:epr:ch-vacd:vaccination-record:2022</code> related to the patient.</li>
   *   <li>If data is available, a {@link VaccinationRecordDTO} is created and is written to EPD by forcing the conversion to an Immunization Administration document.</li>
   *   <li>If no documents are found, it attempts to call EPD to see if any data is present there.</li>
   *   <li>If data is retrieved, a {@link VaccinationRecordDTO} is created an returned to the frontend.</li>
   *   <li>If no data is retrieved, the cache will be queried next and also delete the records (if there are any) for subsequent calls.</li>
   *   <li>If data is available, a {@link VaccinationRecordDTO} is created and is written to EPD by forcing the conversion to an Immunization Administration document.</li>
   *   <li>If no data is found at any stage, an empty {@link VaccinationRecordDTO} is returned.</li>
   * </ul>
   *
   * @param patientIdentifier the PatientIdentifier
   * @param assertion {@link Assertion}
   * @return {@link VaccinationRecordDTO}
   * @throws TechnicalException If the retrieved bundle cannot be parsed.
   */
  public VaccinationRecordDTO convertVaccinationToImmunization(PatientIdentifier patientIdentifier, Assertion assertion) {
    PatientIdentifier patientIdentifierLock = patientIdentifierByHash
        .computeIfAbsent(patientIdentifier.hashCode(), key -> patientIdentifier);
    synchronized (patientIdentifierLock) {
      log.debug(LOAD_DATA_FOR_FROM + " cache.", patientIdentifier.getPatientInfo().getFullName());
      List<EPRDocument> vaccinationRecords = cache.getData(cache.createCacheIdentifier(patientIdentifier),
          Cache.VACCINATION_RECORDS_CACHE_NAME);
      if (!vaccinationRecords.isEmpty()) {
        return convertAndWriteDocumentToEPD(patientIdentifier, assertion, vaccinationRecords);
      }

      List<BaseDTO> result = baseService.getAll(patientIdentifier, assertion, true);
      if (!result.isEmpty()) {
        return createVaccinationRecordDTO(patientIdentifier, result, createAuthorDTO(), LocalDateTime.now());
      }

      vaccinationRecords = cache.getData(cache.createCacheIdentifier(patientIdentifier),
          Cache.VACCINATION_RECORDS_CACHE_NAME);
      if (vaccinationRecords.isEmpty()) {
        return createEmptyVaccinationRecordDTO(patientIdentifier);
      }

      return convertAndWriteDocumentToEPD(patientIdentifier, assertion, vaccinationRecords);
    }
  }

  private VaccinationRecordDTO convertAndWriteDocumentToEPD(PatientIdentifier patientIdentifier, Assertion assertion,
      List<EPRDocument> eprDocuments) {
    Bundle bundle = fhirAdapter.unmarshallFromString(eprDocuments.getFirst().getJsonOrXmlFhirContent());
    if (bundle == null) {
      throw new TechnicalException("Conversion to Immunization Administration document failed. "
          + "Bundle can't be parsed.");
    }

    List<BaseDTO> entities = baseService.parseBundle(patientIdentifier, bundle);
    if (!entities.isEmpty()) {
      VaccinationRecordDTO vaccinationRecordDTO = createVaccinationRecordDTO(patientIdentifier, entities,
          HttpSessionUtils.getAuthorFromSession(), LocalDateTime.now());
      baseService.create(patientIdentifier.getCommunityIdentifier(),
          patientIdentifier.getLocalAssigningAuthority(), patientIdentifier.getLocalExtenstion(), vaccinationRecordDTO,
          assertion, true);
      cache.clear(cache.createCacheIdentifier(patientIdentifier), Cache.VACCINATION_RECORDS_CACHE_NAME);
      return vaccinationRecordDTO;
    }
    return createEmptyVaccinationRecordDTO(patientIdentifier);
  }

  /**
   * Creates a {@link VaccinationRecordDTO} based on the given identifier and assertion.
   * <p>
   * The method first tries to retrieve a list of Immunization Administration documents.
   * If no records are found, it performs an additional check to determine whether Vaccination Records are present in the cache.
   * </p>
   *
   * @param patientIdentifier The unique PatientIdentifier used to fetch records.
   * @param assertion  The assertion {@link Assertion} used for access control and validation.
   * @return {@link VaccinationRecordDTO}
   */
  public VaccinationRecordDTO create(PatientIdentifier patientIdentifier, Assertion assertion) {
    List<BaseDTO> immunizationAdmRecords = baseService.getAll(patientIdentifier, assertion, true);
    LocalDateTime creationDate = null;
    if (immunizationAdmRecords.isEmpty()) {
      List<EPRDocument> vaccinationRecord = cache.getData(cache.createCacheIdentifier(patientIdentifier),
          Cache.VACCINATION_RECORDS_CACHE_NAME);
      creationDate =
          vaccinationRecord != null && !vaccinationRecord.isEmpty() ? vaccinationRecord.getFirst().getCreationDate() : null;
    }

    return createVaccinationRecordDTO(patientIdentifier, immunizationAdmRecords, createAuthorDTO(), creationDate);
  }

  /**
   * Creates a vaccination record in the EPD containing the given vaccinations, allergies and past
   * illnesses.
   *
   * @param communityIdentifier EPD community to talk with
   * @param oid local assigning authority oid
   * @param localId local patient id
   * @param record {@link VaccinationRecordDTO} containing the entities to export
   * @param assertion IDP Assertion
   *
   * @return json containing the full bundle which was written.
   */
  public String create(String communityIdentifier, String oid, String localId, VaccinationRecordDTO record,
      Assertion assertion) {
    PatientIdentifier patientIdentifier = huskyAdapter.getPatientIdentifier(communityIdentifier, oid, localId);
    AuthorDTO author = HttpSessionUtils.getAuthorFromSession();
    record.setAuthor(author);

    Bundle bundle = fhirAdapter.create(patientIdentifier, record);
    String jsonToWrite = fhirAdapter.convertBundleToJson(bundle);

    huskyAdapter.writeDocument(patientIdentifier, bundle.getIdentifier().getValue(), jsonToWrite, record, assertion,
        true);

    return jsonToWrite;
  }

  private AuthorDTO createAuthorDTO() {
    return new AuthorDTO(new HumanNameDTO("generated by", "system", "document",
        LocalDate.of(1900, 1, 1), AdministrativeGender.UNKNOWN.name()));
  }

  private VaccinationRecordDTO createVaccinationRecordDTO(PatientIdentifier patientIdentifier,
      List<BaseDTO> entities, AuthorDTO createAuthor, LocalDateTime createdAt) {
    List<VaccinationDTO> vaccinations = entities.parallelStream().filter(entity -> entity instanceof VaccinationDTO)
        .map(entity -> (VaccinationDTO) entity).toList();
    List<AllergyDTO> allergies = entities.parallelStream().filter(entity -> entity instanceof AllergyDTO)
        .map(entity -> (AllergyDTO) entity).toList();
    List<MedicalProblemDTO> medicalProblems = entities.parallelStream()
        .filter(entity -> entity instanceof MedicalProblemDTO).map(entity -> (MedicalProblemDTO) entity).toList();
    List<PastIllnessDTO> pastIllnesses = entities.parallelStream().filter(entity -> entity instanceof PastIllnessDTO)
        .map(entity -> (PastIllnessDTO) entity).toList();

    VaccinationRecordDTO vaccinationRecordDTO = new VaccinationRecordDTO(createAuthor, patientIdentifier.getPatientInfo(),
        allergies, pastIllnesses, vaccinations, medicalProblems);
    vaccinationRecordDTO.setCreatedAt(createdAt);

    Bundle bundle = fhirAdapter.create(patientIdentifier, vaccinationRecordDTO);
    vaccinationRecordDTO.setJson(fhirAdapter.convertBundleToJson(bundle));

    return vaccinationRecordDTO;
  }

  private VaccinationRecordDTO createEmptyVaccinationRecordDTO(PatientIdentifier identifier) {
    log.debug("An empty VaccinationRecordDTO was created.");
    return createVaccinationRecordDTO(identifier, Collections.emptyList(), createAuthorDTO(), null);
  }
}
