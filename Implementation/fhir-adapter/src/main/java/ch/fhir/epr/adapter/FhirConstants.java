/**
 * Copyright (c) 2024 eHealth Suisse
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

import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.CodeableConcept;

import ch.fhir.epr.adapter.data.dto.ValueDTO;
import lombok.NoArgsConstructor;

/**
 * Constants class aiming to centralize fixed values.
 */
@NoArgsConstructor
public final class FhirConstants {

  /** used for general UUID identifiers */
  public static final String DEFAULT_URN_SYSTEM_IDENTIFIER = "urn:ietf:rfc:3986";
  public static final String DEFAULT_ID_PREFIX = "urn:uuid:";
  public static final String PATIENT_SYSTEM_URL_PREFIX = "urn:oid:";

  /** Fixed system url defined in the standard */
  public static final String FIXED_PRACTITIONER_SYSTEM = "urn:oid:2.51.1.3";
  /** Used if practitioner GLN is unknown */
  public static final String DEFAULT_PRACTITIONER_CODE = "7601007922000";

  /** various URLs used for meta information */
  public static final String META_VACCINATION_RECORD_TYPE_URL =
      "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-document-vaccination-record";
  public static final String META_VACCINATION_TYPE_URL =
      "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-document-immunization-administration";
  public static final String META_CORE_PATIENT_URL =
      "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-patient-epr";
  public static final String META_CORE_PRACTITIONER_URL =
      "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-practitioner-epr";
  public static final String META_PRACTITIONER_ROLE_URL =
      "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-practitionerrole-epr";
  public static final String META_VACD_IMMUNIZATION_URL =
      "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-immunization";
  public static final String META_ORGANIZATION_URL =
      "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-organization-epr";
  public static final String META_VACD_MEDICAL_PROBLEMS_URL =
      "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-medical-problems";
  public static final String META_VACD_COMPOSITION_VAC_REC_URL =
      "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-composition-vaccination-record";
  public static final String META_VACD_COMPOSITION_IMMUN_URL =
      "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-composition-immunization-administration";

  /** various extension URLs */
  public static final String CONFIDENTIALITY_CODE_EXTENSION_URL =
      "http://fhir.ch/ig/ch-core/StructureDefinition/ch-ext-epr-confidentialitycode";

  /** Constants around confidentiality code */
  public static final String CONFIDENTIALITY_CODE_SYSTEM_NORMAL_RESTRICTED = "2.16.840.1.113883.6.96";
  public static final String CONFIDENTIALITY_CODE_SYSTEM_SECRET = "2.16.756.5.30.1.127.3.4";
  public static final ValueDTO DEFAULT_CONFIDENTIALITY_CODE =
      new ValueDTO("17621005", "Normal", CONFIDENTIALITY_CODE_SYSTEM_NORMAL_RESTRICTED);
  public static final String SNOMED_SYSTEM_URL = "http://snomed.info/sct";

  /** Status constants */
  public static final String ENTERED_IN_ERROR = "entered-in-error";

  /** Legacy Target Disease codes */
  public static final String LEGACY_TARGET_DISEASE_CODE = "16901001";
  public static final String LEGACY_TARGET_DISEASE_DISPLAY = "Central European encephalitis";

  /** Current Target Disease codes */
  public static final String CURRENT_TARGET_DISEASE_CODE = "712986001";
  public static final String CURRENT_TARGET_DISEASE_DISPLAY = "Tickborne encephalitis";

  /** Composition Category */
  public static final List<CodeableConcept> COMPOSITION_IMMUNIZATON_CATEGORY = List.of(Objects.requireNonNull(
    FhirUtils.toCodeableConcept(new ValueDTO("urn:che:epr:ch-vacd:immunization-administration:2022",
      "CH VACD Immunization Administration",
      "urn:oid:2.16.756.5.30.1.127.3.10.10"))));
  public static final List<CodeableConcept> COMPOSITION_RECORD_CATEGORY = List.of(Objects.requireNonNull(
    FhirUtils.toCodeableConcept(new ValueDTO("urn:che:epr:ch-vacd:vaccination-record:2022",
      "CH VACD Vaccination Record",
      "urn:oid:2.16.756.5.30.1.127.3.10.10"))));

  /** Author roles */
  public static final String HCP = "HCP";
  public static final String ASS = "ASS";
  public static final String TCU = "TCU";
}
