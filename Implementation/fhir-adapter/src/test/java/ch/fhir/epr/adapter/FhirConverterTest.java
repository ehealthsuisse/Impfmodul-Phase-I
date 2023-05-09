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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import java.time.LocalDate;
import java.util.Date;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationProtocolAppliedComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FhirConverterTest {
  private static final String FIRST_NAME = "first";
  private static final String LAST_NAME = "last";
  private static final String PREFIX = "prefix";
  private static final String ID = "ID";

  private static final String COMMENT = "comment";
  private static final Integer DOSE_NUMBER = 200;
  private static final Date DATE = new Date();
  private static final String LOT_NUMBER = "prefix";
  private static final ImmunizationStatus STATUS = ImmunizationStatus.NOTDONE;
  private static final String ORGANIZATION = "organization";
  private HumanNameDTO humanNameDTO;

  private final String SYSTEM = "name";
  private final String DISPLAY = "display";
  private final String CODE = "code";

  private VaccinationDTO vaccinationDTO;
  private FhirConverterIfc fhirConverter = new FhirConverter();

  @Test
  void constructor_validInput_noException() {
    assertThat(humanNameDTO.getFirstName()).isEqualTo(FIRST_NAME);
    assertThat(humanNameDTO.getLastName()).isEqualTo(LAST_NAME);
    assertThat(humanNameDTO.getPrefix()).isEqualTo(PREFIX);
  }

  @Test
  void constructur_validInput_gettersReturnCorrectValues() {
    assertEquals(ID, vaccinationDTO.getId());
    assertEquals(CODE, vaccinationDTO.getCode().getCode());
    assertEquals(1, vaccinationDTO.getComments().size());
    assertEquals(COMMENT, vaccinationDTO.getComments().get(0).getText());
    assertEquals(LocalDate.now(), vaccinationDTO.getOccurrenceDate());
    assertEquals(DOSE_NUMBER, vaccinationDTO.getDoseNumber());
    assertEquals(1, vaccinationDTO.getTargetDiseases().size());
    assertEquals(DISPLAY, vaccinationDTO.getTargetDiseases().get(0).getName());
    assertEquals(CODE, vaccinationDTO.getTargetDiseases().get(0).getCode());
    assertEquals(LOT_NUMBER, vaccinationDTO.getLotNumber());
    assertEquals(DISPLAY, vaccinationDTO.getReason().getName());
    assertEquals(CODE, vaccinationDTO.getReason().getCode());
    assertEquals(STATUS.getDisplay(), vaccinationDTO.getStatus().getCode());
  }

  @BeforeEach
  void setUp() {
    Practitioner practitioner = new Practitioner();
    practitioner
        .addName(new HumanName().addGiven(FIRST_NAME).setFamily(LAST_NAME).addPrefix(PREFIX));
    humanNameDTO = toHumanNameDTO(practitioner);

    Immunization immunization = new Immunization();
    DateTimeType dateTime = new DateTimeType();
    dateTime.setValue(DATE, TemporalPrecisionEnum.DAY);

    immunization.addIdentifier(new Identifier().setValue(ID))
        .setVaccineCode(new CodeableConcept(new Coding(SYSTEM, CODE, DISPLAY)))
        .addNote(new Annotation(new MarkdownType(COMMENT)))
        .setOccurrence(dateTime)
        .addProtocolApplied(
            new ImmunizationProtocolAppliedComponent(new PositiveIntType(DOSE_NUMBER))
                .addTargetDisease(new CodeableConcept(new Coding(SYSTEM, CODE, DISPLAY))))
        .setLotNumber(LOT_NUMBER)
        .addReasonCode(new CodeableConcept(new Coding(SYSTEM, CODE, DISPLAY)))
        .setStatus(STATUS);

    String organization = ORGANIZATION;
    vaccinationDTO = fhirConverter.toVaccinationDTO(immunization, practitioner, organization);
    vaccinationDTO.setComments(fhirConverter.createComments(null, immunization.getNote()));
  }

  @Test
  void toString_validData_returnCorrectFormatting() {
    assertThat(humanNameDTO.toString()).hasToString(
        "HumanNameDTO(firstName=" + FIRST_NAME + ", lastName=" + LAST_NAME + ", prefix=" + PREFIX
            + ", birthday=null, gender=null)");
  }

  private HumanNameDTO toHumanNameDTO(Practitioner practitioner) {
    if (practitioner == null) {
      return null;
    }

    HumanName humanName = practitioner.getNameFirstRep();
    String lastName = humanName.getFamily();
    String firstName = humanName.getGivenAsSingleString();
    String prefix = humanName.getPrefixAsSingleString();

    return new HumanNameDTO(firstName, lastName, prefix, null, null);
  }
}
