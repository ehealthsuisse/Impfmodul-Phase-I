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

import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;

/**
 * Service based on the HAPI-FHIR library
 *
 */
public interface FhirAdapterIfc {

  public String convertBundleToJson(Bundle createdBundle);

  /**
   * Create a json for creation based on the HAPI/FHIR format.
   *
   * @param patientIdentifier {@link PatientIdentifier}
   * @param dto entity like vaccination, allergy, past-illness, medical-problem or VaccinationRecord
   * @return {@link Bundle}
   */
  public Bundle create(PatientIdentifier patientIdentifier, BaseDTO dto);

  /**
   * Create a json from deletion based on the HAPI/FHIR format.
   *
   * @param patientIdentifier {@link PatientIdentifier}
   * @param dto entity like vaccination, allergy, medical-problem or past illness
   * @param bundleToDelete the bundle which contains the entry to delete
   * @param uuid the {@link Identifier} of the item
   * 
   * @return deleted bundle
   */
  public Bundle delete(PatientIdentifier patientIdentifier, BaseDTO dto, Bundle bundleToDelete, String uuid);

  /**
   * Convert a unique entry within a bundle to the DTO.
   *
   * @param clazz the requested object class
   * @param bundle {@link Bundle}
   * @param uuid the uuid of the entry to search (not the UUID of the bundle!)
   *
   * @return the DTO
   */
  public <T extends BaseDTO> T getDTO(Class<T> clazz, Bundle bundle, String uuid);

  /**
   * Convert a bundle to DTOs belonging to a specific class.
   *
   * @param clazz the requested object class
   * @param bundle {@link Bundle}
   * @return the DTOs
   */
  public <T extends BaseDTO> List<T> getDTOs(Class<T> clazz, Bundle bundle);

  /**
   * Create a bundle by a file content.
   *
   * @param fileContent
   * @return {@link Bundle}
   */
  public Bundle unmarshallFromString(String fileContent);

  /**
   * Create a new bundle which updates the existing one.
   *
   * @param patientIdentifier {@link PatientIdentifier}
   * @param dto entity like vaccination, allergy, medical-problem or past illness
   * @param bundleToUpdate the bundle which contains the entry to update
   * @param uuid the {@link Identifier} of the item
   * 
   * @return {@link Bundle}
   */
  public Bundle update(PatientIdentifier patientIdentifier, BaseDTO dto, Bundle bundleToUpdate, String uuid);

  /**
   * Gets the local stored jsons (vaccinations, allergies, pastillnesses).
   *
   * @return the list of json content
   */
  List<String> getLocalEntities();

}
