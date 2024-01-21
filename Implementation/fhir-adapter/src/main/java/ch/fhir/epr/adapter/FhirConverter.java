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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
        copyNoteReferenceIfKnown(targetBundle, sourceBundle, note);
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
    return createBundle(ctx, patientIdentifier, dto, false);
  }

  @Override
  public Bundle createBundle(FhirContext ctx, PatientIdentifier patientIdentifier, BaseDTO dto,
      boolean forceImmunizationAdministrationDocument) {
    Bundle bundle = createBundle(ctx, !forceImmunizationAdministrationDocument && dto instanceof VaccinationRecordDTO);
    Composition composition =
        createComposition(bundle, dto, patientIdentifier, forceImmunizationAdministrationDocument);
    createPatient(bundle, composition, patientIdentifier, "Patient-0001", true);

    if (dto instanceof VaccinationRecordDTO) {
      return createVaccinationRecord(bundle, (VaccinationRecordDTO) dto);
    }

    DomainResource updatedResource = null;
    if (dto instanceof VaccinationDTO vaccination) {
      bundle = createVaccination(bundle, vaccination);
      updatedResource = (DomainResource) getReference(bundle, Immunization.class);
    } else if (dto instanceof AllergyDTO allergy) {
      bundle = createAllergy(bundle, allergy);
      updatedResource = (DomainResource) getReference(bundle, AllergyIntolerance.class);
    } else {
      if (dto instanceof PastIllnessDTO pastIllness) {
        bundle = createPastIllness(bundle, pastIllness);
      } else if (dto instanceof MedicalProblemDTO medicalProblem) {
        bundle = createMedicalProblem(bundle, medicalProblem);
      } else {
        throw new TechnicalException("toBundle not supported for class " + dto.getClass().getSimpleName());
      }
      updatedResource = (DomainResource) getReference(bundle, Condition.class);
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
      commentDTOs.add(new CommentDTO(convertToLocalDateTime(note.getTime()), author.getFullName(), note.getText()));
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

    allergyDTO.setDeleted(FhirConverterIfc.ENTERED_IN_ERROR.equalsIgnoreCase(verificationStatus.getCode()));
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

    dto.setDeleted(FhirConverterIfc.ENTERED_IN_ERROR.equalsIgnoreCase(verificationStatus.getCode()));
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

    PastIllnessDTO pastIllnessDTO =
        new PastIllnessDTO(id, code, clinicalStatus, verificationStatus, recordedData,
            begin, end, recorder, null, organization);

    pastIllnessDTO.setDeleted(FhirConverterIfc.ENTERED_IN_ERROR.equalsIgnoreCase(verificationStatus.getCode()));
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
        .map(targetDisease -> FhirUtils.toValueDTO(targetDisease))
        .collect(Collectors.toList());

    String lotNumber = immunization.getLotNumber();
    ValueDTO reason = immunization.getReasonCode().stream().findFirst()
        .map(reasonCode -> FhirUtils.toValueDTO(reasonCode)).orElse(null);

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
        status);

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
    targetReference.setReference(FhirUtils.getIdentifierValue(compostion));
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

  private Bundle createAllergy(Bundle bundle, AllergyDTO dto) {
    return createAllergy(bundle, dto, 1);
  }

  private Bundle createAllergy(Bundle bundle, AllergyDTO dto, int entryNumber) {
    String entryNumberString = StringUtils.leftPad(String.valueOf(entryNumber), 4, '0');
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), null, entryNumberString);
    createOrganization(bundle, dto.getOrganization(), practitionerRole, entryNumberString);

    AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
    allergyIntolerance.setId("AllergyIntolerance-" + entryNumberString);
    allergyIntolerance.addIdentifier(createIdentifier());
    allergyIntolerance.setPatient(new Reference("Patient/Patient-0001"));
    allergyIntolerance.setRecorder(new Reference("PractitionerRole/PractitionerRole-" + entryNumberString));
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

    addEntry(bundle, "AllergyIntolerance/", allergyIntolerance);
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

  private Composition createComposition(Bundle bundle, BaseDTO dto, PatientIdentifier patientIdentifier,
      boolean forceImmunizationAdministrationDocument) {
    Composition composition = new Composition();
    composition.setId("Composition-0001");
    addEntry(bundle, "Composition/", composition);
    boolean createVaccinationRecord = !forceImmunizationAdministrationDocument && dto instanceof VaccinationRecordDTO;

    composition.setMeta(createMeta(
        createVaccinationRecord ? FhirConstants.META_VACD_COMPOSITION_VAC_REC_URL
            : FhirConstants.META_VACD_COMPOSITION_IMMUN_URL));
    composition.setLanguage("en-US");

    CodeableConcept compositionType =
        createCodeableConcept("41000179103", "Immunization record", FhirConstants.SNOMED_SYSTEM_URL);
    composition.setType(compositionType);

    composition.setStatus(Composition.CompositionStatus.FINAL);
    composition.setIdentifier(createIdentifier());
    composition.setDate(new Date());

    if (fhirConfig.isPractitioner(dto.getAuthor().getRole())) {
      composition.addAuthor(new Reference("Practitioner/Practitioner-author"));
      fillAuthor(bundle, dto);
    } else if (dto.getAuthor().getUser().getFullName().equals(patientIdentifier.getPatientInfo().getFullName())) {
      composition.addAuthor(new Reference("Patient/Patient-0001"));
    } else if (fhirConfig.isPatient(dto.getAuthor().getRole())) {
      composition.addAuthor(new Reference("Patient/Patient-author"));
      PatientIdentifier authorPatientIdentifier = new PatientIdentifier(null, null, null);
      authorPatientIdentifier.setPatientInfo(dto.getAuthor().getUser());
      createPatient(bundle, composition, authorPatientIdentifier, "Patient-author", false);
    } else {
      throw new TechnicalException("role:" + dto.getAuthor().getRole() + " not supported");
    }

    composition.setTitle(createVaccinationRecord ? "Vaccination Record" : "Immunization Administration");
    composition.setSubject(new Reference("Patient/Patient-0001"));
    composition.setConfidentiality(DocumentConfidentiality.N);

    Extension confidentialityExtension = composition.getConfidentialityElement().addExtension();
    confidentialityExtension.setUrl(FhirConstants.CONFIDENTIALITY_CODE_EXTENSION_URL);
    CodeableConcept confidentiality = FhirUtils.toCodeableConcept(FhirConstants.DEFAULT_CONFIDENTIALITY_CODE);
    if (dto.getConfidentiality() != null) {
      String system = dto.getConfidentiality().getSystem();
      // replace definition by snomed url to be conform to new validator - only used for bundle not for
      // XDS metadata
      if (FhirConstants.CONFIDENTIALITY_CODE_SYSTEM_NORMAL_RESTRICTED.equals(system)
          || FhirConstants.CONFIDENTIALITY_CODE_SYSTEM_SECRET.equals(system)) {
        dto.getConfidentiality().setSystem(FhirConstants.SNOMED_SYSTEM_URL);
      }
      confidentiality = FhirUtils.toCodeableConcept(dto.getConfidentiality());
    }
    confidentialityExtension.setValue(confidentiality);

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
    extension.addExtension("entry", new Reference(FhirUtils.getIdentifierValue(resource)));
    extension.addExtension("document", new Reference(FhirUtils.getIdentifierValue(compostion)));
    extension.addExtension("relationcode", new CodeType("replaces"));
  }

  private Identifier createIdentifier() {
    Identifier identifier = new Identifier();
    identifier.setValue(FhirConstants.DEFAULT_ID_PREFIX + UUID.randomUUID());
    identifier.setSystem(FhirConstants.DEFAULT_URN_SYSTEM_IDENTIFIER);
    return identifier;
  }

  private Bundle createMedicalProblem(Bundle bundle, MedicalProblemDTO dto) {
    return createMedicalProblem(bundle, dto, 1);
  }

  private Bundle createMedicalProblem(Bundle bundle, MedicalProblemDTO dto, int entryNumber) {
    String entryNumberString = StringUtils.leftPad(String.valueOf(entryNumber), 4, '0');
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), null, entryNumberString);
    createOrganization(bundle, dto.getOrganization(), practitionerRole, entryNumberString);

    Condition condition = new Condition();
    condition.setId("Condition-" + entryNumberString);
    condition.setSubject(new Reference("Patient/Patient-0001"));
    condition.addIdentifier(createIdentifier());
    condition.setRecorder(new Reference("PractitionerRole/PractitionerRole-" + entryNumberString));

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

    addEntry(bundle, "Condition/", condition);
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

  private Organization createOrganization(Bundle bundle, String organizationName, PractitionerRole practitionerRole,
      String entryNumber) {
    Organization organization = new Organization();
    organization.setMeta(createMeta(FhirConstants.META_ORGANIZATION_URL));
    organization.setId("Organization-" + entryNumber);
    organization.setName(organizationName != null ? organizationName : "-");

    practitionerRole.setOrganization(new Reference("Organization/Organization-" + entryNumber));

    addEntry(bundle, "Organization/", organization);
    return organization;
  }

  private Bundle createPastIllness(Bundle bundle, PastIllnessDTO dto) {
    return createPastIllness(bundle, dto, 1);
  }

  private Bundle createPastIllness(Bundle bundle, PastIllnessDTO dto, int entryNumber) {
    String entryNumberString = StringUtils.leftPad(String.valueOf(entryNumber), 4, '0');
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), null, entryNumberString);
    createOrganization(bundle, dto.getOrganization(), practitionerRole, entryNumberString);

    Condition condition = new Condition();
    condition.setId("Condition-" + entryNumberString);
    condition.setSubject(new Reference("Patient/Patient-0001"));
    condition.addIdentifier(createIdentifier());
    condition.setRecorder(new Reference("PractitionerRole/PractitionerRole-" + entryNumberString));

    condition.setCode(FhirUtils.toCodeableConcept(dto.getCode()));
    condition.setClinicalStatus(FhirUtils.toCodeableConcept(dto.getClinicalStatus()));
    condition.setVerificationStatus(FhirUtils.toCodeableConcept(dto.getVerificationStatus()));

    condition.setRecordedDate(convertToDate(dto.getRecordedDate()));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    condition.setOnset(new DateTimeType(formatter.format(dto.getBegin())));
    if (dto.getEnd() != null) {
      condition.setAbatement(new DateTimeType(formatter.format(dto.getEnd())));
    }

    addEntry(bundle, "Condition/", condition);
    createSectionIfNecessary(FhirUtils.getResource(Composition.class, bundle), SectionType.PAST_ILLNESSES,
        condition);
    return bundle;
  }

  @SuppressWarnings("deprecation")
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
      // For system generated data birthdate should be 1900-01-01
      patient.setBirthDate(new Date(0, 0, 1));
      patient.setGender(AdministrativeGender.UNKNOWN);
    }
    patient.setActive(true);

    addEntry(bundle, "Patient/", patient);
    composition.setSubject(new Reference("Patient/" + patient.getIdElement().getValue()));
  }

  private Practitioner createPractitioner(HumanNameDTO practitionerName, String gln, String entryNumberString) {
    Practitioner practitioner = new Practitioner();
    practitioner.setId("Practitioner-" + entryNumberString);
    Identifier identifier = new Identifier();
    identifier.setValue(gln != null ? gln : FhirConstants.DEFAULT_PRACTITIONER_CODE);
    identifier.setSystem(FhirConstants.FIXED_PRACTITIONER_SYSTEM);

    practitioner.setIdentifier(List.of(identifier));
    practitioner.addName().setFamily(practitionerName.getLastName())
        .addGiven(practitionerName.getFirstName())
        .addPrefix(practitionerName.getPrefix());

    practitioner.setMeta(createMeta(FhirConstants.META_CORE_PRACTITIONER_URL));
    return practitioner;
  }

  private PractitionerRole createPractitionerRole(Bundle bundle, HumanNameDTO practitionerName, String gln,
      String entryNumberString) {
    Practitioner practitioner = createPractitioner(practitionerName, gln, entryNumberString);

    PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId("PractitionerRole-" + entryNumberString);
    practitionerRole.setPractitioner(new Reference("Practitioner/Practitioner-" + entryNumberString));
    practitionerRole.setMeta(createMeta(FhirConstants.META_PRACTITIONER_ROLE_URL));

    addEntry(bundle, "Practitioner/", practitioner);
    addEntry(bundle, "PractitionerRole/", practitionerRole);
    return practitionerRole;
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
      sectionComponent.addEntry(new Reference(resource));
    }
  }

  private Bundle createVaccination(Bundle bundle, VaccinationDTO vaccinationDTO) {
    return createVaccination(bundle, vaccinationDTO, 1);
  }

  private Bundle createVaccination(Bundle bundle, VaccinationDTO dto, int entryNumber) {
    String entryNumberString = StringUtils.leftPad(String.valueOf(entryNumber), 4, '0');
    PractitionerRole practitionerRole = createPractitionerRole(bundle, dto.getRecorder(), null, entryNumberString);
    createOrganization(bundle, dto.getOrganization(), practitionerRole, entryNumberString);

    Immunization immunization = new Immunization();
    immunization.setId("Immunization-" + entryNumberString);
    immunization.addIdentifier(createIdentifier());
    immunization.setPatient(new Reference("Patient/Patient-0001"));
    immunization.setVaccineCode(FhirUtils.toCodeableConcept(dto.getCode()));
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
          .forEach(disease -> ipac.addTargetDisease(FhirUtils.toCodeableConcept(disease)));
    }

    immunization.addProtocolApplied(ipac);

    if (dto.getReason() != null) {
      immunization.addReasonCode(FhirUtils.toCodeableConcept(dto.getReason()));
    }
    immunization.setStatus(Immunization.ImmunizationStatus.fromCode(dto.getStatus().getCode()));
    immunization.setMeta(createMeta(FhirConstants.META_VACD_IMMUNIZATION_URL));

    addEntry(bundle, "Immunization/", immunization);
    createSectionIfNecessary(FhirUtils.getResource(Composition.class, bundle), SectionType.IMMUNIZATION,
        immunization);
    log.debug("Bundle:{}", ToStringBuilder.reflectionToString(bundle, ToStringStyle.JSON_STYLE));

    return bundle;
  }

  private Bundle createVaccinationRecord(Bundle bundle, VaccinationRecordDTO vaccinationRecord) {
    int entryNumber = 1;
    for (VaccinationDTO vaccination : vaccinationRecord.getVaccinations()) {
      if (!vaccination.isHasErrors()) {
        createVaccination(bundle, vaccination, entryNumber);
        entryNumber++;
      }
    }

    for (PastIllnessDTO pastIllness : vaccinationRecord.getPastIllnesses()) {
      if (!pastIllness.isHasErrors()) {
        createPastIllness(bundle, pastIllness, entryNumber);
        entryNumber++;
      }
    }

    for (AllergyDTO allergy : vaccinationRecord.getAllergies()) {
      if (!allergy.isHasErrors()) {
        createAllergy(bundle, allergy, entryNumber);
        entryNumber++;
      }
    }

    for (MedicalProblemDTO medicalProblem : vaccinationRecord.getMedicalProblems()) {
      if (!medicalProblem.isHasErrors()) {
        createMedicalProblem(bundle, medicalProblem, entryNumber);
        entryNumber++;
      }
    }

    return bundle;
  }

  private void fillAuthor(Bundle bundle, BaseDTO dto) {
    HumanNameDTO author = dto.getAuthor().getUser();
    String gln = dto.getAuthor().getGln();
    if ("ASS".equals(dto.getAuthor().getRole())) {
      // use information of the principal instead of the ASS.
      String principal = dto.getAuthor().getPrincipalName();
      int lastIndexOfSpace = principal.lastIndexOf(" ");
      String given = principal.substring(0, lastIndexOfSpace);
      String family = principal.substring(lastIndexOfSpace + 1);
      author = new HumanNameDTO(given, family, null, null, null);
      gln = dto.getAuthor().getPrincipalId();
    }

    createPractitionerRole(bundle, author, gln, "author");
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
    Extension crossRefExtension = domainResource.getExtensionByUrl(
        "http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference");
    if (crossRefExtension != null) {
      Extension extension = crossRefExtension.getExtensionByUrl("entry");
      Reference reference = (Reference) extension.getValue();
      return reference.getReference().replace(FhirConstants.DEFAULT_ID_PREFIX, "");
    }

    return null;
  }

  private CommentDTO getNewComment(BaseDTO newDto) {
    return newDto.getComments() != null ? newDto.getComments().stream()
        .filter(comment -> comment.getDate() == null)
        .findAny().orElse(null) : null;
  }

  private Resource getReference(Bundle bundle, Class<?> clazz) {
    return bundle.getEntry().stream()
        .filter(entry -> clazz.isAssignableFrom(entry.getResource().getClass()))
        .findFirst().get().getResource();
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
