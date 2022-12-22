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
package ch.fhir.epr.adapter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ch.fhir.epr.adapter.config.FhirConfig;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Adapter of the {@link IParser} library.
 * <ul>
 * <li>{@link Bundle} is parsed as xml</li>
 * </ul>
 *
 */
@Service
@Slf4j
public class FhirAdapter implements FhirAdapterIfc {
  public static final String CONFIG_TESTFILES_JSON = "config/testfiles/json/";

  private FhirContext fhirContext;

  @Autowired
  private FhirConfig fhirConfig;
  @Autowired
  private FhirConverterIfc fhirConverter;

  @Override
  public String convertBundleToJson(Bundle createdBundle) {
    return getFhirContext().newJsonParser()
        .setPrettyPrint(true).encodeResourceToString(createdBundle);
  }

  @Override
  public Bundle create(PatientIdentifier patientIdentifier, BaseDTO dto) {
    try {
      FhirContext ctx = getFhirContext();
      return fhirConverter.createBundle(ctx, patientIdentifier, dto);
    } catch (Exception e) {
      log.warn("Exception:{}", e);
      throw new TechnicalException(e.getMessage());
    }
  }

  @Override
  public Bundle delete(PatientIdentifier patientIdentifier, BaseDTO dto, Bundle deletedBundle) {

    try {
      @SuppressWarnings("unchecked")
      Class<DomainResource> resourceType = (Class<DomainResource>) getResourceType(dto.getClass());
      DomainResource resource = FhirUtils.getResource(resourceType, deletedBundle);

      FhirContext ctx = getFhirContext();
      Composition composition = FhirUtils.getResource(Composition.class, deletedBundle);

      return fhirConverter.deleteBundle(ctx, patientIdentifier, dto, composition, resource);
    } catch (Exception e) {
      log.warn("Exception:{}", e);
      throw new TechnicalException(e.getMessage());
    }
  }

  @Override
  public <T extends BaseDTO> T getDTO(Class<T> clazz, Bundle bundle, String id) {
    List<T> dtos = getDTOs(clazz, bundle);
    for (T dto : dtos) {
      if (id.equals(dto.getId())) {
        return dto;
      }
    }

    return null;
  }

  @Override
  public <T extends BaseDTO> List<T> getDTOs(Class<T> clazz, Bundle bundle) {
    List<T> dtos = new ArrayList<>();
    if (bundle == null) {
      log.warn("bundle is null.");
      return dtos;
    }
    if (FhirUtils.isVaccinationRecord(bundle)) {
      log.warn("profile is VaccinationRecord");
      return dtos;
    }

    Class<?> type = getResourceType(clazz);

    try {
      for (BundleEntryComponent entry : bundle.getEntry()) {
        if (type.isInstance(entry.getResource())) {
          Object resource = type.cast(entry.getResource());
          dtos.add(parseFhirResource(clazz, bundle, resource));
        }
      }
    } catch (Exception e) {
      log.warn("Exception:{}", e);
      throw new TechnicalException(type + " not found");
    }

    return dtos;
  }

  @Override
  public List<String> getLocalEntities() {
    List<String> localJsonFileContent = new ArrayList<>();

    if (!fhirConfig.getTestCases().isEmpty()) {
      for (String testCase : fhirConfig.getTestCases()) {
        String json = jsonFromFile(testCase);
        if (json != null) {
          localJsonFileContent.add(json);
        }
      }
    }

    List<String> jsonFilenames = getJsonFilenames(CONFIG_TESTFILES_JSON);
    for (String jsonFilename : jsonFilenames) {
      String json = jsonFromFile(CONFIG_TESTFILES_JSON + "/" + jsonFilename);
      if (json != null) {
        localJsonFileContent.add(json);
      }
    }

    return localJsonFileContent;
  }

  public String marshall(Bundle bundle) {
    try {
      FhirContext ctx = getFhirContext();
      String json = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
      log.debug("Json:{}", json);
      return json;
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }

  /**
   * Unmarshall a json into a {@link Bundle}
   *
   * @param filename the json file
   * @return The {@link Bundle}
   */
  public Bundle unmarshallFromFile(String filename) {
    try {
      return unmarshallFromFile(filename, new FileReader(filename));
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }

  @Override
  public Bundle unmarshallFromString(String json) {
    try {
      FhirContext ctx = getFhirContext();
      boolean isJson = json.trim().startsWith("{");
      IParser parser = isJson ? ctx.newJsonParser() : ctx.newXmlParser();

      Bundle parsed = parser.parseResource(Bundle.class, json);
      log.debug("unmarshall -> Bundle:{}",
          ToStringBuilder.reflectionToString(parsed, ToStringStyle.JSON_STYLE));
      return parsed;
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }

  @Override
  public Bundle update(PatientIdentifier patientIdentifier, BaseDTO dto, Bundle updatedBundle) {
    try {
      @SuppressWarnings("unchecked")
      Class<DomainResource> resourceType = (Class<DomainResource>) getResourceType(dto.getClass());
      DomainResource resource = FhirUtils.getResource(resourceType, updatedBundle);

      FhirContext ctx = getFhirContext();
      Composition composition = FhirUtils.getResource(Composition.class, updatedBundle);

      Bundle bundle = fhirConverter.updateBundle(ctx, patientIdentifier, dto, composition, resource);
      fhirConverter.copyNotes(bundle, updatedBundle, resourceType);
      return bundle;
    } catch (Exception e) {
      log.warn("Exception:{}", e);
      throw new TechnicalException(e.getMessage());
    }
  }

  /**
   * Gets the {@link AllergyIntolerance} of a Bundle.
   *
   * @param bundle The {@link Bundle}
   * @return The {@link AllergyIntolerance}
   */
  protected AllergyIntolerance getAllergyIntolerance(Bundle bundle) {
    AllergyIntolerance allergyIntolerance = FhirUtils.getResource(AllergyIntolerance.class, bundle);
    return allergyIntolerance;
  }

  /**
   * Gets the {@link Immunization} of a Bundle.
   *
   * @param bundle The {@link Bundle}
   * @return The {@link Immunization}
   */
  protected Immunization getImmunization(Bundle bundle) {
    Immunization immunization = FhirUtils.getResource(Immunization.class, bundle);
    return immunization;
  }

  /**
   * Gets the list of the json files defining a Bundle
   *
   * @return the list of json files.
   */
  protected List<String> getJsonFilenames(String path) {
    List<String> jsonFilenames = new ArrayList<>();
    File folder = new File(path);
    File[] listOfFiles = folder.listFiles();
    for (int i = 0; i < listOfFiles.length; i++) {
      String filename = listOfFiles[i].getName();
      if (filename.endsWith(".json")) {
        log.debug("{}", filename);
        jsonFilenames.add(filename);
      }
    }
    return jsonFilenames;
  }

  protected Organization getOrganization(Bundle bundle, String id) {
    return FhirUtils.getResource(Organization.class, bundle, id);
  }

  protected PastIllnessDTO getPastIllnessDTO(Bundle bundle, Condition condition) {
    Practitioner practitioner = null;
    String organization = null;
    PractitionerRole practitionerRole =
        FhirUtils.getPractitionerRole(bundle, condition.getRecorder().getReference());
    if (practitionerRole != null) {
      practitioner =
          FhirUtils.getPractitioner(bundle, practitionerRole.getPractitioner().getReference());
      organization = practitionerRole.hasOrganization()
          ? getOrganization(bundle, practitionerRole.getOrganization().getReference()).getName()
          : null;
    }

    PastIllnessDTO dto = fhirConverter.toPastIllnessDTO(condition, practitioner, organization);
    dto.setComments(fhirConverter.createComments(bundle, condition.getNote()));
    return dto;
  }

  protected Bundle unmarshallFromFile(File file) {
    try {
      return unmarshallFromFile(file.getName(), new FileReader(file));
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }

  private AllergyDTO getAllergyDTO(Bundle bundle, AllergyIntolerance allergyIntolerance) {
    Practitioner practitioner = null;
    String organization = null;
    if (allergyIntolerance.getRecorder() != null) {
      PractitionerRole practitionerRole =
          FhirUtils.getPractitionerRole(bundle, allergyIntolerance.getRecorder().getReference());
      if (practitionerRole != null) {
        practitioner = FhirUtils.getPractitioner(bundle, practitionerRole.getPractitioner().getReference());
        organization = practitionerRole.hasOrganization()
            ? getOrganization(bundle, practitionerRole.getOrganization().getReference()).getName()
            : null;
      }
    }

    AllergyDTO dto = fhirConverter.toAllergyDTO(allergyIntolerance, practitioner, organization);
    dto.setComments(fhirConverter.createComments(bundle, allergyIntolerance.getNote()));
    return dto;
  }

  private FhirContext getFhirContext() {
    if (fhirContext == null) {
      fhirContext = FhirContext.forR4();
    }

    return fhirContext;
  }

  private <T extends BaseDTO> Class<?> getResourceType(Class<T> clazz) {
    Class<?> type = null;
    if (clazz.equals(VaccinationDTO.class)) {
      type = Immunization.class;
    } else if (clazz.equals(PastIllnessDTO.class)) {
      type = Condition.class;
    } else if (clazz.equals(AllergyDTO.class)) {
      type = AllergyIntolerance.class;
    } else {
      log.warn("getDTOs {} not supported", clazz.getSimpleName());
    }
    return type;
  }

  private VaccinationDTO getVaccinationDTO(Bundle bundle, Immunization immunization) {
    PractitionerRole practitionerRole =
        FhirUtils.getPractitionerRole(bundle, immunization.getPerformerFirstRep().getActor().getReference());
    String organization = practitionerRole.hasOrganization()
        ? getOrganization(bundle, practitionerRole.getOrganization().getReference()).getName()
        : null;
    Practitioner practitioner =
        FhirUtils.getPractitioner(bundle, practitionerRole.getPractitioner().getReference());

    Composition composition = FhirUtils.getResource(Composition.class, bundle);
    boolean validated = true;
    Patient patient = FhirUtils.getPatient(bundle, composition.getAuthorFirstRep().getReference());
    if (patient != null) {
      validated = false;
    }

    VaccinationDTO dto = fhirConverter.toVaccinationDTO(immunization, practitioner, organization, validated);
    dto.setComments(fhirConverter.createComments(bundle, immunization.getNote()));
    return dto;
  }

  private String jsonFromFile(String filename) {
    try {
      return new String(Files.readAllBytes(Paths.get(filename)));
    } catch (Exception e) {
      log.warn("Exception:{}", e.getMessage());
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends BaseDTO> T parseFhirResource(Class<T> clazz, Bundle bundle, Object resource) {
    T result;
    if (resource instanceof Immunization immunization) {
      result = (T) getVaccinationDTO(bundle, immunization);
    } else if (resource instanceof AllergyIntolerance allergy) {
      result = (T) getAllergyDTO(bundle, allergy);
    } else if (resource instanceof Condition condition) {
      result = (T) getPastIllnessDTO(bundle, condition);
    } else {
      throw new TechnicalException("Resource not supported");
    }

    result.setCreatedAt(fhirConverter.convertToLocalDateTime(bundle.getTimestamp()));
    result.setAuthor(FhirUtils.getAuthor(bundle));
    return result;
  }

  private Bundle unmarshallFromFile(String filename, FileReader fileReader) {
    try {
      FhirContext ctx = getFhirContext();
      IParser parser = ctx.newJsonParser();

      Bundle parsed = parser.parseResource(Bundle.class, fileReader);
      log.debug("unmarshall {} -> Bundle:{}", filename,
          ToStringBuilder.reflectionToString(parsed, ToStringStyle.JSON_STYLE));
      return parsed;
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }

}
