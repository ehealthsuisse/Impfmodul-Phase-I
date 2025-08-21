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
   * Creates a {@link CommentDTO} by aggregating multiple {@link Annotation} notes.
   * <p>
   * The resulting {@code CommentDTO} uses the author and timestamp from the first note in the list.
   * The content text is composed as follows:
   * <ul>
   *   <li>The text of the first note appears first.</li>
   *   <li>For each subsequent note, an empty line separates it from the previous entry.</li>
   *   <li>Each of these entries includes a line with the author and date (without time), followed by the note's text.</li>
   * </ul>
   *
   * @param bundle the FHIR {@link Bundle} containing resources used to resolve author information
   * @param notes a list of {@link Annotation} objects to convert into a single comment
   * @return a {@code CommentDTO}, or null if the input is null or empty
   */
  CommentDTO createComment(Bundle bundle, List<Annotation> notes);

  Bundle deleteBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto,
      Composition compostion, DomainResource resource);

  AllergyDTO toAllergyDTO(AllergyIntolerance allergyIntolerance, Practitioner practitioner, String organization);

  MedicalProblemDTO toMedicalProblemDTO(Condition condition, Practitioner practitioner, String organization);

  PastIllnessDTO toPastIllnessDTO(Condition condition, Practitioner practitioner, String organization);

  VaccinationDTO toVaccinationDTO(Immunization immunization, Practitioner practitioner, String organization);

  Bundle updateBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto, Composition composition,
      DomainResource resource);
}
