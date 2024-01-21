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

import ch.fhir.epr.adapter.data.dto.ValueDTO;
import lombok.extern.slf4j.Slf4j;
import org.projecthusky.common.enums.ValueSetEnumInterface;
import org.projecthusky.common.model.Code;
import org.projecthusky.communication.ch.enums.beta.PurposeOfUse;
import org.projecthusky.communication.ch.enums.beta.Role;
import org.projecthusky.xua.hl7v3.CE;
import org.projecthusky.xua.hl7v3.impl.CodedWithEquivalentsBuilder;

/**
 * Utility class for Husky
 *
 */
@Slf4j
public class HuskyUtils {

  public static final Code IMMUNIZATION_ADMINISTRATION_DOCUMENT_FORMAT_CODE =
      new Code("urn:che:epr:ch-vacd:immunization-administration:2022",
          "2.16.756.5.30.1.127.3.10.10", "CH VACD Immunization Administration");
  public static final Code VACCINATION_RECORD_DOCUMENT_FORMAT_CODE =
      new Code("urn:che:epr:ch-vacd:vaccination-record:2022",
          "2.16.756.5.30.1.127.3.10.10", "CH VACD Vaccination Record");
  public static final Code IMMUNIZATION_TYPE_CODE =
      new Code("41000179103", "2.16.840.1.113883.6.96", "Immunization Record (record artifact)");
  public static final Code PATIENT_RECORD_CLASS_CODE =
      new Code("184216000", "2.16.840.1.113883.6.96", "Patient record type (record artifact)");
  public static final Code DEFAULT_CONTENT_TYPE_CODE =
      new Code("71388002", "2.16.840.1.113883.6.96", "Procedure (procedure)");
  public static final ValueDTO DEFAULT_CONFIDENTIALITY_CODE =
      new ValueDTO("17621005", "Normal", "2.16.840.1.113883.6.96");
  public static final ValueDTO RESTRICTED_CONFIDENTIALITY_CODE =
      new ValueDTO("263856008", "Restricted", "2.16.840.1.113883.6.96");
  public static final ValueDTO SECRET_CONFIDENTIALITY_CODE =
      new ValueDTO("1141000195107", "Secret", "2.16.756.5.30.1.127.3.4");
  public static final String HCP = "HCP";
  public static final String ASS = "ASS";
  public static final String TCU = "TCU";
  public static final String PAT = "PAT";
  public static final String UPLOADER_ROLE_METADATA_KEY = "urn:e-health-suisse:2020:originalProviderRole";



  /**
   * Get a PurposeOfUse as {@link CE}
   * <p>
   * https://fhir.ch/ig/ch-epr-term/CodeSystem-2.16.756.5.30.1.127.3.10.5.html
   *
   * @param code the code of the purposeOfUse
   * @return the PurposeOfUse
   */
  static public CE getCodedPurposeOfUse(String code) {
    PurposeOfUse purposeOfUse = PurposeOfUse.getEnum(code);
    if (purposeOfUse == null) {
      log.warn("PurposeOfUse {} not defined!", code);
      return null;
    }

    return getCodedWithEquivalent(purposeOfUse,
        org.projecthusky.xua.hl7v3.PurposeOfUse.DEFAULT_NS_URI,
        org.projecthusky.xua.hl7v3.PurposeOfUse.DEFAULT_ELEMENT_LOCAL_NAME,
        org.projecthusky.xua.hl7v3.PurposeOfUse.DEFAULT_PREFIX);
  }

  /**
   * Get a Role as {@link CE}
   * <p>
   * https://fhir.ch/ig/ch-epr-term/CodeSystem-2.16.756.5.30.1.127.3.10.6.html
   *
   * @param code the code of the role
   * @return the Role
   */
  static public CE getCodedRole(String code) {
    Role role = Role.getEnum(code);
    if (role == null) {
      log.warn("Role {} not defined!", code);
      return null;
    }

    return getCodedWithEquivalent(role,
        org.projecthusky.xua.hl7v3.Role.DEFAULT_NS_URI,
        org.projecthusky.xua.hl7v3.Role.DEFAULT_ELEMENT_LOCAL_NAME,
        org.projecthusky.xua.hl7v3.Role.DEFAULT_PREFIX);
  }


  static private CE getCodedWithEquivalent(ValueSetEnumInterface value, String nsURI, String elementLocalName,
      String prefix) {
    return new CodedWithEquivalentsBuilder()
        .code(value.getCodeValue())
        .codeSystem(value.getCodeSystemId())
        .displayName(value.getDisplayName(null))
        .buildObject(nsURI, elementLocalName, prefix);
  }

}

