package ch.fhir.epr.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class FhirUtilsTest {
  @Test
  void testCreateLegacyTargetDisease() {
    CodeableConcept codeableConcept = createLegacyTargetDisease();

    CodeableConcept legacyTargetDisease = FhirUtils.replaceLegacyTargetDiseaseCoding(codeableConcept);

    assertEquals(FhirConstants.CURRENT_TARGET_DISEASE_CODE, legacyTargetDisease.getCoding().getFirst().getCode());
    assertEquals(FhirConstants.CURRENT_TARGET_DISEASE_DISPLAY, legacyTargetDisease.getCoding().getFirst().getDisplay());
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

  private Immunization createImmunization() {
    Identifier identifier = new Identifier();
    identifier.setValue(FhirConstants.DEFAULT_ID_PREFIX + UUID.randomUUID());
    identifier.setSystem(FhirConstants.DEFAULT_URN_SYSTEM_IDENTIFIER);

    Immunization immunization = new Immunization();
    immunization.setIdentifier(List.of(identifier));
    return immunization;
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
