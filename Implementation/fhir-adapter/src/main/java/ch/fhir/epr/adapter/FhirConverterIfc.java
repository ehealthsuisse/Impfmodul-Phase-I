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
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Practitioner;

/**
 *
 * Interface of the FhirConverter
 *
 */
public interface FhirConverterIfc {
  LocalDateTime convertToLocalDateTime(Date value);

  Bundle createBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto);

  Bundle createBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto,
      boolean forceImmunizationAdministrationDocument);

  /**
   * Resolves and applies a comment (FHIR {@link Annotation}) to the specified {@link DomainResource}
   * ({@link Immunization}, {@link AllergyIntolerance}, or {@link Condition}) and updates the provided {@code newBundle}.
   * <p>
   * This method is mainly used during create, update, and delete operations on FHIR resources.
   * <p>
   * The comment text is always sourced from the {@link CommentDTO} in the provided {@link BaseDTO}.
   * The author reference is resolved as follows:
   * <ul>
   *   <li>If the author is the patient, the patient reference (without the epr suffix) is taken from the {@code newBundle}.</li>
   *   <li>If the author is a practitioner and {@code bundleToBeUpdated} contains a previous comment:
   *     <ul>
   *       <li>If the practitioner is the same in both bundles, the practitioner reference (without the epr suffix) is taken from the {@code newBundle}.</li>
   *       <li>Otherwise, a new {@link org.hl7.fhir.r4.model.PractitionerRole} and {@link Practitioner} entry are created as needed.</li>
   *     </ul>
   *   </li>
   *   <li>If there is no previous comment or the comment text differs between the two comments, the practitioner reference (without the epr suffix) is taken from the {@code newBundle}.</li>
   * </ul>
   *
   * @param bundleToBeUpdated the old bundle that may contain a pre-existing comment and author reference
   * @param oldNote           the existing {@link Annotation} whose author reference may be reused
   * @param newBundle         the newly created bundle to which the new {@link Annotation} will be added
   * @param resource          the FHIR resource ({@link Immunization}, {@link AllergyIntolerance}, or {@link Condition}) to which the annotation will be attached
   * @param dto               the data transfer object ({@link BaseDTO}) containing the comment ({@link CommentDTO})
   * @param isPatientTheAuthor whether the patient is the author of the comment; if {@code false}, a practitioner reference is resolved or created as necessary
   */
  void resolveAndApplyComment(Bundle bundleToBeUpdated, Annotation oldNote, Bundle newBundle,
      DomainResource resource, BaseDTO dto, boolean isPatientTheAuthor);

  /**
   * Extracts a {@link CommentDTO} from the given FHIR {@link Bundle} and list of {@link Annotation} notes.
   * <p>
   * This method is typically called when querying the EPR (e.g., via {@code getDTOs}) to build DTOs for the frontend.
   * The resulting {@link CommentDTO} is then attached to the corresponding DTO, such as
   * {@link VaccinationDTO}, {@link MedicalProblemDTO}, {@link AllergyDTO}, or {@link PastIllnessDTO}.
   *
   * @param bundle the FHIR {@link Bundle} containing the resource data
   * @param notes  the list of {@link Annotation} notes from which the comment will be extracted
   * @return the extracted {@link CommentDTO}, or {@code null} if no comment is found
   */
  CommentDTO extractCommentFromBundle(Bundle bundle, List<Annotation> notes);

  Bundle deleteBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto,
      Composition composition, DomainResource resource);

  AllergyDTO toAllergyDTO(AllergyIntolerance allergyIntolerance, Practitioner practitioner, String organization);

  MedicalProblemDTO toMedicalProblemDTO(Condition condition, Practitioner practitioner, String organization);

  PastIllnessDTO toPastIllnessDTO(Condition condition, Practitioner practitioner, String organization);

  VaccinationDTO toVaccinationDTO(Immunization immunization, Practitioner practitioner, String organization);

  Bundle updateBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto, Composition composition,
      DomainResource resource);
}
