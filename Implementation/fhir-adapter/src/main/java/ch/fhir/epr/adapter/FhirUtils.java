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

import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
@NoArgsConstructor
public final class FhirUtils {

  public static SectionComponent getSectionByType(Composition composition, SectionType type) {
    for (SectionComponent sectionComponent : composition.getSection()) {
      boolean isSameId = type.getId().equals(sectionComponent.getId());
      boolean isSameCode = type.getCode().equals(sectionComponent.getCode().getCodingFirstRep().getCode());
      if (isSameId || isSameCode) {
        return sectionComponent;
      }
    }

    return null;
  }

  public static String getUuidFromBundle(Bundle bundle) {
    return bundle != null && bundle.getIdentifier() != null ? bundle.getIdentifier().getValue() : null;
  }

  public static CodeableConcept replaceLegacyTargetDiseaseCoding(CodeableConcept codeableConcept) {
    Coding coding = codeableConcept.getCoding().getFirst();
    String code = coding.getCode();
    String display = coding.getDisplay();
    if (FhirConstants.LEGACY_TARGET_DISEASE_CODE.equals(code)
        && FhirConstants.LEGACY_TARGET_DISEASE_DISPLAY.equals(display)) {
      coding.setCode(FhirConstants.CURRENT_TARGET_DISEASE_CODE);
      coding.setDisplay(FhirConstants.CURRENT_TARGET_DISEASE_DISPLAY);
    }
    return codeableConcept;
  }

  static AuthorDTO getAuthor(Bundle bundle) {
    Composition composition = getResource(Composition.class, bundle);
    if (composition == null) {
      return null;
    }
    Reference author = composition.getAuthorFirstRep();
    if (author == null) {
      return null;
    }

    return getAuthor(bundle, author.getReference());
  }

  static AuthorDTO getAuthor(Bundle bundle, String id) {
    if (id == null || bundle == null) {
      return null;
    }
    Patient patient = getPatient(bundle, id);
    if (patient != null) {
      return new AuthorDTO(toHumanNameDTO(patient), null, "PAT", null, null, null, null);
    }

    PractitionerRole practitionerRole = getPractitionerRole(bundle, id);
    if (practitionerRole != null) {
      Practitioner practitioner = getPractitioner(bundle, practitionerRole.getPractitioner().getReference());
      return new AuthorDTO(toHumanNameDTO(practitioner), null, "HCP", null,
          practitioner.getIdentifierFirstRep().getValue(), null, null);
    }

    Practitioner practitioner = getPractitioner(bundle, id);
    if (practitioner != null) {
      return new AuthorDTO(toHumanNameDTO(practitioner), null, "HCP", null,
          practitioner.getIdentifierFirstRep().getValue(), null, null);
    }
    log.warn("getAuthor {} not found!", id);
    return null;
  }

  static ValueDTO getConfidentiality(Bundle bundle) {
    Composition composition = FhirUtils.getResource(Composition.class, bundle);
    Extension extension = composition.getConfidentialityElement().getExtensionFirstRep();
    return toValueDTO((CodeableConcept) extension.getValue());
  }

  static String getIdentifierValue(Resource resource) {
    String result = null;
    if (resource instanceof Immunization vaccination) {
      result = vaccination.getIdentifierFirstRep().getValue();
    } else if (resource instanceof Condition condition) {
      result = condition.getIdentifierFirstRep().getValue();
    } else if (resource instanceof AllergyIntolerance allergy) {
      result = allergy.getIdentifierFirstRep().getValue();
    } else if (resource instanceof Patient patient) {
      result = patient.getIdentifierFirstRep().getValue();
    } else if (resource instanceof Practitioner practitioner) {
      result = practitioner.getIdentifierFirstRep().getValue();
    } else if (resource instanceof Composition composition) {
      result = composition.getIdentifier().getValue();
    }
    return result;
  }

  static Organization getOrganization(Bundle bundle, String id) {
    return getResource(Organization.class, bundle, id);
  }

  static Patient getPatient(Bundle bundle, String id) {
    return getResource(Patient.class, bundle, id);
  }

  static Practitioner getPractitioner(Bundle bundle, String id) {
    return getResource(Practitioner.class, bundle, id);
  }

  static PractitionerRole getPractitionerRole(Bundle bundle, String id) {
    return getResource(PractitionerRole.class, bundle, id);
  }

  static Resource getResource(Bundle bundle, String identifier) {
    if (bundle == null) {
      log.warn("getResource bundle null");
      return null;
    }
    try {
      for (BundleEntryComponent entry : bundle.getEntry()) {
        Resource resource = entry.getResource();

        String bundleIdentifier = getIdentifierValue(resource);
        if (bundleIdentifier != null) {
          bundleIdentifier = bundleIdentifier.replace(FhirConstants.DEFAULT_ID_PREFIX, "");
        }

        if (identifier.equals(bundleIdentifier)) {
          log.debug("getResource: id={} found {}", identifier,
              ToStringBuilder.reflectionToString(resource, ToStringStyle.JSON_STYLE));
          return resource;
        }
      }
      log.warn("getResource: identifier={} not found", identifier);
      return null;
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }

  static <T extends DomainResource> T getResource(Class<T> type, Bundle bundle) {
    if (bundle == null) {
      log.warn("getResource bundle null");
      return null;
    }
    try {
      for (BundleEntryComponent entry : bundle.getEntry()) {
        if (type.isInstance(entry.getResource())) {
          T resource = type.cast(entry.getResource());
          log.debug("Ressource:{} found {}", type, resource);
          return resource;
        }
      }
      log.debug("Ressource:{} not found", type);
      return null;
    } catch (Exception e) {
      log.warn("Exception:{}", e);
      throw new TechnicalException(type + " not found");
    }
  }

  static <T> T getResource(Class<T> type, Bundle bundle, String id) {
    if (bundle == null) {
      log.warn("getResource bundle null");
      return null;
    }
    try {
      for (BundleEntryComponent entry : bundle.getEntry()) {
        if (type.isInstance(entry.getResource())) {
          Resource resource = Resource.class.cast(entry.getResource());
          if (resource.getId() != null && //
              (resource.getId().endsWith(id) || id.endsWith(resource.getId()))) { // FIXME
            log.debug("Ressource:{} id={} found {}", type, id,
                ToStringBuilder.reflectionToString(resource, ToStringStyle.JSON_STYLE));
            return type.cast(entry.getResource());
          }
        }
      }
      log.debug("Ressource:{} id={} not found", type, id);
      return null;
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }

  static String getResourceType(Resource resource) {
    String result = null;
    if (resource instanceof Immunization) {
      result = "Immunization";
    } else if (resource instanceof Condition) {
      result = "Condition";
    } else if (resource instanceof AllergyIntolerance) {
      result = "AllergyIntolerance";
    } else if (resource instanceof Patient) {
      result = "Patient";
    } else if (resource instanceof Practitioner) {
      result = "Practitioner";
    } else if (resource instanceof Composition) {
      result = "Composition";
    }
    return result;
  }

  static boolean isVaccinationRecord(Bundle bundle) {
    if ((bundle == null) || (bundle.getMeta() == null) || bundle.getMeta().getProfile() == null
        || bundle.getMeta().getProfile().isEmpty()) {
      return false;
    }
    if (FhirConstants.META_VACCINATION_RECORD_TYPE_URL.equals(bundle.getMeta().getProfile().getFirst().getValue())) {
      return true;
    }
    return false;
  }

  static CodeableConcept toCodeableConcept(ValueDTO valueDTO) {
    if (valueDTO == null) {
      log.warn("valueDTO is null!");
      return null;
    }
    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.getCodingFirstRep().setCode(valueDTO.getCode());
    codeableConcept.getCodingFirstRep().setDisplay(valueDTO.getName());
    codeableConcept.getCodingFirstRep().setSystem(valueDTO.getSystem());
    return codeableConcept;
  }

  static HumanNameDTO toHumanNameDTO(Patient patient) {
    if (patient == null) {
      return null;
    }

    HumanName humanName = patient.getNameFirstRep();
    return toHumanNameDTO(humanName);
  }

  static HumanNameDTO toHumanNameDTO(Practitioner practitioner) {
    if (practitioner == null) {
      return null;
    }

    HumanName humanName = practitioner.getNameFirstRep();
    return toHumanNameDTO(humanName);
  }

  static ValueDTO toValueDTO(CodeableConcept concept) {
    if (concept == null) {
      log.warn("concept is null!");
      return null;
    }
    return new ValueDTO( //
        concept.getCodingFirstRep().getCode(), //
        concept.getCodingFirstRep().getDisplay(), //
        concept.getCodingFirstRep().getSystem()); //
  }

  private static HumanNameDTO toHumanNameDTO(HumanName humanName) {
    if (humanName == null) {
      return null;
    }

    String lastName = humanName.getFamily();
    String firstName = humanName.getGivenAsSingleString();
    String prefix = humanName.getPrefixAsSingleString();

    return new HumanNameDTO(firstName, lastName, prefix, null, null);
  }
}
