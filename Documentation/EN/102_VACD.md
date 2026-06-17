## Visualization of vaccination data

### Vaccination

- Date of vaccination - [Immunization.occurenceDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.occurrence[x]:occurrenceDateTime)
- Vaccine - CodeableConcept, [Immunization.vaccineCode](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.vaccineCode)
- Vaccination protection - Codeable Concepts, [Immunization.protocolApplied.targetDisease](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.protocolApplied.targetDisease)
- Dose - [Immunization.protocolApplied.doseNumberPositiveInt](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.protocolApplied.doseNumber[x]:doseNumberPositiveInt)
- Lot number - [Immunization.lotnumber](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.lotNumber)
- Reason for vaccination - CodeableConcept, [Immunization.reasonCode](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.reasonCode)
- Performer - [Immunization.performer -> PractitionerRole.practitioner](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.performer), resolved per reference.
- organization - [Immunization.performer -> PractitionerRole.organization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.performer), resolved per reference.
- Comments - [Immunization.note](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.note) with time, text und author.name

siehe [CH VACD Immunization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization.html)

### Basic immunization

- Assumed date of immunization - [BasicImmunization.onsetDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-basic-immunization-definitions.html#Condition.onset)
- Date of determination - [BasicImmunization.recordedDate](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-basic-immunization-definitions.html#Condition.recordedDate)
- Basic immunization - CodeableConcept, [BasicImmunization.code](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-basic-immunization-definitions.html#Condition.code)
- Confidentiality - [Composition.confidentiality](http://hl7.org/fhir/R4/composition-definitions.html#Composition.confidentiality) with CH EPR confidentiality code extension
- Comments - [BasicImmunization.note](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-basic-immunization-definitions.html#Condition.note) with time, text und author.name
- *Category* - [BasicImmunization.category](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-basic-immunization-definitions.html#Condition.category) (not displayed. Fixed value set to **encounter-diagnosis**)
- *Clinical status* - [BasicImmunization.clinicalStatus](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-basic-immunization-definitions.html#Condition.clinicalStatus) (not displayed. Fixed value set to **active**)
- *Verification status* - [BasicImmunization.verificationStatus](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-basic-immunization-definitions.html#Condition.verificationStatus) (not displayed. Automatically set according to the validation status)

see [CH VACD Basic Immunization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-basic-immunization.html)

### Adverse effects

- Date - [AllergyIntolerance.recordedDate](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.recordedDate)
- Adverse effect with - [AllergyIntolerance.code](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.code)
- Recorded by - [AllergyIntolerance.recorder -> PractitionerRole.practitioner](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.recorder), resolved per reference.
- Organization - [AllergyIntolerance.recorder -> PractitionerRole.organization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.recorder), resolved per reference.
- Comment - [AllergyIntolerance.note](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.note) with time, text und author.name
- *Kategorie* - [AllergyIntolerance.category](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.category) (not displayed. Fix value set to **medical**)

see [CH VACD AllergyIntolerance](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances.html)

### Infectious deseases

- Date of diagnosis - [PastIllness.recordedDate](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.recordedDate)
- Medical Problem - CodeableConcept [PastIllness.code](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.code)
- Begin - [PastIllness.onset[0].onsetDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.onset)
- End - [PastIllness.abatement[0].abatementDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.abatement)
- Recorded by - [PastIllness.recorder -> PractitionerRole.practitioner](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.recorder), resolved per reference.
- Organization - [PastIllness.recorder -> PractitionerRole.organization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.recorder), resolved per reference.
- Comments - [PastIllness.note](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.note) with time, text und author.name

see [CH VACD Past Illness](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html)

### Laboratory and serology

- Date of determination - [LaboratorySerology.effectiveDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.effective[x]:effectiveDateTime)
- Laboratory/Serology - CodeableConcept, [LaboratorySerology.code](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.code)
- Value - [LaboratorySerology.valueQuantity](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.value[x]:valueQuantity)
- Unit - [LaboratorySerology.valueQuantity.unit](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.value[x]:valueQuantity) and [LaboratorySerology.valueQuantity.code](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.value[x]:valueQuantity), automatically derived from the selected laboratory/serology code.
- Recorded by - [LaboratorySerology.performer -> PractitionerRole.practitioner](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.performer), resolved per reference.
- Organization - [LaboratorySerology.performer -> PractitionerRole.organization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.performer), resolved per reference.
- Confidentiality - [Composition.confidentiality](http://hl7.org/fhir/R4/composition-definitions.html#Composition.confidentiality) with CH EPR confidentiality code extension
- Comments - [LaboratorySerology.note](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.note) with time, text und author.name
- *Status* - [LaboratorySerology.status](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.status) (not displayed. Automatically set to **final** for create/update and to **entered-in-error** for delete)
- *Verification status* - [LaboratorySerology.extension:verificationStatus](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology-definitions.html#Observation.extension:verificationStatus) (not displayed. Automatically set according to the validation status)

see [CH VACD Laboratory and Serology](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-laboratory-serology.html)

### Medical Problems
- Date of diagnosis - [MedicalProblems.recordedDate](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-medical-problems-definitions.html#Condition.recordedDate)
- Medical Problem - CodeableConcept [MedicalProblems.code](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-medical-problems-definitions.html#Condition.code)
- Begin - [MedicalProblems.onset[0].onsetDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-medical-problems-definitions.html#Condition.onset)
- End - [Medical Problems.abatement[0].abatementDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-medical-problems-definitions.html#Condition.abatement)
- Recorded by - [Medical Problems.recorder -> PractitionerRole.practitioner](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-medical-problems-definitions.html#Condition.recorder), resolved per reference.
- Organization - [Medical Problems.recorder -> PractitionerRole.organization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-medical-problems-definitions.html#Condition.recorder), resolved per reference.
- Comments - [Medical Problems.note](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-medical-problems-definitions.html#Condition.note) with time, text und author.name

see [CH VACD Medical Problems](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-medical-problems-definitions.html)
