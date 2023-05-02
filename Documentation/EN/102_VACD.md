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

see [CH VACD Past Illness](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.recordedDater.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses.html)

### Medical Problems
TODO
