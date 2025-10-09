package ch.fhir.epr.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.fhir.epr.adapter.FhirUtils.ReferenceType;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationPerformerComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class FhirUtilsTest {

  @Test
  void testGetAnnotation_allergyWithoutNote() {
    AllergyIntolerance allergy = new AllergyIntolerance();
    allergy.setNote(null);

    Annotation result = FhirUtils.getAnnotation(allergy);

    assertNull(result);
  }

  @Test
  void testGetAnnotation_conditionWithEmptyNoteList() {
    Condition condition = new Condition();
    condition.setNote(List.of());

    Annotation result = FhirUtils.getAnnotation(condition);

    assertNull(result);
  }

  @Test
  void testCreateLegacyTargetDisease() {
    CodeableConcept codeableConcept = createLegacyTargetDisease();

    CodeableConcept legacyTargetDisease = FhirUtils.replaceLegacyTargetDiseaseCoding(codeableConcept);

    assertEquals(FhirConstants.CURRENT_TARGET_DISEASE_CODE, legacyTargetDisease.getCoding().getFirst().getCode());
    assertEquals(FhirConstants.CURRENT_TARGET_DISEASE_DISPLAY, legacyTargetDisease.getCoding().getFirst().getDisplay());
  }

  @Test
  void testGetAnnotationFromImmunization() {
    Annotation annotation = FhirUtils.getAnnotation(createImmunization());

    assertNotNull(annotation);
    assertEquals("test", annotation.getText());
  }

  @Test
  void testGetNullAnnotation() {
    Annotation annotation = FhirUtils.getAnnotation(null);

    assertNull(annotation);
  }

  @Test
  void testGetSectionByType() {
    Composition composition = new Composition();
    SectionComponent sectionComponent = createSection();
    composition.addSection(sectionComponent);

    // Test if getSectionByType returns the correct SectionComponent
    SectionComponent result = FhirUtils.getSectionByType(composition, SectionType.IMMUNIZATION);
    assertNotNull(result);
    assertEquals(SectionType.IMMUNIZATION.getId(), result.getId());
  }

  @Test
  void testGetUuidFromBundle() {
    String bundleUUID = "bundleUuid";

    // Create a Bundle with an Identifier
    Bundle bundle = new Bundle();
    bundle.setIdentifier(new Identifier().setValue(bundleUUID));

    // Test if getUuidFromBundle returns the correct UUID
    String uuid = FhirUtils.getUuidFromBundle(bundle);
    assertNotNull(uuid);
    assertEquals(bundleUUID, uuid);
  }

  @Test
  void testGetResourceTypeFromImmunization() {
    String resourceType = FhirUtils.getResourceType(createImmunization());

    assertNotNull(resourceType);
    assertEquals("Immunization", resourceType);
  }

  @Test
  void testGetResourceTypeNull() {
    String resourceType = FhirUtils.getResourceType(null);

    assertNull(resourceType);
  }

  @Test
  void testGetIdentifierValue_Immunization() {
    Immunization immunization = new Immunization();
    immunization.setIdentifier(List.of(new Identifier().setValue("IMMUNIZATION_ID")));
    assertEquals("IMMUNIZATION_ID", FhirUtils.getIdentifierValue(immunization));
  }

  @Test
  void testGetIdentifierValue_Condition() {
    Condition condition = new Condition();
    condition.setIdentifier(List.of(new Identifier().setValue("CONDITION_ID")));
    assertEquals("CONDITION_ID", FhirUtils.getIdentifierValue(condition));
  }

  @Test
  void testGetIdentifierValue_AllergyIntolerance() {
    AllergyIntolerance allergy = new AllergyIntolerance();
    allergy.setIdentifier(List.of(new Identifier().setValue("ALLERGY_ID")));
    assertEquals("ALLERGY_ID", FhirUtils.getIdentifierValue(allergy));
  }

  @Test
  void testGetIdentifierValue_Patient() {
    Patient patient = new Patient();
    patient.setId("PATIENT_ID");
    assertEquals("PATIENT_ID", FhirUtils.getIdentifierValue(patient));
  }

  @Test
  void testGetIdentifierValue_Practitioner() {
    Practitioner practitioner = new Practitioner();
    practitioner.setId("PRACTITIONER_ID");
    assertEquals("PRACTITIONER_ID", FhirUtils.getIdentifierValue(practitioner));
  }

  @Test
  void testGetIdentifierValue_Composition() {
    Composition composition = new Composition();
    composition.setIdentifier(new Identifier().setValue("COMPOSITION_ID"));
    assertEquals("COMPOSITION_ID", FhirUtils.getIdentifierValue(composition));
  }

  @Test
  void testGetIdentifierValue_UnknownResource() {
    Organization org = new Organization();
    org.setId("ORG_ID");
    assertNull(FhirUtils.getIdentifierValue(org));
  }

  @Test
  void testGetIdentifierValue_NullResource() {
    assertNull(FhirUtils.getIdentifierValue(null));
  }

  @Test
  void testImmunizationWithNote() {
    Annotation note = new Annotation();
    note.setText("First Note");

    Immunization immunization = new Immunization();
    immunization.setNote(List.of(note));

    Annotation result = FhirUtils.getAnnotation(immunization);

    assertNotNull(result);
    assertEquals("First Note", result.getText());
  }

  @Test
  void testNullResource() {
    assertNull(FhirUtils.getAnnotation(null));
  }

  @Test
  void testIsPatientTheAuthor_shouldReturnFalseWhenPatientInfoIsNull() {
    PatientIdentifier patientIdentifier = new PatientIdentifier("oid", "localId",
        "localAssigningAuthorityId");
    patientIdentifier.setPatientInfo(null);

    assertFalse(FhirUtils.isPatientTheAuthor(patientIdentifier, new VaccinationDTO()));
  }

  @Test
  void testIsPatientTheAuthor_shouldReturnFalseWhenNamesDoNotMatch() {
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    HumanNameDTO patient = new HumanNameDTO("First", "Last", "prefix", LocalDate.now(), "MALE");
    AuthorDTO author = new AuthorDTO();
    author.setUser(patient);
    VaccinationDTO vaccination = new VaccinationDTO();
    vaccination.setAuthor(author);

    assertFalse(FhirUtils.isPatientTheAuthor(patientIdentifier, vaccination));
  }

  @Test
  void testIsPatientTheAuthor_shouldReturnTrueWhenNamesMatch() {
    PatientIdentifier patientIdentifier = createPatientIdentifier("oid", "localId");
    HumanNameDTO patient = new HumanNameDTO("Kenneth", "Branagh", "Mr.", null, null);
    AuthorDTO author = new AuthorDTO();
    author.setUser(patient);
    MedicalProblemDTO medicalProblem = new MedicalProblemDTO();
    medicalProblem.setAuthor(author);

    assertTrue(FhirUtils.isPatientTheAuthor(patientIdentifier, medicalProblem));
  }

  @Test
  void testIsSamePractitioner_shouldReturnFalseIfPractitionerIsNull() {
    HumanNameDTO authorName = new HumanNameDTO("John", "Doe", "Dr.", null, null);
    assertFalse(FhirUtils.isSamePractitioner(null, authorName));
  }

  @Test
  void testIsSamePractitioner_shouldReturnFalseIfAuthorNameIsNull() {
    Practitioner practitioner = new Practitioner();
    practitioner.setName(List.of(new HumanName().setFamily("Doe")
        .setGiven(List.of(new org.hl7.fhir.r4.model.StringType("John")))));
    assertFalse(FhirUtils.isSamePractitioner(practitioner, null));
  }

  @Test
  void testIsSamePractitioner_shouldReturnFalseIfNamesDoNotMatch() {
    Practitioner practitioner = new Practitioner();
    practitioner.setName(List.of(new HumanName().setFamily("Smith")
        .setGiven(List.of(new org.hl7.fhir.r4.model.StringType("Jane")))));
    HumanNameDTO authorName = new HumanNameDTO("John", "Doe", "Dr.", null, null);
    assertFalse(FhirUtils.isSamePractitioner(practitioner, authorName));
  }

  @Test
  void testIsSamePractitioner_shouldReturnTrueIfNamesMatchCaseInsensitive() {
    Practitioner practitioner = new Practitioner();
    practitioner.setName(List.of(new HumanName().setFamily("Doe")
        .setGiven(List.of(new org.hl7.fhir.r4.model.StringType("John")))));
    HumanNameDTO authorName = new HumanNameDTO("john", "DOE", "Dr.", null, null);
    assertTrue(FhirUtils.isSamePractitioner(practitioner, authorName));
  }

  @Test
  void testIsSamePractitioner_shouldReturnFalseIfFirstOrLastNameIsNull() {
    Practitioner practitioner = new Practitioner();
    practitioner.setName(List.of(new HumanName().setFamily("Doe")
        .setGiven(List.of(new org.hl7.fhir.r4.model.StringType("John")))));
    HumanNameDTO authorName = new HumanNameDTO(null, "Doe", "Dr.", null, null);
    assertFalse(FhirUtils.isSamePractitioner(practitioner, authorName));
    authorName = new HumanNameDTO("John", null, "Dr.", null, null);
    assertFalse(FhirUtils.isSamePractitioner(practitioner, authorName));
  }

  @Test
  void testStripAuthorReference_shouldStripDefaultIdPrefix() {
    String reference = FhirConstants.DEFAULT_ID_PREFIX + "abc123";

    String result = FhirUtils.stripAuthorReference(reference);

    assertEquals("abc123", result);
  }

  @Test
  void testStripAuthorReference_shouldReturnIdentifierPartAfterSlash() {
    String reference = "Practitioner/TC-HCP1-C1";

    String result = FhirUtils.stripAuthorReference(reference);

    assertEquals("TC-HCP1-C1", result);
  }

  @Test
  void getReferenceImmunization_shouldReturnPatientReference() {
    assertEquals("patient uuid", FhirUtils.getReference(createImmunization(), ReferenceType.PATIENT));
  }

  @Test
  void getReferenceImmunization_shouldReturnPractitionerRoleReference() {
    assertEquals("actor uuid", FhirUtils.getReference(createImmunization(), ReferenceType.PRACTITIONER_ROLE));
  }

  @Test
  void getReferenceCondition_shouldReturnPatientReference() {
    assertEquals("patient uuid", FhirUtils.getReference(createCondition(), ReferenceType.PATIENT));
  }

  @Test
  void getReferenceCondition_shouldReturnPractitionerRoleReference() {
    assertEquals("actor uuid", FhirUtils.getReference(createCondition(), ReferenceType.PRACTITIONER_ROLE));
  }

  @Test
  void testGetReference_shouldReturnNullWhenResourceIsNull() {
    assertNull(FhirUtils.getReference(null, ReferenceType.PRACTITIONER_ROLE));
  }

  private Immunization createImmunization() {
    Identifier identifier = new Identifier();
    identifier.setValue(FhirConstants.DEFAULT_ID_PREFIX + UUID.randomUUID());
    identifier.setSystem(FhirConstants.DEFAULT_URN_SYSTEM_IDENTIFIER);

    Immunization immunization = new Immunization();
    immunization.setIdentifier(List.of(identifier));
    immunization.setPatient(new Reference("patient uuid"));
    List<Immunization.ImmunizationPerformerComponent>  immunizationPerformerComponents = new ArrayList<>();
    ImmunizationPerformerComponent immunizationPerformerComponent = new ImmunizationPerformerComponent();
    immunizationPerformerComponent.setActor(new Reference("actor uuid"));
    immunizationPerformerComponents.add(immunizationPerformerComponent);
    immunization.setPerformer(immunizationPerformerComponents);

    Annotation annotation = new Annotation();
    annotation.setText("test");
    List<Annotation> annotations = new ArrayList<>();
    annotations.add(annotation);
    immunization.setNote(annotations);
    return immunization;
  }

  private Condition createCondition() {
    Condition condition = new Condition();
    condition.setSubject(new Reference("patient uuid"));
    condition.setRecorder(new Reference("actor uuid"));

    return condition;
  }

  private PatientIdentifier createPatientIdentifier(String localAssigningAuthorityOid, String localId) {
    PatientIdentifier identifier = new PatientIdentifier(null, localId, localAssigningAuthorityOid);
    identifier.setPatientInfo(new HumanNameDTO("Kenneth", "Branagh", "Mr.", null, null));
    return identifier;
  }

  private SectionComponent createSection() {
    SectionComponent sectionComponent = new SectionComponent();
    sectionComponent.setId(SectionType.IMMUNIZATION.getId());
    Coding coding = new Coding();
    coding.setCode(SectionType.IMMUNIZATION.getCode());
    coding.setSystem(SectionType.IMMUNIZATION.getSystem());
    coding.setDisplay(SectionType.IMMUNIZATION.getDisplay());
    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.addCoding(coding);
    sectionComponent.setCode(codeableConcept);
    return sectionComponent;
  }

  private CodeableConcept createLegacyTargetDisease() {
    CodeableConcept codeableConcept = new CodeableConcept();
    Coding coding = new Coding();
    coding.setCode(FhirConstants.LEGACY_TARGET_DISEASE_CODE);
    coding.setDisplay(FhirConstants.LEGACY_TARGET_DISEASE_DISPLAY);

    codeableConcept.setCoding(List.of(coding));
    return codeableConcept;
  }
}
