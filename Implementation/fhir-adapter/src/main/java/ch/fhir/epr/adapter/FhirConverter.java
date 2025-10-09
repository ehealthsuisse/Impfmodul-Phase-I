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
import ca.uhn.fhir.util.BundleBuilder;
import ch.fhir.epr.adapter.FhirUtils.ReferenceType;
import ch.fhir.epr.adapter.config.FhirConfig;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.data.dto.MedicalProblemDTO;
import ch.fhir.epr.adapter.data.dto.PastIllnessDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationDTO;
import ch.fhir.epr.adapter.data.dto.VaccinationRecordDTO;
import ch.fhir.epr.adapter.data.dto.ValueDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCriticality;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.CompositionRelatesToComponent;
import org.hl7.fhir.r4.model.Composition.DocumentConfidentiality;
import org.hl7.fhir.r4.model.Composition.DocumentRelationshipType;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Immunization.ImmunizationProtocolAppliedComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Converter to build {@link HumanNameDTO} and {@link VaccinationDTO}
 */
@Service
@Slf4j
public class FhirConverter implements FhirConverterIfc {

  @Autowired
  private FhirConfig fhirConfig;

  @Override
  public LocalDateTime convertToLocalDateTime(Date dateToConvert) {
    return dateToConvert != null ? LocalDateTime.ofInstant(
        dateToConvert.toInstant(), ZoneId.systemDefault()) : null;
  }

  @Override
  public Bundle createBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto) {
    return createBundle(ctx, patientIdentifier, dto, false);
  }

  @Override
  public Bundle createBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto,
      boolean forceImmunizationAdministrationDocument) {
    dto.validate(dto.isDeleted());
    String authorId = String.valueOf(UUID.randomUUID());
    String resourceId = FhirUtils.isPatientTheAuthor(patientIdentifier, dto) ? authorId : String.valueOf(UUID.randomUUID());
    Bundle bundle = createBundle(ctx, !forceImmunizationAdministrationDocument && dto instanceof VaccinationRecordDTO);
    createComposition(bundle, dto, patientIdentifier, forceImmunizationAdministrationDocument, authorId, resourceId);
    createPatient(bundle, patientIdentifier, resourceId, FhirConstants.META_CORE_PATIENT_EPR_URL);

    if (dto instanceof VaccinationRecordDTO tO) {
      return createVaccinationRecord(bundle, tO, patientIdentifier);
    }

    return switch (dto) {
      case VaccinationDTO vaccination -> createVaccination(bundle, vaccination, patientIdentifier);
      case AllergyDTO allergy -> createAllergy(bundle, allergy, patientIdentifier);
      case PastIllnessDTO pastIllness -> createPastIllness(bundle, pastIllness, patientIdentifier);
      case MedicalProblemDTO medicalProblem -> createMedicalProblem(bundle, medicalProblem, patientIdentifier);
      default -> throw new TechnicalException("toBundle not supported for class " + dto.getClass().getSimpleName());
    };
  }

  @Override
  public void resolveAndApplyComment(Bundle bundleToBeUpdated, Annotation oldNote, Bundle newBundle, DomainResource resource,
      BaseDTO dto, boolean isPatientTheAuthor) {
    CommentDTO comment = dto.getComment();
    if (comment != null) {
      Annotation annotation = addNote(resource);

      if (annotation != null) {
        annotation.setTime(new Date());
        annotation.setAuthor(new Reference(resolveAuthorReference(bundleToBeUpdated, oldNote, newBundle, dto, resource,
            isPatientTheAuthor)));
        annotation.setText(comment.getText());
      }
    }
  }

  @Override
  public CommentDTO extractCommentFromBundle(Bundle bundle, List<Annotation> notes) {
    if (notes == null || notes.isEmpty()) {
      return null;
    }

    notes.sort(Comparator.comparing(Annotation::getTime));
    Annotation firstNote = notes.getLast();
    HumanNameDTO author = getAuthor(bundle, firstNote);
    LocalDateTime date = convertToLocalDateTime(firstNote.getTime());

    return new CommentDTO(date, author.getFullName(), firstNote.getText());
  }

  @Override
  public Bundle deleteBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto, Composition composition,
      DomainResource resource) {
    dto.setDeleted(true);

    return updateBundle(ctx, patientIdentifier, dto, composition, resource);
  }

  @Override
  public AllergyDTO toAllergyDTO(AllergyIntolerance allergyIntolerance,
      Practitioner practitioner, String organization) {

    HumanNameDTO recorder = FhirUtils.toHumanNameDTO(practitioner);
    String id = allergyIntolerance.getIdentifierFirstRep().getValue().replace(FhirConstants.DEFAULT_ID_PREFIX, "");
    ValueDTO allergyCode = FhirUtils.toValueDTO(allergyIntolerance.getCode());
    ValueDTO criticality = null;
    if (allergyIntolerance.getCriticality() != null) {
      criticality = new ValueDTO(
          allergyIntolerance.getCriticality().toCode(),
          allergyIntolerance.getCriticality().getDisplay(),
          allergyIntolerance.getCriticality().getSystem());
    }
    ValueDTO clinicalStatus = FhirUtils.toValueDTO(allergyIntolerance.getClinicalStatus());
    ValueDTO verificationStatus = FhirUtils.toValueDTO(allergyIntolerance.getVerificationStatus());
    ValueDTO type = null;
    if (allergyIntolerance.hasType()) {
      type = new ValueDTO(allergyIntolerance.getType().toCode(),
          allergyIntolerance.getType().getDisplay(), allergyIntolerance.getType().getSystem());
    }
    Date dateOld = allergyIntolerance.getRecordedDate();
    LocalDate occurrenceDate = convertToLocalDate(dateOld);

    AllergyDTO allergyDTO = new AllergyDTO(
        id,
        occurrenceDate,
        allergyCode,
        criticality,
        clinicalStatus,
        verificationStatus,
        type,
        recorder,
        null,
        organization);

    allergyDTO.setDeleted(FhirConstants.ENTERED_IN_ERROR.equalsIgnoreCase(verificationStatus.getCode()));
    allergyDTO.setRelatedId(getCrossReference(allergyIntolerance));

    return allergyDTO;
  }

  @Override
  public MedicalProblemDTO toMedicalProblemDTO(Condition condition, Practitioner practitioner, String organization) {
    HumanNameDTO recorder = FhirUtils.toHumanNameDTO(practitioner);
    String id = condition.getIdentifierFirstRep().getValue().replace(FhirConstants.DEFAULT_ID_PREFIX, "");
    ValueDTO code = FhirUtils.toValueDTO(condition.getCode());
    ValueDTO clinicalStatus = FhirUtils.toValueDTO(condition.getClinicalStatus());
    ValueDTO verificationStatus = FhirUtils.toValueDTO(condition.getVerificationStatus());
    Date dateOld = condition.getRecordedDate();
    LocalDate recordedData = convertToLocalDate(dateOld);
    LocalDate begin = convertToLocalDate(condition.getOnsetDateTimeType().getValue());
    LocalDate end = convertToLocalDate(condition.getAbatementDateTimeType().getValue());

    MedicalProblemDTO dto =
        new MedicalProblemDTO(id, code, clinicalStatus, verificationStatus, recordedData,
            begin, end, recorder, null, organization);

    dto.setDeleted(FhirConstants.ENTERED_IN_ERROR.equalsIgnoreCase(verificationStatus.getCode()));
    dto.setRelatedId(getCrossReference(condition));

    return dto;
  }

  @Override
  public PastIllnessDTO toPastIllnessDTO(Condition condition, Practitioner practitioner, String organization) {
    HumanNameDTO recorder = FhirUtils.toHumanNameDTO(practitioner);
    String id = condition.getIdentifierFirstRep().getValue().replace(FhirConstants.DEFAULT_ID_PREFIX, "");
    ValueDTO code = FhirUtils.toValueDTO(condition.getCode());
    ValueDTO clinicalStatus = FhirUtils.toValueDTO(condition.getClinicalStatus());
    ValueDTO verificationStatus = FhirUtils.toValueDTO(condition.getVerificationStatus());
    Date dateOld = condition.getRecordedDate();
    LocalDate recordedData = convertToLocalDate(dateOld);
    LocalDate begin = convertToLocalDate(condition.getOnsetDateTimeType().getValue());
    LocalDate end = convertToLocalDate(condition.getAbatementDateTimeType().getValue());

    PastIllnessDTO pastIllnessDTO = new PastIllnessDTO(id, code, clinicalStatus, verificationStatus, recordedData,
            begin, end, recorder, null, organization);

    pastIllnessDTO.setDeleted(FhirConstants.ENTERED_IN_ERROR.equalsIgnoreCase(verificationStatus.getCode()));
    pastIllnessDTO.setRelatedId(getCrossReference(condition));

    return pastIllnessDTO;
  }

  @Override
  public VaccinationDTO toVaccinationDTO(Immunization immunization,
      Practitioner practitioner, String organization) {

    HumanNameDTO performer = FhirUtils.toHumanNameDTO(practitioner);
    String id = immunization.getIdentifierFirstRep().getValue().replace(FhirConstants.DEFAULT_ID_PREFIX, "");

    ValueDTO vaccineCode = FhirUtils.toValueDTO(immunization.getVaccineCode());

    Date dateOld = immunization.getOccurrenceDateTimeType().getValue();
    LocalDate occurrenceDate = convertToLocalDate(dateOld);

    ImmunizationProtocolAppliedComponent protocolAppliedFirstRep =
        immunization.getProtocolAppliedFirstRep();
    Integer doseNumber = protocolAppliedFirstRep.getDoseNumberPositiveIntType().getValue();
    List<ValueDTO> targetDiseases = protocolAppliedFirstRep.getTargetDisease().stream()
        .map(FhirUtils::replaceLegacyTargetDiseaseCoding)
        .map(FhirUtils::toValueDTO)
        .collect(Collectors.toList());

    String lotNumber = immunization.getLotNumber();
    ValueDTO reason = immunization.getReasonCode().stream().findFirst()
        .map(FhirUtils::toValueDTO).orElse(null);

    ValueDTO status = new ValueDTO(immunization.getStatus().toCode(),
        immunization.getStatus().getDisplay(), immunization.getStatus().getSystem());

    VaccinationDTO vaccinationDTO = new VaccinationDTO(
        id,
        vaccineCode,
        targetDiseases,
        null,
        doseNumber,
        occurrenceDate,
        performer,
        organization,
        lotNumber,
        reason,
        status,
        getImmunizationsVerificationStatus(immunization));

    vaccinationDTO.setDeleted(FhirConstants.ENTERED_IN_ERROR.equalsIgnoreCase(status.getCode()));
    vaccinationDTO.setRelatedId(getCrossReference(immunization));

    return vaccinationDTO;
  }

  @Override
  public Bundle updateBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto, Composition composition,
      DomainResource resource) {
    Bundle bundle = createBundle(ctx, patientIdentifier, dto);

    CompositionRelatesToComponent crc = new CompositionRelatesToComponent();
    crc.setCode(DocumentRelationshipType.REPLACES);
    Reference targetReference = new Reference();
    targetReference.setReference(FhirUtils.getIdentifierValue(composition));
    crc.setTarget(targetReference);
    ((Composition) bundle.getEntryFirstRep().getResource()).setRelatesTo(new ArrayList<>(List.of(crc)));

    DomainResource updatedResource = (DomainResource) FhirUtils.getReference(bundle, resource.getClass());
    if (dto.isDeleted()) {
      markResourceDeleted(updatedResource);
    }

    createCrossReference(composition, resource, updatedResource);
    return bundle;
  }

  private void addEntry(Bundle bundle, Resource resource) {
    if (resource == null) {
      return;
    }

    bundle.addEntry()
      .setFullUrl(FhirConstants.DEFAULT_ID_PREFIX + resource.getIdElement().getValue())
      .setResource(resource);
  }

  private Annotation addNote(DomainResource resource) {
    Annotation annotation = null;
    if (resource instanceof Immunization immunization) {
      annotation = immunization.addNote();
    } else if (resource instanceof Condition condition) {
      annotation = condition.addNote();
    } else if (resource instanceof AllergyIntolerance allergyIntolerance) {
      annotation = allergyIntolerance.addNote();
    }
    return annotation;
  }

  private void convertAndSetGender(Patient patient, HumanNameDTO patientInfo) {
    boolean found = false;
    for (AdministrativeGender gender : AdministrativeGender.values()) {
      if (gender.name().equals(patientInfo.getGender())) {
        found = true;
        patient.setGender(AdministrativeGender.valueOf(patientInfo.getGender()));
        break;
      }
    }

    if (!found) {
      patient.setGender(AdministrativeGender.UNKNOWN);
    }
  }

  private Date convertToDate(LocalDate dateToConvert) {
    if (dateToConvert == null) {
      return null;
    }
    return Date.from(ZonedDateTime.of(dateToConvert, LocalTime.of(0, 0), ZoneOffset.UTC).toInstant());
  }

  private LocalDate convertToLocalDate(Date dateToConvert) {
    return dateToConvert != null ? LocalDate.ofInstant(
        dateToConvert.toInstant(), ZoneId.systemDefault()) : null;
  }

  private void copyNoteReferenceIfKnown(Bundle targetBundle, Bundle sourceBundle, Annotation note) {
    String reference = note.getAuthorReference().getReference();
    Practitioner sourcePractitioner = FhirUtils.getPractitioner(sourceBundle, reference);
    if (sourcePractitioner != null) {
      copyPractioner(targetBundle, note, sourcePractitioner);
      return;
    }
    Patient sourcePatient = FhirUtils.getPatient(sourceBundle, reference);
    if (sourcePatient != null) {
      copyPatient(targetBundle, reference, sourcePatient);
      return;
    }
    log.warn("copyNoteAuthor {} not found!", note);
  }

  private void copyPatient(Bundle targetBundle, String id, Patient sourcePatient) {
    Patient targetPatient = FhirUtils.getPatient(targetBundle, id);
    if (targetPatient == null) {
      log.debug("Patient {} added", sourcePatient.getId());
      targetBundle.addEntry().setFullUrl(sourcePatient.getIdElement().getValue()).setResource(sourcePatient);
    }
  }

  private void copyPractioner(Bundle targetBundle, Annotation note, Practitioner sourcePractitioner) {
    Practitioner targetPractioner =
        FhirUtils.getPractitioner(targetBundle, note.getAuthorReference().getReference());
    String srcPractGLN = sourcePractitioner.getIdentifierFirstRep().getValue();

    // special case if multiple HCP are commenting on the same case
    boolean notSameGLN = targetPractioner != null
        && !Objects.equals(srcPractGLN, targetPractioner.getIdentifierFirstRep().getValue());
    if (targetPractioner == null || notSameGLN) {
      log.debug("Practitioner {} added", sourcePractitioner.getId());
      String reference = "/Practitioner/Practitioner-" + srcPractGLN;
      String url = sourcePractitioner.getIdElement().getBaseUrl() + reference;
      sourcePractitioner.setId("Practitioner-" + srcPractGLN);
      note.setAuthor(new Reference(reference));
      targetBundle.addEntry().setFullUrl(url).setResource(sourcePractitioner);
    }
  }

  private Bundle createAllergy(Bundle bundle, AllergyDTO dto, PatientIdentifier patientIdentifier) {
    String patientId = String.valueOf(UUID.randomUUID());
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), dto.getAuthor(),
        FhirConstants.META_PRACTITIONER_ROLE_URL);
    createPatient(bundle, patientIdentifier, patientId, FhirConstants.META_CORE_PATIENT_URL);
    createOrganization(bundle, dto.getOrganization(), practitionerRole);

    AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
    allergyIntolerance.setId(String.valueOf(UUID.randomUUID()));
    allergyIntolerance.addIdentifier(createIdentifier());
    allergyIntolerance.setPatient(new Reference(FhirConstants.DEFAULT_ID_PREFIX + patientId));
    allergyIntolerance.setRecorder(new Reference(FhirConstants.DEFAULT_ID_PREFIX +
        practitionerRole.getIdElement().getValue()));
    allergyIntolerance.setCode(FhirUtils.toCodeableConcept(dto.getCode()));
    allergyIntolerance.addCategory(AllergyIntoleranceCategory.MEDICATION);
    if (dto.getType() != null) {
      allergyIntolerance.setType(AllergyIntolerance.AllergyIntoleranceType.fromCode(dto.getType().getCode()));
    }

    allergyIntolerance
      .setCriticality(AllergyIntoleranceCriticality
      .fromCode(dto.getCriticality() != null ? dto.getCriticality().getCode() : null));

    allergyIntolerance.setClinicalStatus(FhirUtils.toCodeableConcept(dto.getClinicalStatus()));
    allergyIntolerance.setVerificationStatus(FhirUtils.toCodeableConcept(dto.getVerificationStatus()));

    Date dateOccurrenceDate = convertToDate(dto.getOccurrenceDate());
    allergyIntolerance.setLastOccurrence(dateOccurrenceDate);
    allergyIntolerance.setRecordedDate(dateOccurrenceDate);

    addEntry(bundle, allergyIntolerance);
    createSectionIfNecessary(FhirUtils.getResource(Composition.class, bundle), SectionType.ALLERGIES,
        allergyIntolerance);
    return bundle;
  }

  private Bundle createBundle(FhirContext ctx, boolean isVaccinationRecord) {
    BundleBuilder builder = new BundleBuilder(ctx);
    builder.setType("document");

    Bundle bundle = (Bundle) builder.getBundle();
    bundle.setTimestamp(new Date());
    bundle.setId("Bundle-0001");
    bundle.setIdentifier(createIdentifier());
    bundle.setMeta(
        createMeta(isVaccinationRecord ? FhirConstants.META_VACCINATION_RECORD_TYPE_URL
            : FhirConstants.META_VACCINATION_TYPE_URL));

    return bundle;
  }

  private CodeableConcept createCodeableConcept(String code, String name, String system) {
    CodeableConcept concept = new CodeableConcept();
    concept.getCodingFirstRep().setCode(code);
    concept.getCodingFirstRep().setDisplay(name);
    concept.getCodingFirstRep().setSystem(system);

    return concept;
  }

  private Coding createCoding(String code, String name, String system) {
    Coding coding = new Coding();
    coding.setCode(code);
    coding.setDisplay(name);
    coding.setSystem(system);
    return coding;
  }

  private void createComposition(Bundle bundle, BaseDTO dto, PatientIdentifier patientIdentifier,
      boolean forceImmunizationAdministrationDocument, String authorId, String patientId) {
    Composition composition = new Composition();
    composition.setId(String.valueOf(UUID.randomUUID()));
    addEntry(bundle, composition);
    boolean createVaccinationRecord = !forceImmunizationAdministrationDocument && dto instanceof VaccinationRecordDTO;

    composition.setMeta(createMeta(
        createVaccinationRecord ? FhirConstants.META_VACD_COMPOSITION_VAC_REC_URL
            : FhirConstants.META_VACD_COMPOSITION_IMMUN_URL));
    composition.setLanguage("en-US");
    composition.setCategory(createVaccinationRecord ? FhirConstants.COMPOSITION_RECORD_CATEGORY
        : FhirConstants.COMPOSITION_IMMUNIZATON_CATEGORY);

    CodeableConcept compositionType =
        createCodeableConcept("41000179103", "Immunization record", FhirConstants.SNOMED_SYSTEM_URL);
    composition.setType(compositionType);

    composition.setStatus(Composition.CompositionStatus.FINAL);
    composition.setIdentifier(createIdentifier());
    composition.setDate(new Date());

    if (fhirConfig.isPractitioner(dto.getAuthor().getRole())) {
      String practitionerAuthorId = fillAuthor(bundle, dto);
      composition.addAuthor(new Reference(practitionerAuthorId));
    } else if (FhirUtils.isPatientTheAuthor(patientIdentifier, dto)) {
      composition.addAuthor(new Reference(FhirConstants.DEFAULT_ID_PREFIX + authorId));
    } else if (fhirConfig.isPatient(dto.getAuthor().getRole())) {
      // in this block we cover the case in which a record created by a PAT is modified by a REP(representative e.g. family member)
      // this case is not supported at the moment!!!
      createRelatedPerson(bundle, dto, authorId);
      composition.addAuthor(new Reference(FhirConstants.DEFAULT_ID_PREFIX + authorId));
    } else {
      throw new TechnicalException("role:" + dto.getAuthor().getRole() + " not supported");
    }

    composition.setTitle(createVaccinationRecord ? "Vaccination Record" : "Immunization Administration");
    composition.setSubject(new Reference(FhirConstants.DEFAULT_ID_PREFIX + patientId));
    composition.setConfidentiality(DocumentConfidentiality.N);

    Extension confidentialityExtension = composition.getConfidentialityElement().addExtension();
    confidentialityExtension.setUrl(FhirConstants.CONFIDENTIALITY_CODE_EXTENSION_URL);
    // replace definition by snomed url to be conform to new validator - only used for bundle not for
    // XDS metadata
    CodeableConcept confidentiality = FhirUtils.toCodeableConcept(
        dto.getConfidentiality() != null ? dto.getConfidentiality() : FhirConstants.DEFAULT_CONFIDENTIALITY_CODE);
    String system = confidentiality.getCodingFirstRep().getSystem();
    if (FhirConstants.CONFIDENTIALITY_CODE_SYSTEM_NORMAL_RESTRICTED.equals(system)
        || FhirConstants.CONFIDENTIALITY_CODE_SYSTEM_SECRET.equals(system)) {
      confidentiality.getCodingFirstRep().setSystem(FhirConstants.SNOMED_SYSTEM_URL);
    }
    confidentialityExtension.setValue(confidentiality);
  }

  private void createCrossReference(Composition composition, DomainResource resource,
      DomainResource updatedResource) {
    if (resource.hasType(FhirConstants.IMMUNIZATION)) {
      Extension extension = updatedResource.addExtension();
      extension.setUrl(
          "http://fhir.ch/ig/ch-core/StructureDefinition/ch-ext-author");
      extension.setValue(composition.getAuthorFirstRep());
    }

    Extension extension = updatedResource.addExtension();
    extension.setUrl("http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-ext-entry-resource-cross-references");
    extension.addExtension("entry", createReference(FhirUtils.getResourceType(resource),
        FhirUtils.getIdentifierValue(resource)));
    extension.addExtension("container", createReference(FhirUtils.getResourceType(composition),
        FhirUtils.getIdentifierValue(composition)));
    extension.addExtension("relationcode", new CodeType("replaces"));
  }

  private Identifier createIdentifier() {
    return createIdentifier(FhirConstants.DEFAULT_ID_PREFIX + UUID.randomUUID());
  }

  private Identifier createIdentifier(String identifierValue) {
    Identifier identifier = new Identifier();
    identifier.setValue(identifierValue);
    identifier.setSystem(FhirConstants.DEFAULT_URN_SYSTEM_IDENTIFIER);
    return identifier;
  }

  private Bundle createMedicalProblem(Bundle bundle, MedicalProblemDTO dto, PatientIdentifier patientIdentifier) {
    String patientId = String.valueOf(UUID.randomUUID());
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), dto.getAuthor(),
        FhirConstants.META_PRACTITIONER_ROLE_URL);
    createPatient(bundle, patientIdentifier, patientId, FhirConstants.META_CORE_PATIENT_URL);
    createOrganization(bundle, dto.getOrganization(), practitionerRole);

    Condition condition = new Condition();
    condition.setId(String.valueOf(UUID.randomUUID()));
    condition.setSubject(new Reference(FhirConstants.DEFAULT_ID_PREFIX + patientId));
    condition.addIdentifier(createIdentifier());
    condition.setRecorder(new Reference(FhirConstants.DEFAULT_ID_PREFIX + practitionerRole.getIdElement().getValue()));

    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.getCodingFirstRep().setCode("problem-list-item");
    codeableConcept.getCodingFirstRep().setSystem("http://terminology.hl7.org/CodeSystem/condition-category");

    condition.setCategory(List.of(codeableConcept));
    condition.setCode(FhirUtils.toCodeableConcept(dto.getCode()));
    condition.setClinicalStatus(FhirUtils.toCodeableConcept(dto.getClinicalStatus()));
    condition.setVerificationStatus(FhirUtils.toCodeableConcept(dto.getVerificationStatus()));

    condition.setRecordedDate(convertToDate(dto.getRecordedDate()));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    condition.setOnset(new DateTimeType(formatter.format(dto.getBegin())));
    if (dto.getEnd() != null) {
      condition.setAbatement(new DateTimeType(formatter.format(dto.getEnd())));
    }

    condition.setMeta(createMeta(FhirConstants.META_VACD_MEDICAL_PROBLEMS_URL));

    addEntry(bundle, condition);
    createSectionIfNecessary(FhirUtils.getResource(Composition.class, bundle), SectionType.MEDICAL_PROBLEM,
        condition);
    return bundle;
  }

  private Meta createMeta(String url) {
    Meta meta = new Meta();
    meta.setLastUpdated(new Date());
    meta.setProfile(List.of(new CanonicalType(url)));
    return meta;
  }

  private void createOrganization(Bundle bundle, String organizationName, PractitionerRole practitionerRole) {
    Organization organization = new Organization();
    organization.setMeta(createMeta(FhirConstants.META_ORGANIZATION_URL));
    organization.setId(String.valueOf(UUID.randomUUID()));
    organization.setName(organizationName != null ? organizationName : "-");

    practitionerRole.setOrganization(new Reference(FhirConstants.DEFAULT_ID_PREFIX +
        organization.getIdElement().getValue()));

    addEntry(bundle, organization);
  }

  private Bundle createPastIllness(Bundle bundle, PastIllnessDTO dto, PatientIdentifier patientIdentifier) {
    String patientId = String.valueOf(UUID.randomUUID());
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), dto.getAuthor(),
        FhirConstants.META_PRACTITIONER_ROLE_URL);
    createPatient(bundle, patientIdentifier, patientId, FhirConstants.META_CORE_PATIENT_URL);
    createOrganization(bundle, dto.getOrganization(), practitionerRole);

    Condition condition = new Condition();
    condition.setId(String.valueOf(UUID.randomUUID()));
    condition.setSubject(new Reference(FhirConstants.DEFAULT_ID_PREFIX + patientId));
    condition.addIdentifier(createIdentifier());
    condition.setRecorder(new Reference(FhirConstants.DEFAULT_ID_PREFIX + practitionerRole.getIdElement().getValue()));

    condition.setCode(FhirUtils.toCodeableConcept(dto.getCode()));
    condition.setClinicalStatus(FhirUtils.toCodeableConcept(dto.getClinicalStatus()));
    condition.setVerificationStatus(FhirUtils.toCodeableConcept(dto.getVerificationStatus()));

    condition.setRecordedDate(convertToDate(dto.getRecordedDate()));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    condition.setOnset(new DateTimeType(formatter.format(dto.getBegin())));
    if (dto.getEnd() != null) {
      condition.setAbatement(new DateTimeType(formatter.format(dto.getEnd())));
    }

    addEntry(bundle, condition);
    createSectionIfNecessary(FhirUtils.getResource(Composition.class, bundle), SectionType.PAST_ILLNESSES,
        condition);
    return bundle;
  }

  @SuppressWarnings("deprecation")
  private void createPatient(Bundle bundle, PatientIdentifier patientIdentifier, String patientId, String resourceUrl) {
    Identifier identifier = new Identifier();
    identifier
      .setValue(patientIdentifier.getSpidExtension() == null ? FhirConstants.DEFAULT_ID_PREFIX + UUID.randomUUID()
      : patientIdentifier.getSpidExtension());
    identifier.setSystem(FhirConstants.PATIENT_SYSTEM_URL_PREFIX + patientIdentifier.getSpidRootAuthority());

    Patient patient = new Patient();
    patient.setId(patientId);
    patient.setIdentifier(List.of(identifier));
    patient.setMeta(createMeta(resourceUrl));

    HumanNameDTO patientInfo = patientIdentifier.getPatientInfo();
    if (patientInfo != null) {
      patient.addName().setFamily(patientInfo.getLastName()).addGiven(patientInfo.getFirstName())
        .addPrefix(patientInfo.getPrefix());
      patient.setBirthDate(convertToDate(patientInfo.getBirthday()));
      convertAndSetGender(patient, patientInfo);
    } else {
      patient.addName().setFamily("emptyFamily").addGiven("emptyGiven").addPrefix("emptyPrefix");
      // For system generated data birthdate should be 1900-01-01
      patient.setBirthDate(new Date(0, Calendar.JANUARY, 1));
      patient.setGender(AdministrativeGender.UNKNOWN);
    }
    patient.setActive(true);

    addEntry(bundle, patient);
  }

  private Practitioner createPractitioner(HumanNameDTO practitionerName, String gln, String resourceUrl) {
    Practitioner practitioner = new Practitioner();
    practitioner.setId(String.valueOf(UUID.randomUUID()));
    Identifier identifier = new Identifier();
    identifier.setValue(gln != null && !"GLN".equalsIgnoreCase(gln) ? gln : FhirConstants.DEFAULT_PRACTITIONER_CODE);
    identifier.setSystem(FhirConstants.FIXED_PRACTITIONER_SYSTEM);

    practitioner.setIdentifier(List.of(identifier));
    practitioner.addName().setFamily(practitionerName.getLastName())
      .addGiven(practitionerName.getFirstName())
      .addPrefix(practitionerName.getPrefix());

    practitioner.setMeta(createMeta(resourceUrl));
    return practitioner;
  }

  private PractitionerRole createPractitionerRole(Bundle bundle, HumanNameDTO practitionerName, AuthorDTO authorDTO,
      String resourceUrl) {
    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId(String.valueOf(UUID.randomUUID()));
    practitionerRole.setMeta(createMeta(resourceUrl));
    addEntry(bundle, practitionerRole);

    if (practitionerName != null && !practitionerName.getFirstName().isBlank()
        && !practitionerName.getLastName().isBlank()) {
      boolean isAuthorPractitioner = practitionerName.getFullName().equals(authorDTO.getUser().getFullName());
      String gln = isAuthorPractitioner ? authorDTO.getGln() : FhirConstants.DEFAULT_PRACTITIONER_CODE;
      boolean shouldHaveEprSuffix = resourceUrl.contains("epr");
      Practitioner practitioner = createPractitioner(practitionerName, gln,
          shouldHaveEprSuffix ? FhirConstants.META_CORE_PRACTITIONER_EPR_URL : FhirConstants.META_CORE_PRACTITIONER_URL);
      practitionerRole.setPractitioner(new Reference(FhirConstants.DEFAULT_ID_PREFIX +
          practitioner.getIdElement().getValue()));
      addEntry(bundle, practitioner);
    }

    return practitionerRole;
  }

  private Reference createReference(String referenceType, String resourceValue) {
    Reference reference = new Reference();
    reference.setType(referenceType);
    reference.setIdentifier(createIdentifier(resourceValue));
    return reference;
  }

  private void createRelatedPerson(Bundle bundle, BaseDTO dto, String authorId) {
    PatientIdentifier authorPatientIdentifier = new PatientIdentifier(null, null, null);
    authorPatientIdentifier.setPatientInfo(dto.getAuthor().getUser());
    // a new patient instance will be created for the REP
    createPatient(bundle, authorPatientIdentifier, authorId, FhirConstants.META_RELATED_PERSON_URL);
  }

  private void createSectionIfNecessary(Composition composition, SectionType sectionType, Resource resource) {
    SectionComponent sectionComponent = FhirUtils.getSectionByType(composition, sectionType);

    if (sectionComponent == null) {
      sectionComponent = new SectionComponent();
      sectionComponent.setId(sectionType.getId());
      sectionComponent.setTitle(sectionType.getTitle());
      sectionComponent.setCode(
          createCodeableConcept(sectionType.getCode(), sectionType.getDisplay(), sectionType.getSystem()));

      Narrative narrative = new Narrative();
      narrative.setStatus(Narrative.NarrativeStatus.GENERATED);
      narrative.setDivAsString("empty");
      sectionComponent.setText(narrative);
      composition.addSection(sectionComponent);
    }

    if (resource != null) {
      Reference reference =
          new Reference(FhirConstants.DEFAULT_ID_PREFIX + resource.getIdElement().getValue());
      reference.setResource(resource);
      sectionComponent.addEntry(reference);
    }
  }

  private Bundle createVaccination(Bundle bundle, VaccinationDTO dto, PatientIdentifier patientIdentifier) {
    String patientId = String.valueOf(UUID.randomUUID());
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), dto.getAuthor(),
        FhirConstants.META_PRACTITIONER_ROLE_URL);
    createPatient(bundle, patientIdentifier, patientId, FhirConstants.META_CORE_PATIENT_URL);
    createOrganization(bundle, dto.getOrganization(), practitionerRole);

    Immunization immunization = new Immunization();
    immunization.setId(String.valueOf(UUID.randomUUID()));
    immunization.addIdentifier(createIdentifier());
    immunization.setPatient(new Reference(FhirConstants.DEFAULT_ID_PREFIX + patientId));
    immunization.setVaccineCode(FhirUtils.toCodeableConcept(dto.getCode()));
    immunization.setLotNumber(dto.getLotNumber());

    immunization.addPerformer(new Immunization.ImmunizationPerformerComponent(new Reference(
            FhirConstants.DEFAULT_ID_PREFIX + practitionerRole.getIdElement().getValue())));
    immunization.setOccurrence(new DateTimeType(Date.from(
        Instant.from(dto.getOccurrenceDate().atStartOfDay(ZoneId.systemDefault())))));

    Immunization.ImmunizationProtocolAppliedComponent ipac =
        new Immunization.ImmunizationProtocolAppliedComponent();
    ipac.setDoseNumber(new PositiveIntType(dto.getDoseNumber()));
    if (dto.getTargetDiseases() != null) {
      dto.getTargetDiseases()
        .forEach(disease -> ipac.addTargetDisease(FhirUtils.toCodeableConcept(disease)));
    }

    immunization.addProtocolApplied(ipac);

    if (dto.getReason() != null) {
      immunization.addReasonCode(FhirUtils.toCodeableConcept(dto.getReason()));
    }
    immunization.setStatus(Immunization.ImmunizationStatus.fromCode(dto.getStatus().getCode()));
    immunization.setExtension(new ArrayList<>(List.of(createImmunizationsVerificationStatus(dto.getVerificationStatus()))));
    immunization.setMeta(createMeta(FhirConstants.META_VACD_IMMUNIZATION_URL));

    addEntry(bundle, immunization);
    createSectionIfNecessary(FhirUtils.getResource(Composition.class, bundle), SectionType.IMMUNIZATION,
        immunization);
    log.debug("Bundle:{}", ToStringBuilder.reflectionToString(bundle, ToStringStyle.JSON_STYLE));

    return bundle;
  }

  private Bundle createVaccinationRecord(Bundle bundle, VaccinationRecordDTO vaccinationRecord,
      PatientIdentifier patientIdentifier) {
    for (VaccinationDTO vaccination : vaccinationRecord.getVaccinations()) {
      if (!vaccination.isHasErrors()) {
        createVaccination(bundle, vaccination, patientIdentifier);
      }
    }

    for (PastIllnessDTO pastIllness : vaccinationRecord.getPastIllnesses()) {
      if (!pastIllness.isHasErrors()) {
        createPastIllness(bundle, pastIllness, patientIdentifier);
      }
    }

    for (AllergyDTO allergy : vaccinationRecord.getAllergies()) {
      if (!allergy.isHasErrors()) {
        createAllergy(bundle, allergy, patientIdentifier);
      }
    }

    for (MedicalProblemDTO medicalProblem : vaccinationRecord.getMedicalProblems()) {
      if (!medicalProblem.isHasErrors()) {
        createMedicalProblem(bundle, medicalProblem, patientIdentifier);
      }
    }

    return bundle;
  }

  private Extension createImmunizationsVerificationStatus(ValueDTO status) {
    Extension verificationStatus =
        new Extension("http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-verification-status");
    verificationStatus.setValue(
        status != null ? createCoding(status.getCode(), status.getName(), status.getSystem()) :  null);
    return verificationStatus;
  }

  private String fillAuthor(Bundle bundle, BaseDTO dto) {
    AuthorDTO authorDTO = dto.getAuthor();
    if ("ASS".equals(dto.getAuthor().getRole())) {
      // use information of the principal instead of the ASS.
      String principal = dto.getAuthor().getPrincipalName();
      int lastIndexOfSpace = principal.lastIndexOf(" ");
      String given = principal.substring(0, lastIndexOfSpace);
      String family = principal.substring(lastIndexOfSpace + 1);
      HumanNameDTO author = new HumanNameDTO(given, family, null, null, null);

      authorDTO = new AuthorDTO();
      authorDTO.setGln(dto.getAuthor().getPrincipalId());
      authorDTO.setUser(author);
    }

    PractitionerRole practitionerRole = createPractitionerRole(bundle, authorDTO.getUser(), authorDTO,
        FhirConstants.META_PRACTITIONER_ROLE_EPR_URL);
    return practitionerRole.getPractitioner().getReference();
  }

  private HumanNameDTO getAuthor(Bundle bundle, Annotation note) {
    HumanNameDTO author = new HumanNameDTO();
    author.setFirstName("unknown");

    if (note.hasAuthor()) {
      if (note.hasAuthorStringType()) {
        String result = note.getAuthorStringType().asStringValue();
        author.setFirstName(result);
      } else {
        String reference = note.getAuthorReference().getReference();
        AuthorDTO authorDTO = FhirUtils.getAuthor(bundle, reference);
        if (authorDTO != null) {
          author = authorDTO.getUser();
        }
      }
    }
    return author;
  }

  private String getCrossReference(DomainResource domainResource) {
    // ensure backwards compatibility with old cross reference extension - EHSIMAW-666
    Extension legacyCrossRefExtension = domainResource.getExtensionByUrl(
      "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference");
    if (legacyCrossRefExtension != null) {
      Extension extension = legacyCrossRefExtension.getExtensionByUrl("entry");
      Reference reference = (Reference) extension.getValue();
      return reference.getReference().replace(FhirConstants.DEFAULT_ID_PREFIX, "");
    }

    Extension crossRefExtension = domainResource.getExtensionByUrl(
        "http://fhir.ch/ig/ch-core/StructureDefinition/ch-core-ext-entry-resource-cross-references");
    if (crossRefExtension != null) {
      Extension extension = crossRefExtension.getExtensionByUrl("entry");
      Reference reference = (Reference) extension.getValue();
      return reference.getIdentifier().getValue().replace(FhirConstants.DEFAULT_ID_PREFIX, "");
    }

    return null;
  }

  private ValueDTO getImmunizationsVerificationStatus(DomainResource domainResource) {
    Extension verificationStatusExtension = domainResource.getExtensionByUrl(
        "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-verification-status");
    if (Objects.nonNull(verificationStatusExtension)) {
      Coding coding = (Coding) verificationStatusExtension.getValue();
      return coding != null ? new ValueDTO(coding.getCode(), coding.getDisplay(), coding.getSystem()) : null;
    }
    return null;
  }

  private String getPractitionerReference(Bundle bundle, DomainResource updatedResource) {
    String practitionerRoleReference = FhirUtils.getReference(updatedResource, ReferenceType.PRACTITIONER_ROLE);
    PractitionerRole practitionerRole = FhirUtils.getResource(PractitionerRole.class, bundle, practitionerRoleReference);
    return practitionerRole.getPractitioner().getReference();
  }

  private void markResourceDeleted(DomainResource resource) {
    CodeableConcept enteredInError = createCodeableConcept(FhirConstants.ENTERED_IN_ERROR, "Entered in Error", null);
    if (resource instanceof Immunization immunization) {
      immunization.setStatus(ImmunizationStatus.ENTEREDINERROR);
    } else if (resource instanceof AllergyIntolerance allergyIntolerance) {
      enteredInError.getCodingFirstRep()
          .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification");
      allergyIntolerance.setVerificationStatus(enteredInError);
    } else if (resource instanceof Condition pastIllness) {
      enteredInError.getCodingFirstRep()
          .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status");
      pastIllness.setVerificationStatus(enteredInError);
    }
  }

  private String resolveAuthorFromOldNote(Bundle bundleToBeUpdated, Annotation oldNote, Bundle bundle,
      DomainResource updatedResource) {
    String reference = FhirUtils.stripAuthorReference(oldNote.getAuthorReference().getReference());
    Resource authorResource = FhirUtils.getResource(bundleToBeUpdated, reference);

    return switch (authorResource) {
      case Patient ignored -> FhirUtils.getReference(updatedResource, ReferenceType.PATIENT);
      case Practitioner practitioner -> getOrCreatePractitionerReference(bundle, FhirUtils.toHumanNameDTO(practitioner),
          FhirUtils.getAuthor(bundleToBeUpdated), updatedResource);
      case null, default -> null;
    };
  }


  private String resolveAuthorReference(Bundle bundleToBeUpdated, Annotation oldNote, Bundle bundle,
      BaseDTO dto, DomainResource updatedResource, boolean patientIsTheAuthor) {
    CommentDTO commentDTO = dto.getComment();
    if (oldNote != null && commentDTO != null && commentDTO.getText().equals(oldNote.getText())) {
      return resolveAuthorFromOldNote(bundleToBeUpdated, oldNote, bundle, updatedResource);
    }

    // If there is no old comment, or the text differs from the old one,
    // treat it as a new comment and do not reuse the old comment's author reference.
    return patientIsTheAuthor ? FhirUtils.getReference(updatedResource, ReferenceType.PATIENT) :
        getOrCreatePractitionerReference(bundle, dto, updatedResource);
  }

  private String getOrCreatePractitionerReference(Bundle bundle, BaseDTO dto, DomainResource updatedResource) {
    return getOrCreatePractitionerReference(bundle, dto.getAuthor().getUser(), dto.getAuthor(), updatedResource);
  }

  private String getOrCreatePractitionerReference(Bundle bundle, HumanNameDTO practitionerName, AuthorDTO authorDTO,
      DomainResource updatedResource) {
    String practitionerReference = getPractitionerReference(bundle, updatedResource);
    Practitioner newPractitioner = FhirUtils.getResource(Practitioner.class, bundle, practitionerReference);
    boolean samePractitioner = FhirUtils.isSamePractitioner(newPractitioner, practitionerName);

    if (practitionerReference != null && samePractitioner) {
      return practitionerReference;
    }

    return createPractitionerRole(bundle, practitionerName, authorDTO, FhirConstants.META_PRACTITIONER_ROLE_URL)
        .getPractitioner().getReference();
  }
}
