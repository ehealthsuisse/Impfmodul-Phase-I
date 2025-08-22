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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ch.fhir.epr.TestMain;
import ch.fhir.epr.adapter.config.FhirConfig;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCriticality;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationProtocolAppliedComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(classes = TestMain.class)
@SpringJUnitConfig(classes = {FhirConfig.class})
class FhirConverterTest {
  private static final String SYSTEM = "system";
  private static final String DISPLAY = "display";
  private static final String CODE = "code";
  private static final CodeableConcept CODEABLE_CONCEPT = new CodeableConcept(new Coding(SYSTEM, CODE, DISPLAY));
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

  private VaccinationDTO vaccinationDTO;

  @Autowired
  private FhirConverterIfc fhirConverter;
  @Autowired
  private FhirConfig fhirConfig;

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
    assertNotNull(vaccinationDTO.getComment());
    assertEquals(COMMENT, vaccinationDTO.getComment().getText());
    assertEquals(LocalDate.now(), vaccinationDTO.getOccurrenceDate());
    assertEquals(DOSE_NUMBER, vaccinationDTO.getDoseNumber());
    assertEquals(1, vaccinationDTO.getTargetDiseases().size());
    assertEquals(DISPLAY, vaccinationDTO.getTargetDiseases().getFirst().getName());
    assertEquals(CODE, vaccinationDTO.getTargetDiseases().getFirst().getCode());
    assertEquals(LOT_NUMBER, vaccinationDTO.getLotNumber());
    assertEquals(DISPLAY, vaccinationDTO.getReason().getName());
    assertEquals(CODE, vaccinationDTO.getReason().getCode());
    assertEquals(STATUS.getDisplay(), vaccinationDTO.getStatus().getCode());
  }

  @Test
  void createBundle_confidentialitySet_replaceSnomedInBundleButKeepDTOUnchanged() {
    vaccinationDTO.setConfidentiality(FhirConstants.DEFAULT_CONFIDENTIALITY_CODE);
    PatientIdentifier identifier = mock(PatientIdentifier.class);

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), identifier, vaccinationDTO);
    assertEquals("http://snomed.info/sct", FhirUtils.getConfidentiality(bundle).getSystem());
    assertEquals(FhirConstants.CONFIDENTIALITY_CODE_SYSTEM_NORMAL_RESTRICTED,
        vaccinationDTO.getConfidentiality().getSystem());
  }

  @Test
  void createBundle_fillPatientWithKnownGender_patientGenderIsCorrectlySet() {
    HumanNameDTO patient = new HumanNameDTO("First", "Last", "prefix", LocalDate.now(), "MALE");
    PatientIdentifier identifier = mock(PatientIdentifier.class);
    when(identifier.getPatientInfo()).thenReturn(patient);

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), identifier, vaccinationDTO);
    Patient result = FhirUtils.getPatient(bundle, "Patient-0001");
    assertEquals("MALE", result.getGender().name());
  }

  @Test
  void createBundle_fillPatientWithUnknownGender_patientGenderIsSetUnknown() {
    String unknownGender = "adfasdf";
    HumanNameDTO patient = new HumanNameDTO("First", "Last", "prefix", LocalDate.now(), unknownGender);
    PatientIdentifier identifier = mock(PatientIdentifier.class);
    when(identifier.getPatientInfo()).thenReturn(patient);

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), identifier, vaccinationDTO);
    Patient result = FhirUtils.getPatient(bundle, "Patient-0001");
    assertEquals("UNKNOWN", result.getGender().name());
  }

  @Test
  void createBundle_withoutRecorder_noExceptionDuringWriteAndRead_noPractitionerCreated_organizationCorrectlyReturned() {
    HumanNameDTO patient = new HumanNameDTO("First", "Last", "prefix", LocalDate.now(), "MALE");
    PatientIdentifier identifier = mock(PatientIdentifier.class);
    when(identifier.getPatientInfo()).thenReturn(patient);

    vaccinationDTO.setRecorder(null);
    // avoid having author as practitioner
    vaccinationDTO.getAuthor().setRole("PAT");

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), identifier, vaccinationDTO);

    Practitioner practitioner = FhirUtils.getResource(Practitioner.class, bundle);
    Organization organization = FhirUtils.getResource(Organization.class, bundle);

    assertNull(practitioner);
    assertEquals(vaccinationDTO.getOrganization(), organization.getName());
  }

  @Test
  void createBundle_recorderDifferentThanThePerformer_practitionerShouldHaveTheDefaultPractitionerCode() {
    HumanNameDTO patientRecorder = new HumanNameDTO("John", "Doe", "prefix", LocalDate.now(), "MALE");
    HumanNameDTO patient = new HumanNameDTO("First", "Last", "prefix", LocalDate.now(), "FEMALE");
    PatientIdentifier identifier = mock(PatientIdentifier.class);
    AuthorDTO authorDTO = createAuthor();
    when(identifier.getPatientInfo()).thenReturn(patient);

    vaccinationDTO.setRecorder(patientRecorder);
    vaccinationDTO.setAuthor(authorDTO);

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), identifier, vaccinationDTO);

    Practitioner practitioner = FhirUtils.getResource(Practitioner.class, bundle, "Practitioner-0001");

    assertEquals("7601007922000", practitioner.getIdentifier().getFirst().getValue());
  }

  @Test
  void createBundle_recorderTheSameAsThePerformer_practitionerShouldHaveTheGlnOfTheAuthor() {
    HumanNameDTO patientRecorder = new HumanNameDTO("first", "last", "prefix", LocalDate.now(), "MALE");
    HumanNameDTO patient = new HumanNameDTO("Jane", "Doe", null, LocalDate.now(), "FEMALE");
    PatientIdentifier identifier = mock(PatientIdentifier.class);
    AuthorDTO authorDTO = createAuthor();
    when(identifier.getPatientInfo()).thenReturn(patient);

    vaccinationDTO.setRecorder(patientRecorder);
    vaccinationDTO.setAuthor(authorDTO);

    Bundle bundle = fhirConverter.createBundle(FhirContext.forR4(), identifier, vaccinationDTO);

    Practitioner practitioner = FhirUtils.getResource(Practitioner.class, bundle, "Practitioner-0001");

    assertEquals("7601007922000", practitioner.getIdentifier().getFirst().getValue());
  }

  @Test
  void fillAuthor_assistentRole_usePrincipalId() {
    Bundle bundle = new Bundle();
    BaseDTO dto = mock(VaccinationDTO.class);

    // author should be ASS role
    AuthorDTO author = createAuthor();
    when(dto.getAuthor()).thenReturn(author);
    PatientIdentifier identifier = mock(PatientIdentifier.class);

    ReflectionTestUtils.invokeMethod(fhirConverter, "createComposition", bundle, dto, identifier, false);

    author = FhirUtils.getAuthor(bundle);
    assertEquals("principalId", author.getGln());
    assertEquals("given", author.getUser().getFirstName());
    assertEquals("family", author.getUser().getLastName());
  }

  @BeforeEach
  void setUp() {
    Practitioner practitioner = new Practitioner();
    practitioner.addName(new HumanName().addGiven(FIRST_NAME).setFamily(LAST_NAME).addPrefix(PREFIX));
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

    vaccinationDTO = fhirConverter.toVaccinationDTO(immunization, practitioner, ORGANIZATION);
    vaccinationDTO.setComment(fhirConverter.createComment(null, immunization.getNote()));
    vaccinationDTO.setAuthor(createAuthor());

    fhirConfig.setPractitionerRoles(List.of("ASS", "HCP"));
    fhirConfig.setPatientRoles(List.of("REP", "PAT"));
  }

  @SuppressWarnings("deprecation")
  @Test
  void testConvertToLocalDateTime() {
    Date date = new Date();
    LocalDateTime localDate = fhirConverter.convertToLocalDateTime(date);
    assertNotNull(localDate);
    assertEquals(date.getSeconds(), localDate.getSecond());
    assertEquals(date.getMinutes(), localDate.getMinute());
    assertEquals(date.getHours(), localDate.getHour());
    assertEquals(date.getDate(), localDate.getDayOfMonth());
    // month starts with 1 for local date
    assertEquals(date.getMonth(), localDate.getMonth().ordinal());
    assertEquals(date.getYear(), localDate.getYear() - 1900);

    // no date given
    assertNull(fhirConverter.convertToLocalDateTime(null));
  }

  @Test
  void testToAllergyDTO() {
    // Create mock objects for dependencies
    AllergyIntolerance allergyIntolerance = mock(AllergyIntolerance.class);
    Practitioner practitioner = mock(Practitioner.class);
    String organization = "OrganizationName";

    // Define behavior for mock objects
    Identifier identifier = createIdentifier("Allergy-123");
    when(allergyIntolerance.getIdentifierFirstRep()).thenReturn(identifier);
    when(allergyIntolerance.getCode()).thenReturn(CODEABLE_CONCEPT);
    when(allergyIntolerance.getCriticality()).thenReturn(AllergyIntoleranceCriticality.UNABLETOASSESS);
    when(allergyIntolerance.getClinicalStatus()).thenReturn(CODEABLE_CONCEPT);
    when(allergyIntolerance.getVerificationStatus()).thenReturn(CODEABLE_CONCEPT);
    when(allergyIntolerance.getRecordedDate()).thenReturn(
        Date.from(ZonedDateTime.of(LocalDate.now(), LocalTime.of(0, 0), ZoneOffset.UTC).toInstant()));


    // Call the method to test
    AllergyDTO allergyDTO = fhirConverter.toAllergyDTO(allergyIntolerance, practitioner, organization);

    // Assertions
    assertEquals("Allergy-123", allergyDTO.getId());
    assertEquals(organization, allergyDTO.getOrganization());
    assertEquals(SYSTEM, allergyDTO.getCode().getSystem());
    assertEquals(CODE, allergyDTO.getCode().getCode());
    assertEquals(DISPLAY, allergyDTO.getCode().getName());
    assertEquals(SYSTEM, allergyDTO.getClinicalStatus().getSystem());
    assertEquals(CODE, allergyDTO.getClinicalStatus().getCode());
    assertEquals(DISPLAY, allergyDTO.getClinicalStatus().getName());
    assertEquals(SYSTEM, allergyDTO.getVerificationStatus().getSystem());
    assertEquals(CODE, allergyDTO.getVerificationStatus().getCode());
    assertEquals(DISPLAY, allergyDTO.getVerificationStatus().getName());
    assertEquals(AllergyIntoleranceCriticality.UNABLETOASSESS.toCode(), allergyDTO.getCriticality().getCode());
  }

  @Test
  void testToMedicalProblemDTO() {
    // Create mock objects for dependencies
    Condition condition = mock(Condition.class);
    Practitioner practitioner = mock(Practitioner.class);
    String organization = "OrganizationName";

    // Define behavior for mock objects
    Identifier identifier = createIdentifier("Condition-123");
    when(condition.getIdentifierFirstRep()).thenReturn(identifier);
    when(condition.getCode()).thenReturn(CODEABLE_CONCEPT);
    when(condition.getClinicalStatus()).thenReturn(CODEABLE_CONCEPT);
    when(condition.getVerificationStatus()).thenReturn(CODEABLE_CONCEPT);
    LocalDate today = LocalDate.now();
    when(condition.getOnsetDateTimeType()).thenReturn(new DateTimeType(
        Date.from(ZonedDateTime.of(today, LocalTime.of(0, 0), ZoneOffset.UTC).toInstant())));
    when(condition.getAbatementDateTimeType()).thenReturn(new DateTimeType(
        Date.from(ZonedDateTime.of(today, LocalTime.of(0, 0), ZoneOffset.UTC).toInstant())));
    when(condition.getRecordedDate()).thenReturn(
        Date.from(ZonedDateTime.of(today, LocalTime.of(0, 0), ZoneOffset.UTC).toInstant()));

    // Call the method to test
    MedicalProblemDTO medicalProblemDTO = fhirConverter.toMedicalProblemDTO(condition, practitioner, organization);

    // Assertions
    assertEquals("Condition-123", medicalProblemDTO.getId());
    assertEquals(organization, medicalProblemDTO.getOrganization());
    assertEquals(SYSTEM, medicalProblemDTO.getCode().getSystem());
    assertEquals(CODE, medicalProblemDTO.getCode().getCode());
    assertEquals(DISPLAY, medicalProblemDTO.getCode().getName());
    assertEquals(SYSTEM, medicalProblemDTO.getClinicalStatus().getSystem());
    assertEquals(CODE, medicalProblemDTO.getClinicalStatus().getCode());
    assertEquals(DISPLAY, medicalProblemDTO.getClinicalStatus().getName());
    assertEquals(SYSTEM, medicalProblemDTO.getVerificationStatus().getSystem());
    assertEquals(CODE, medicalProblemDTO.getVerificationStatus().getCode());
    assertEquals(DISPLAY, medicalProblemDTO.getVerificationStatus().getName());
    assertEquals(today, medicalProblemDTO.getBegin());
    assertEquals(today, medicalProblemDTO.getEnd());
  }

  @Test
  void testToPastIllnessDTO() {
    // Create mock objects for dependencies
    Condition condition = mock(Condition.class);
    Practitioner practitioner = mock(Practitioner.class);
    String organization = "OrganizationName";

    // Define behavior for mock objects
    Identifier identifier = createIdentifier("Condition-123");
    when(condition.getIdentifierFirstRep()).thenReturn(identifier);
    when(condition.getCode()).thenReturn(CODEABLE_CONCEPT);
    when(condition.getClinicalStatus()).thenReturn(CODEABLE_CONCEPT);
    when(condition.getVerificationStatus()).thenReturn(CODEABLE_CONCEPT);

    LocalDate today = LocalDate.now();
    when(condition.getOnsetDateTimeType()).thenReturn(new DateTimeType(
        Date.from(ZonedDateTime.of(today, LocalTime.of(0, 0), ZoneOffset.UTC).toInstant())));
    when(condition.getAbatementDateTimeType()).thenReturn(new DateTimeType(
        Date.from(ZonedDateTime.of(today, LocalTime.of(0, 0), ZoneOffset.UTC).toInstant())));
    when(condition.getRecordedDate()).thenReturn(
        Date.from(ZonedDateTime.of(today, LocalTime.of(0, 0), ZoneOffset.UTC).toInstant()));

    // Call the method to test
    PastIllnessDTO pastIllnessDTO = fhirConverter.toPastIllnessDTO(condition, practitioner, organization);

    // Assertions
    assertEquals("Condition-123", pastIllnessDTO.getId());
    assertEquals(organization, pastIllnessDTO.getOrganization());
    assertEquals(SYSTEM, pastIllnessDTO.getCode().getSystem());
    assertEquals(CODE, pastIllnessDTO.getCode().getCode());
    assertEquals(DISPLAY, pastIllnessDTO.getCode().getName());
    assertEquals(SYSTEM, pastIllnessDTO.getClinicalStatus().getSystem());
    assertEquals(CODE, pastIllnessDTO.getClinicalStatus().getCode());
    assertEquals(DISPLAY, pastIllnessDTO.getClinicalStatus().getName());
    assertEquals(SYSTEM, pastIllnessDTO.getVerificationStatus().getSystem());
    assertEquals(CODE, pastIllnessDTO.getVerificationStatus().getCode());
    assertEquals(DISPLAY, pastIllnessDTO.getVerificationStatus().getName());
    assertEquals(today, pastIllnessDTO.getBegin());
    assertEquals(today, pastIllnessDTO.getEnd());
  }

  @Test
  void testToVaccinationDTO() {
    // Create mock objects for dependencies
    Immunization immunization = mock(Immunization.class);
    Practitioner practitioner = mock(Practitioner.class);
    String organization = "OrganizationName";

    // Define behavior for mock objects
    Identifier identifier = createIdentifier("Immunization-123");
    when(immunization.getIdentifierFirstRep()).thenReturn(identifier);
    when(immunization.getVaccineCode()).thenReturn(CODEABLE_CONCEPT);
    when(immunization.getLotNumber()).thenReturn("Lot123");
    when(immunization.getOccurrenceDateTimeType()).thenReturn(new DateTimeType(new Date()));
    when(immunization.getStatus()).thenReturn(Immunization.ImmunizationStatus.COMPLETED);
    ImmunizationProtocolAppliedComponent protocolAppliedFirstRep = new ImmunizationProtocolAppliedComponent();
    protocolAppliedFirstRep.setTargetDisease(List.of(CODEABLE_CONCEPT));
    protocolAppliedFirstRep.setDoseNumber(new PositiveIntType(1));
    when(immunization.getProtocolAppliedFirstRep()).thenReturn(protocolAppliedFirstRep);
    immunization.getProtocolAppliedFirstRep();

    // Call the method to test
    VaccinationDTO vaccinationDTO = fhirConverter.toVaccinationDTO(immunization, practitioner, organization);

    // Assertions
    assertEquals("Immunization-123", vaccinationDTO.getId());
    assertEquals(organization, vaccinationDTO.getOrganization());
    assertEquals(SYSTEM, vaccinationDTO.getCode().getSystem());
    assertEquals(CODE, vaccinationDTO.getCode().getCode());
    assertEquals(DISPLAY, vaccinationDTO.getCode().getName());
    assertEquals("Lot123", vaccinationDTO.getLotNumber());
    assertEquals(1, vaccinationDTO.getDoseNumber());
  }

  @Test
  void toString_validData_returnCorrectFormatting() {
    assertThat(humanNameDTO.toString()).hasToString(
        "HumanNameDTO(firstName=" + FIRST_NAME + ", lastName=" + LAST_NAME + ", prefix=" + PREFIX
            + ", birthday=null, gender=null)");
  }

  private AuthorDTO createAuthor() {
    AuthorDTO author = new AuthorDTO(humanNameDTO, "ASS", "gln");
    String principalId = "principalId";
    String principalName = "given family";
    author.setPrincipalId(principalId);
    author.setPrincipalName(principalName);
    return author;
  }

  private Identifier createIdentifier(String id) {
    Identifier identifier = new Identifier();
    identifier.setValue(id);
    identifier.setSystem(SYSTEM);
    return identifier;
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
