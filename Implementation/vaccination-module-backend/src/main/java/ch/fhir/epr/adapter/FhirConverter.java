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
import ch.fhir.epr.adapter.config.FhirConfig;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AllergyDTO;
import ch.fhir.epr.adapter.data.dto.BaseDTO;
import ch.fhir.epr.adapter.data.dto.CommentDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

  private static final String SECTION_ALLERGIES_ID = "allergies";
  private static final String SECTION_PAST_ILLNESSES_ID = "pastIllnesses";
  private static final String SECTION_IMMUNIZATION_ID = "administration";
  private static final String DEFAULT_SYSTEM_IDENTIFIER = "urn:ietf:rfc:3986";
  public static final String DEFAULT_ID_PREFIX = "urn:uuid:";

  @Autowired
  private FhirConfig fhirConfig;

  @Override
  public LocalDateTime convertToLocalDateTime(Date dateToConvert) {
    return dateToConvert != null ? LocalDateTime.ofInstant(
        dateToConvert.toInstant(), ZoneId.systemDefault()) : null;
  }


  @Override
  public <T> void copyNotes(Bundle targetBundle, Bundle sourceBundle, Class<T> type) {
    List<Annotation> notes;
    if (type == Immunization.class) {
      notes = FhirUtils.getResource(Immunization.class, sourceBundle).getNote();
    } else if (type == AllergyIntolerance.class) {
      notes = FhirUtils.getResource(AllergyIntolerance.class, sourceBundle).getNote();
    } else if (type == Condition.class) {
      notes = FhirUtils.getResource(Condition.class, sourceBundle).getNote();
    } else {
      log.warn("copyNotes type {} not supported", type);
      return;
    }

    if (notes == null || notes.isEmpty()) {
      return;
    }

    for (Annotation note : notes) {
      log.debug("note:{}", note.getText());
      if (note.hasAuthorStringType()) {
        log.debug("hasAuthorStringType:{}", note.getText());
      } else if (note.hasAuthorReference()) {
        copyAuthorNote(targetBundle, sourceBundle, note.getAuthorReference().getReference());
      }
      log.debug("addNote:{} {}", note.getText(), note.getAuthor());
      if (type == Immunization.class) {
        FhirUtils.getResource(Immunization.class, targetBundle).addNote(note);
      } else if (type == AllergyIntolerance.class) {
        FhirUtils.getResource(AllergyIntolerance.class, targetBundle).addNote(note);
      } else if (type == Condition.class) {
        FhirUtils.getResource(Condition.class, targetBundle).addNote(note);
      }
    }
  }

  @Override
  public Bundle createBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto) {
    Bundle bundle = createBundle(ctx, dto instanceof VaccinationRecordDTO);
    Composition composition = createComposition(bundle, dto, patientIdentifier);
    createPatient(bundle, composition, patientIdentifier, "Patient-0001", true);

    if (dto instanceof VaccinationRecordDTO) {
      return createVaccinationRecord(bundle, composition, (VaccinationRecordDTO) dto);
    }

    DomainResource updatedResource = null;
    if (dto instanceof VaccinationDTO vaccination) {
      bundle = createVaccination(bundle, getSection(composition, SECTION_IMMUNIZATION_ID), vaccination);
      updatedResource = (DomainResource) getReference(bundle, Immunization.class);
    } else if (dto instanceof AllergyDTO allergy) {
      bundle = createAllergy(bundle, getSection(composition, SECTION_ALLERGIES_ID), allergy);
      updatedResource = (DomainResource) getReference(bundle, AllergyIntolerance.class);
    } else if (dto instanceof PastIllnessDTO pastIllness) {
      bundle = createPastIllness(bundle, getSection(composition, SECTION_PAST_ILLNESSES_ID), pastIllness);
      updatedResource = (DomainResource) getReference(bundle, Condition.class);
    } else {
      throw new TechnicalException("toBundle not supported for class " + dto.getClass().getSimpleName());
    }

    createComment(updatedResource, dto,
        FhirUtils.getResource(Composition.class, bundle).getAuthorFirstRep().getReference());

    return bundle;
  }

  @Override
  public List<CommentDTO> createComments(Bundle bundle, List<Annotation> notes) {
    List<CommentDTO> commentDTOs = new ArrayList<>();
    for (Annotation note : notes) {
      HumanNameDTO author = getAuthor(bundle, note);
      commentDTOs.add(new CommentDTO(convertToLocalDateTime(note.getTime()), author, note.getText()));
    }

    Collections.sort(commentDTOs, Comparator.comparing(CommentDTO::getDate).reversed());
    return commentDTOs;
  }

  @Override
  public Bundle deleteBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto,
      Composition compostion, DomainResource resource) {
    dto.setDeleted(true);

    return updateBundle(ctx, patientIdentifier, dto, compostion, resource);
  }

  @Override
  public AllergyDTO toAllergyDTO(AllergyIntolerance allergyIntolerance,
      Practitioner practitioner, String organization) {

    HumanNameDTO recorder = FhirUtils.toHumanNameDTO(practitioner);
    String id = allergyIntolerance.getIdentifierFirstRep().getValue().replace(DEFAULT_ID_PREFIX, "");
    ValueDTO allergyCode = new ValueDTO(allergyIntolerance.getCode());
    ValueDTO criticality = null;
    if (allergyIntolerance.getCriticality() != null) {
      criticality = new ValueDTO(
          allergyIntolerance.getCriticality().toCode(),
          allergyIntolerance.getCriticality().getDisplay(),
          allergyIntolerance.getCriticality().getSystem());
    }
    ValueDTO clinicalStatus = new ValueDTO(allergyIntolerance.getClinicalStatus());
    ValueDTO verificationStatus = new ValueDTO(allergyIntolerance.getVerificationStatus());
    ValueDTO type = null;
    if (allergyIntolerance.hasType()) {
      type = new ValueDTO(allergyIntolerance.getType().toCode(),
          allergyIntolerance.getType().getDisplay(), allergyIntolerance.getType().getSystem());
    }
    Date dateOld = allergyIntolerance.getLastOccurrence();
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

    allergyDTO.setDeleted(FhirConverterIfc.ENTERED_IN_ERROR.equalsIgnoreCase(verificationStatus.getCode()));
    allergyDTO.setRelatedId(getCrossReference(allergyIntolerance));

    return allergyDTO;
  }

  @Override
  public PastIllnessDTO toPastIllnessDTO(Condition condition, Practitioner practitioner, String organization) {
    HumanNameDTO recorder = FhirUtils.toHumanNameDTO(practitioner);
    String id = condition.getIdentifierFirstRep().getValue().replace(DEFAULT_ID_PREFIX, "");
    ValueDTO code = new ValueDTO(condition.getCode());
    ValueDTO clinicalStatus = new ValueDTO(condition.getClinicalStatus());
    ValueDTO verificationStatus = new ValueDTO(condition.getVerificationStatus());
    Date dateOld = condition.getRecordedDate();
    LocalDate recordedData = convertToLocalDate(dateOld);
    LocalDate begin = convertToLocalDate(condition.getOnsetDateTimeType().getValue());
    LocalDate end = convertToLocalDate(condition.getAbatementDateTimeType().getValue());

    PastIllnessDTO pastIllnessDTO =
        new PastIllnessDTO(id, code, clinicalStatus, verificationStatus, recordedData,
            begin, end, recorder, null, organization);

    pastIllnessDTO.setDeleted(FhirConverterIfc.ENTERED_IN_ERROR.equalsIgnoreCase(verificationStatus.getCode()));
    pastIllnessDTO.setRelatedId(getCrossReference(condition));

    return pastIllnessDTO;
  }

  @Override
  public VaccinationDTO toVaccinationDTO(Immunization immunization,
      Practitioner practitioner, String organization, boolean validated) {

    HumanNameDTO performer = FhirUtils.toHumanNameDTO(practitioner);
    String id = immunization.getIdentifierFirstRep().getValue().replace(DEFAULT_ID_PREFIX, "");

    ValueDTO vaccineCode = new ValueDTO(immunization.getVaccineCode());

    Date dateOld = immunization.getOccurrenceDateTimeType().getValue();
    LocalDate occurrenceDate = convertToLocalDate(dateOld);

    ImmunizationProtocolAppliedComponent protocolAppliedFirstRep =
        immunization.getProtocolAppliedFirstRep();
    Integer doseNumber = protocolAppliedFirstRep.getDoseNumberPositiveIntType().getValue();
    List<ValueDTO> targetDiseases = protocolAppliedFirstRep.getTargetDisease().stream()
        .map(ValueDTO::new)
        .collect(Collectors.toList());

    String lotNumber = immunization.getLotNumber();
    ValueDTO reason = immunization.getReasonCode().stream().findFirst()
        .map(ValueDTO::new).orElse(null);

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
        validated);

    vaccinationDTO.setDeleted(FhirConverterIfc.ENTERED_IN_ERROR.equalsIgnoreCase(status.getCode()));
    vaccinationDTO.setRelatedId(getCrossReference(immunization));

    return vaccinationDTO;
  }

  @Override
  public Bundle updateBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto,
      Composition compostion, DomainResource resource) {
    Bundle bundle = createBundle(ctx, patientIdentifier, dto);

    CompositionRelatesToComponent crc = new CompositionRelatesToComponent();
    crc.setCode(DocumentRelationshipType.REPLACES);
    Reference targetReference = new Reference();
    targetReference.setReference(getIdentifierValue(compostion));
    crc.setTarget(targetReference);
    ((Composition) bundle.getEntryFirstRep().getResource()).setRelatesTo(Arrays.asList(crc));

    DomainResource updatedResource = (DomainResource) getReference(bundle, resource.getClass());
    if (dto.isDeleted()) {
      markResourceDeleted(updatedResource);
    }

    createCrossReference(compostion, resource, updatedResource);
    return bundle;
  }

  private void addEntry(Bundle bundle, String url, Resource resource) {
    if (resource == null) {
      return;
    }

    bundle.addEntry()
        .setFullUrl(fhirConfig.getBaseURL() + url + resource.getIdElement().getValue())
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

  private void copyAuthorNote(Bundle targetBundle, Bundle sourceBundle, String id) {
    Practitioner sourcePractitioner = FhirUtils.getPractitioner(sourceBundle, id);
    if (sourcePractitioner != null) {
      copyPractioner(targetBundle, id, sourcePractitioner);
      return;
    }
    Patient sourcePatient = FhirUtils.getPatient(sourceBundle, id);
    if (sourcePatient != null) {
      copyPatient(targetBundle, id, sourcePatient);
      return;
    }
    log.warn("copyNoteAuthor {} not found!", id);
  }

  private void copyPatient(Bundle targetBundle, String id, Patient sourcePatient) {
    Patient targetPatient = FhirUtils.getPatient(targetBundle, id);
    if (targetPatient == null) {
      log.debug("Patient {} added", sourcePatient.getId());
      addEntry(targetBundle, "Patient/", sourcePatient);
    }
  }

  private void copyPractioner(Bundle targetBundle, String id, Practitioner sourcePractitioner) {
    Practitioner targetPractioner = FhirUtils.getPractitioner(targetBundle, id);
    if (targetPractioner == null) {
      log.debug("Practitioner {} added", sourcePractitioner.getId());
      addEntry(targetBundle, "Practitioner/", sourcePractitioner);
    }
  }

  private Bundle createAllergy(Bundle bundle, SectionComponent section, AllergyDTO dto) {
    return createAllergy(bundle, section, dto, 1);
  }

  private Bundle createAllergy(Bundle bundle, SectionComponent section, AllergyDTO dto, int entryNumber) {
    String entryNumberString = StringUtils.leftPad(String.valueOf(entryNumber), 4, '0');
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), entryNumberString);
    createOrganization(bundle, dto.getOrganization(), practitionerRole, entryNumberString);

    AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
    allergyIntolerance.setId("AllergyIntolerance-" + entryNumberString);
    allergyIntolerance.addIdentifier(createIdentifier());
    allergyIntolerance.setPatient(new Reference("Patient/Patient-0001"));
    allergyIntolerance.setRecorder(new Reference("PractitionerRole/PractitionerRole-" + entryNumberString));
    allergyIntolerance.setCode(dto.getAllergyCode().toCodeableConcept());
    allergyIntolerance.addCategory(AllergyIntoleranceCategory.MEDICATION);
    if (dto.getType() != null) {
      allergyIntolerance.setType(AllergyIntolerance.AllergyIntoleranceType.fromCode(dto.getType().getCode()));
    }

    allergyIntolerance
        .setCriticality(AllergyIntoleranceCriticality
            .fromCode(dto.getCriticality() != null ? dto.getCriticality().getCode() : null));

    allergyIntolerance.setClinicalStatus(dto.getClinicalStatus().toCodeableConcept());
    allergyIntolerance.setVerificationStatus(dto.getVerificationStatus().toCodeableConcept());

    Date dateOccurrenceDate = convertToDate(dto.getOccurrenceDate());
    allergyIntolerance.setLastOccurrence(dateOccurrenceDate);
    allergyIntolerance.setRecordedDate(dateOccurrenceDate);

    addEntry(bundle, "AllergyIntolerance/", allergyIntolerance);
    section.addEntry(new Reference("AllergyIntolerance/" + allergyIntolerance.getIdElement().getValue()));
    return bundle;
  }

  private Bundle createBundle(FhirContext ctx, boolean isVaccinationRecord) {
    BundleBuilder builder = new BundleBuilder(ctx);
    builder.setType("document");

    Bundle bundle = (Bundle) builder.getBundle();
    bundle.setTimestamp(new Date());
    bundle.setId("Bundle-0001");
    bundle.setIdentifier(createIdentifier());

    Meta meta = new Meta();
    meta.setLastUpdated(new Date());
    meta.setProfile(Arrays.asList(new CanonicalType(
        isVaccinationRecord ? FhirUtils.VACCINATION_RECORD_TYPE_URL : FhirUtils.VACCINATION_TYPE_URL)));
    bundle.setMeta(meta);

    return bundle;
  }

  private CodeableConcept createCodeableConcept(String code, String name, String system) {
    CodeableConcept concept = new CodeableConcept();
    concept.getCodingFirstRep().setCode(code);
    concept.getCodingFirstRep().setDisplay(name);
    concept.getCodingFirstRep().setSystem(system);

    return concept;
  }

  private void createComment(DomainResource resource, BaseDTO dto, String authorReference) {
    CommentDTO comment = getNewComment(dto);
    if (comment != null) {
      Annotation annotation = addNote(resource);

      if (annotation != null) {
        annotation.setTime(new Date());
        annotation.setAuthor(new Reference(authorReference));
        annotation.setText(comment.getText());
      }
    }
  }

  private Composition createComposition(Bundle bundle, BaseDTO dto, PatientIdentifier patientIdentifier) {
    Composition composition = new Composition();
    addEntry(bundle, "Composition/", composition);
    composition.setId("Composition-0001");
    boolean isVaccinationRecord = dto instanceof VaccinationRecordDTO;

    Meta meta = new Meta();
    meta.setProfile(Arrays.asList(new CanonicalType(
        isVaccinationRecord ? "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-composition-vaccination-record"
            : "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-composition-immunization-administration")));
    composition.setMeta(meta);
    composition.setLanguage("en-US");

    CodeableConcept compositionType =
        createCodeableConcept("41000179103", "Immunization record", "http://snomed.info/sct");
    composition.setType(compositionType);

    composition.setStatus(Composition.CompositionStatus.FINAL);
    composition.setIdentifier(createIdentifier());
    composition.setDate(new Date());

    if (dto.getAuthor() == null) {
      composition.addAuthor(new Reference("Practitioner/Practitioner-0001"));
    } else if (fhirConfig.isPractitioner(dto.getAuthor().getRole())) {
      composition.addAuthor(new Reference("Practitioner/Practitioner-author"));
      createPractitionerRole(bundle, dto.getAuthor(), "author");
    } else if (dto.getAuthor().getFullName().equals(patientIdentifier.getPatientInfo().getFullName())) {
      composition.addAuthor(new Reference("Patient/Patient-0001"));
    } else if (fhirConfig.isPatient(dto.getAuthor().getRole())) {
      composition.addAuthor(new Reference("Patient/Patient-author"));
      PatientIdentifier authorPatientIdentifier = new PatientIdentifier(null, null, null);
      authorPatientIdentifier.setPatientInfo(dto.getAuthor());
      createPatient(bundle, composition, authorPatientIdentifier, "Patient-author", false);
    } else {
      throw new TechnicalException("role:" + dto.getAuthor().getRole() + " not supported");
    }

    composition.setTitle("Immunization Administration");
    composition.setSubject(new Reference("Patient/Patient-0001"));
    composition.setConfidentiality(DocumentConfidentiality.N);

    Extension confidentialityExtension = composition.getConfidentialityElement().addExtension();
    confidentialityExtension
        .setUrl("http://fhir.ch/ig/ch-core/StructureDefinition/ch-ext-epr-confidentialitycode");
    CodeableConcept confidentiality = FhirUtils.defaultConfidentialityCode.toCodeableConcept();
    if (dto.getConfidentiality() != null) {
      confidentiality = dto.getConfidentiality().toCodeableConcept();
    }
    confidentialityExtension.setValue(confidentiality);

    CodeableConcept immunizationSectionType =
        createCodeableConcept("11369-6", "Hx of Immunization", "http://loinc.org");
    createSection(composition, SECTION_IMMUNIZATION_ID, "Immunization Administration", immunizationSectionType);

    CodeableConcept pastIllnessSectionType =
        createCodeableConcept("11348-0", "Hx of Past illness", "http://loinc.org");
    createSection(composition, SECTION_PAST_ILLNESSES_ID, "Undergone illnesses for immunization",
        pastIllnessSectionType);

    CodeableConcept allergyIntolerancesSectionType =
        createCodeableConcept("48765-2", "Allergies and adverse reactions Document", "http://loinc.org");
    createSection(composition, SECTION_ALLERGIES_ID, "Allergies", allergyIntolerancesSectionType);

    return composition;
  }

  private void createCrossReference(Composition compostion, DomainResource resource,
      DomainResource updatedResource) {
    Extension extension = updatedResource.addExtension();
    extension.setUrl(
        "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-immunization-recorder-reference");
    extension.setValue(compostion.getAuthorFirstRep());

    extension = updatedResource.addExtension();
    extension.setUrl("http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference");
    extension.addExtension("entry", new Reference(getIdentifierValue(resource)));
    extension.addExtension("document", new Reference(getIdentifierValue(compostion)));
    extension.addExtension("relationcode", new CodeType("replaces"));
  }

  private Identifier createIdentifier() {
    Identifier identifier = new Identifier();
    identifier.setValue(DEFAULT_ID_PREFIX + UUID.randomUUID());
    identifier.setSystem(DEFAULT_SYSTEM_IDENTIFIER);
    return identifier;
  }

  private Organization createOrganization(Bundle bundle, String organizationName, PractitionerRole practitionerRole,
      String entryNumber) {
    Organization organization = null;
    if (organizationName != null) {
      organization = new Organization();
      organization.setId("Organization-" + entryNumber);
      organization.setName(organizationName);
      practitionerRole.setOrganization(new Reference("Organization/Organization-" + entryNumber));
    }

    addEntry(bundle, "Organization/", organization);
    return organization;
  }

  private Bundle createPastIllness(Bundle bundle, SectionComponent section, PastIllnessDTO dto) {
    return createPastIllness(bundle, section, dto, 1);
  }

  private Bundle createPastIllness(Bundle bundle, SectionComponent section, PastIllnessDTO dto, int entryNumber) {
    String entryNumberString = StringUtils.leftPad(String.valueOf(entryNumber), 4, '0');
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), entryNumberString);
    createOrganization(bundle, dto.getOrganization(), practitionerRole, entryNumberString);

    Condition condition = new Condition();
    condition.setId("Condition-" + entryNumberString);
    condition.setSubject(new Reference("Patient/Patient-0001"));
    condition.addIdentifier(createIdentifier());
    condition.setRecorder(new Reference("PractitionerRole/PractitionerRole-" + entryNumberString));

    condition.setCode(dto.getIllnessCode().toCodeableConcept());
    condition.setClinicalStatus(dto.getClinicalStatus().toCodeableConcept());
    condition.setVerificationStatus(dto.getVerificationStatus().toCodeableConcept());

    condition.setRecordedDate(convertToDate(dto.getRecordedDate()));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    condition.setOnset(new DateTimeType(formatter.format(dto.getBegin())));
    if (dto.getEnd() != null) {
      condition.setAbatement(new DateTimeType(formatter.format(dto.getEnd())));
    }

    addEntry(bundle, "Condition/", condition);
    section.addEntry(new Reference("Condition/" + condition.getIdElement().getValue()));
    return bundle;
  }

  private void createPatient(Bundle bundle, Composition composition, PatientIdentifier patientIdentifier,
      String patientId, boolean useSPID) {

    Identifier identifier;
    if (useSPID) {
      identifier = new Identifier();
      identifier.setValue(patientIdentifier.getSpidExtension());
      identifier.setSystem(patientIdentifier.getSpidRootAuthority());
    } else {
      identifier = createIdentifier();
    }
    Patient patient = new Patient();
    patient.setId(patientId);
    patient.setIdentifier(List.of(identifier));

    HumanNameDTO patientInfo = patientIdentifier.getPatientInfo();
    if (patientInfo != null) {
      patient.addName().setFamily(patientInfo.getLastName()).addGiven(patientInfo.getFirstName())
          .addPrefix(patientInfo.getPrefix());
      patient.setBirthDate(convertToDate(patientInfo.getBirthday()));
      patient.setGender(
          patientInfo.getGender() != null ? AdministrativeGender.valueOf(patientInfo.getGender())
              : null);
    } else {
      patient.addName().setFamily("emptyFamily").addGiven("emptyGiven").addPrefix("emptyPrefix");
      patient.setBirthDate(new Date(0));
      patient.setGender(AdministrativeGender.UNKNOWN);
    }
    patient.setActive(true);

    addEntry(bundle, "Patient/", patient);
    composition.setSubject(new Reference("Patient/" + patient.getIdElement().getValue()));
  }

  private Practitioner createPractitioner(HumanNameDTO practitionerName, String entryNumberString) {
    Practitioner practitioner = new Practitioner();
    practitioner.setId("Practitioner-" + entryNumberString);
    Identifier identifier;
    if (practitionerName.getGln() != null) {
      identifier = new Identifier();
      identifier.setValue(practitionerName.getGln());
      identifier.setSystem(DEFAULT_SYSTEM_IDENTIFIER);
    } else {
      identifier = createIdentifier();
    }

    practitioner.setIdentifier(Arrays.asList(identifier));
    practitioner.addName().setFamily(practitionerName.getLastName())
        .addGiven(practitionerName.getFirstName())
        .addPrefix(practitionerName.getPrefix());
    return practitioner;
  }

  private PractitionerRole createPractitionerRole(Bundle bundle, HumanNameDTO practitionerName,
      String entryNumberString) {
    Practitioner practitioner = createPractitioner(practitionerName, entryNumberString);

    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId("PractitionerRole-" + entryNumberString);
    practitionerRole.setPractitioner(new Reference("Practitioner/Practitioner-" + entryNumberString));

    addEntry(bundle, "Practitioner/", practitioner);
    addEntry(bundle, "PractitionerRole/", practitionerRole);
    return practitionerRole;
  }

  private void createSection(Composition composition, String id, String title, CodeableConcept sectionType) {
    SectionComponent sectionComponent = new SectionComponent();
    sectionComponent.setId(id);
    sectionComponent.setTitle(title);
    sectionComponent.setCode(sectionType);

    Narrative narrative = new Narrative();
    narrative.setStatus(Narrative.NarrativeStatus.GENERATED);
    narrative.setDivAsString("empty");
    sectionComponent.setText(narrative);
    composition.addSection(sectionComponent);
  }


  private Bundle createVaccination(Bundle bundle, SectionComponent section, VaccinationDTO vaccinationDTO) {
    return createVaccination(bundle, section, vaccinationDTO, 1);
  }

  private Bundle createVaccination(Bundle bundle, SectionComponent section, VaccinationDTO dto,
      int entryNumber) {
    String entryNumberString = StringUtils.leftPad(String.valueOf(entryNumber), 4, '0');
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), entryNumberString);
    createOrganization(bundle, dto.getOrganization(), practitionerRole, entryNumberString);

    Immunization immunization = new Immunization();
    immunization.setId("Immunization-" + entryNumberString);
    immunization.addIdentifier(createIdentifier());
    immunization.setPatient(new Reference("Patient/Patient-0001"));
    immunization.setVaccineCode(dto.getVaccineCode().toCodeableConcept());
    immunization.setLotNumber(dto.getLotNumber());

    immunization.addPerformer(
        new Immunization.ImmunizationPerformerComponent(
            new Reference("PractitionerRole/PractitionerRole-" + entryNumberString)));
    immunization.setOccurrence(new DateTimeType(Date.from(
        Instant.from(dto.getOccurrenceDate().atStartOfDay(ZoneId.systemDefault())))));

    Immunization.ImmunizationProtocolAppliedComponent ipac =
        new Immunization.ImmunizationProtocolAppliedComponent();
    ipac.setDoseNumber(new PositiveIntType(dto.getDoseNumber()));
    if (dto.getTargetDiseases() != null) {
      dto.getTargetDiseases()
          .forEach(disease -> ipac.addTargetDisease(disease.toCodeableConcept()));
    }

    immunization.addProtocolApplied(ipac);

    if (dto.getReason() != null) {
      immunization.addReasonCode(dto.getReason().toCodeableConcept());
    }
    immunization.setStatus(Immunization.ImmunizationStatus.fromCode(dto.getStatus().getCode()));

    addEntry(bundle, "Immunization/", immunization);
    section.addEntry(new Reference("Immunization/" + immunization.getIdElement().getValue()));

    log.debug("Bundle:{}", ToStringBuilder.reflectionToString(bundle, ToStringStyle.JSON_STYLE));

    return bundle;
  }

  private Bundle createVaccinationRecord(Bundle bundle, Composition composition,
      VaccinationRecordDTO vaccinationRecord) {
    int entryNumber = 1;
    for (VaccinationDTO vaccination : vaccinationRecord.getVaccinations()) {
      createVaccination(bundle, getSection(composition, SECTION_IMMUNIZATION_ID), vaccination, entryNumber);
      entryNumber++;
    }

    for (PastIllnessDTO pastIllness : vaccinationRecord.getPastIllnesses()) {
      createPastIllness(bundle, getSection(composition, SECTION_PAST_ILLNESSES_ID), pastIllness, entryNumber);
      entryNumber++;
    }

    for (AllergyDTO allergy : vaccinationRecord.getAllergies()) {
      createAllergy(bundle, getSection(composition, SECTION_ALLERGIES_ID), allergy, entryNumber);
      entryNumber++;
    }

    return bundle;
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
        author = FhirUtils.getAuthor(bundle, reference);
      }
    }
    return author;
  }

  private String getCrossReference(DomainResource domainResource) {
    Extension crossRefExtension = domainResource.getExtensionByUrl(
        "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference");
    if (crossRefExtension != null) {
      Extension extension = crossRefExtension.getExtensionByUrl("entry");
      Reference reference = (Reference) extension.getValue();
      return reference.getReference().replace(DEFAULT_ID_PREFIX, "");
    }

    return null;
  }

  private String getIdentifierValue(DomainResource resource) {
    String result = null;
    if (resource instanceof Immunization vaccination) {
      result = vaccination.getIdentifierFirstRep().getValue();
    } else if (resource instanceof Condition condition) {
      result = condition.getIdentifierFirstRep().getValue();
    } else if (resource instanceof AllergyIntolerance allergy) {
      result = allergy.getIdentifierFirstRep().getValue();
    } else if (resource instanceof Patient patient) {
      result = patient.getIdentifierFirstRep().getValue();
    } else if (resource instanceof Practitioner practitioner) {
      result = practitioner.getIdentifierFirstRep().getValue();
    } else if (resource instanceof Composition composition) {
      result = composition.getIdentifier().getValue();
    }

    return result;
  }

  private CommentDTO getNewComment(BaseDTO newDto) {
    // the new comment, if available, has no date set
    CommentDTO newComment = newDto.getComments() != null ? newDto.getComments().stream()
        .filter(comment -> comment.getDate() == null)
        .findAny().orElse(null) : null;

    return newComment;
  }

  private Resource getReference(Bundle bundle, Class<?> clazz) {
    return bundle.getEntry().stream()
        .filter(entry -> clazz.isAssignableFrom(entry.getResource().getClass()))
        .findFirst().get().getResource();
  }

  private SectionComponent getSection(Composition composition, String sectionId) {
    return composition.getSection().stream()
        .filter(section -> sectionId.equals(section.getIdElement().getValue()))
        .findAny().get();
  }

  private void markResourceDeleted(DomainResource resource) {
    CodeableConcept enteredInError = createCodeableConcept(ENTERED_IN_ERROR, "Entered in Error", null);
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
}
