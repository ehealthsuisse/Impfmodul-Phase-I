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
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.admin.bag.vaccination.service.husky.HuskyAdapterIfc;
import ch.fhir.epr.adapter.FhirAdapterIfc;
import ch.fhir.epr.adapter.config.FhirConfig;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
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

    Bundle createdBundle = fhirAdapter.create(patientIdentifier, newDTO);
    if (createdBundle != null) {
      String json = fhirAdapter.convertBundleToJson(createdBundle);
      huskyAdapter.writeDocument(patientIdentifier, getUuidFromBundle(createdBundle), json,
          newDTO.getAuthor(), newDTO.getConfidentiality(), assertion);
      cache.putData(patientIdentifier, json);
      return fhirAdapter.getDTOs(getDtoClass(), createdBundle).get(0);
    }

    throw new TechnicalException("no bundle was created, check server logs.");
  }

  @Override
  public T delete(String communityIdentifier, String oid,
      String localId, String toDeleteUuid, ValueDTO confidentiality, AuthorDTO author, Assertion assertion) {
    log.debug("delete {} {} {} {}", communityIdentifier, oid, localId, toDeleteUuid);
    PatientIdentifier patientIdentifier = getPatientIdentifier(communityIdentifier, oid, localId);

    List<String> jsons = getData(patientIdentifier, author, assertion);
    for (String json : jsons) {
      Bundle bundleToDelete = fhirAdapter.unmarshallFromString(json);

      T dto = fhirAdapter.getDTO(getDtoClass(), bundleToDelete, toDeleteUuid);
      if (dto != null) {
        if (confidentiality != null) {
          dto.setConfidentiality(confidentiality);
          dto.setAuthor(author);
        }

        Bundle deletedBundle = fhirAdapter.delete(patientIdentifier, dto, bundleToDelete, toDeleteUuid);
        if (deletedBundle != null) {
          String deletedJson = fhirAdapter.convertBundleToJson(deletedBundle);
          huskyAdapter.writeDocument(patientIdentifier, getUuidFromBundle(deletedBundle), deletedJson,
              author, dto.getConfidentiality(), assertion);
          cache.putData(patientIdentifier, deletedJson);
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
  public List<T> getAll(PatientIdentifier patientIdentifier, AuthorDTO author, Assertion assertion,
      boolean isLifecycleActive) {
    List<T> dtos = new ArrayList<>();
    List<String> jsons = getData(patientIdentifier, author, assertion);

    for (String json : jsons) {
      Bundle bundle = fhirAdapter.unmarshallFromString(json);
      List<T> jsonDtos = fhirAdapter.getDTOs(getDtoClass(), bundle);
      jsonDtos.forEach(dto -> dto.setJson(json));
      dtos.addAll(jsonDtos);
    }

    return (List<T>) lifeCycleService.handle(dtos, !isLifecycleActive);
  }

  @Override
  public List<T> getAll(String communityIdentifier,
      String oid, String localId, AuthorDTO author, Assertion assertion) {
    return getAll(communityIdentifier, oid, localId, author, assertion, true);
  }

  @Override
  public List<T> getAll(String communityIdentifier, String oid, String localId, AuthorDTO author, Assertion assertion,
      boolean isLifecycleActive) {
    PatientIdentifier patientIdentifier = getPatientIdentifier(communityIdentifier, oid, localId);

    return getAll(patientIdentifier, author, assertion, isLifecycleActive);
  }

  public PatientIdentifier getPatientIdentifier(String communityIdentifier, String oid, String localId) {
    return huskyAdapter.getPatientIdentifier(communityIdentifier, oid, localId);
  }

  @Override
  public T update(String communityIdentifier, String oid, String localId,
      String toUpdateUuid, T newDto, Assertion assertion) {
    log.debug("update {} {} {} {}", communityIdentifier, oid, localId, toUpdateUuid);
    PatientIdentifier patientIdentifier = getPatientIdentifier(communityIdentifier, oid, localId);

    List<String> jsons = getData(patientIdentifier, newDto.getAuthor(), assertion);
    for (String json : jsons) {
      Bundle bundleToUpdate = fhirAdapter.unmarshallFromString(json);

      T dto = fhirAdapter.getDTO(getDtoClass(), bundleToUpdate, toUpdateUuid);
      if (dto != null && toUpdateUuid.equals(dto.getId())) {
        Bundle updatedBundle =
            fhirAdapter.update(patientIdentifier, newDto, bundleToUpdate, toUpdateUuid);
        if (updatedBundle != null) {
          String updatedJson = fhirAdapter.convertBundleToJson(updatedBundle);
          huskyAdapter.writeDocument(patientIdentifier, getUuidFromBundle(updatedBundle), updatedJson,
              newDto.getAuthor(), dto.getConfidentiality(), assertion);
          cache.putData(patientIdentifier, updatedJson);
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

    if (!fhirConfig.isPractitioner(newDto.getAuthor().getRole())) {
      throw new TechnicalException("HCP or ASS role required!");
    }

    return update(communityIdentifier, oid, localId, toUpdateUuid, newDto, assertion);
  }

  protected abstract Class<T> getDtoClass();

  private List<String> getData(PatientIdentifier patientIdentifier, AuthorDTO author, Assertion assertion) {
    List<String> data = new ArrayList<>();

    if (!cache.dataCacheMiss(patientIdentifier)) {
      return cache.getData(patientIdentifier);
    }

    if (profileConfig.isLocalMode()) {
      data.addAll(fhirAdapter.getLocalEntities());
    } else {
      List<DocumentEntry> documentEntries =
          huskyAdapter.getDocumentEntries(patientIdentifier,
              vaccinationConfig.getFormatCodes(), vaccinationConfig.getDocumentType(), author, assertion);

      List<RetrievedDocument> retrievedDocuments =
          huskyAdapter.getRetrievedDocuments(patientIdentifier.getCommunityIdentifier(),
              documentEntries);

      for (RetrievedDocument retrievedDocument : retrievedDocuments) {
        handleRetrievedDocument(data, retrievedDocument);
      }
    }

    for (String json : data) {
      cache.putData(patientIdentifier, json);
    }
    return data;
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

  private String getUuidFromBundle(Bundle bundle) {
    return bundle.getIdentifier().getValue();
  }

  private void handleRetrievedDocument(List<String> data, RetrievedDocument retrievedDocument) {
    String document = getDocumentData(retrievedDocument);
    if (document != null) {
      data.add(document);
    }
  }
}
