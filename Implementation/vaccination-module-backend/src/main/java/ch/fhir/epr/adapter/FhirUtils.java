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

import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
public class FhirUtils {
  static public String VACCINATION_RECORD_TYPE_URL =
      "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-document-vaccination-record";
  static public String VACCINATION_TYPE_URL =
      "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-document-immunization-administration";
  static public String CONFIDENTIALITY_CODE_URL =
      "https://ehealthsuisse.art-decor.org/cdachemed-html-20220302T110630/voc-2.16.756.5.30.1.127.3.10.1.5-2021-04-01T170535.html";
  static public ValueDTO defaultConfidentialityCode = new ValueDTO("17621005", "Normal", CONFIDENTIALITY_CODE_URL);

  static HumanNameDTO getAuthor(Bundle bundle) {
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

  static HumanNameDTO getAuthor(Bundle bundle, String id) {
    if (id == null || bundle == null) {
      return null;
    }
    Patient patient = getPatient(bundle, id);
    if (patient != null) {
      return toHumanNameDTO(patient);
    }

    PractitionerRole practitionerRole = getPractitionerRole(bundle, id);
    if (practitionerRole != null) {
      Practitioner practitioner = getPractitioner(bundle, practitionerRole.getPractitioner().getReference());
      return toHumanNameDTO(practitioner);
    }

    Practitioner practitioner = getPractitioner(bundle, id);
    if (practitioner != null) {
      return toHumanNameDTO(practitioner);
    }
    log.warn("getAuthor {} not found!", id);
    return null;
  }

  static Patient getPatient(Bundle bundle, String id) {
    Patient patient = getResource(Patient.class, bundle, id);
    return patient;
  }

  static Practitioner getPractitioner(Bundle bundle, String id) {
    Practitioner practitioner = getResource(Practitioner.class, bundle, id);
    return practitioner;
  }

  static PractitionerRole getPractitionerRole(Bundle bundle, String id) {
    PractitionerRole practitionerRole = getResource(PractitionerRole.class, bundle, id);
    return practitionerRole;
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

  static boolean isVaccinationRecord(Bundle bundle) {
    if (bundle == null) {
      return false;
    }
    if (bundle.getMeta() == null) {
      return false;
    }
    if (bundle.getMeta().getProfile() == null || bundle.getMeta().getProfile().isEmpty()) {
      return false;
    }
    if (VACCINATION_RECORD_TYPE_URL.equals(bundle.getMeta().getProfile().get(0).getValue())) {
      return true;
    }
    return false;
  }

  static HumanNameDTO toHumanNameDTO(Patient patient) {
    if (patient == null) {
      return null;
    }

    HumanName humanName = patient.getNameFirstRep();
    return toHumanNameDTO(humanName, "PAT");
  }

  static HumanNameDTO toHumanNameDTO(Practitioner practitioner) {
    if (practitioner == null) {
      return null;
    }

    HumanName humanName = practitioner.getNameFirstRep();
    return toHumanNameDTO(humanName, "HCP");
  }

  private static HumanNameDTO toHumanNameDTO(HumanName humanName, String role) {
    if (humanName == null) {
      return null;
    }

    String lastName = humanName.getFamily();
    String firstName = humanName.getGivenAsSingleString();
    String prefix = humanName.getPrefixAsSingleString();

    return new HumanNameDTO(firstName, lastName, prefix, null, null, role, null);
  }
}
