## Visualization of vaccination data

### Vaccination

- Datum - [Immunization.occurenceDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.occurrence[x]:occurrenceDateTime)
- Impfstoff - CodeableConcept, [Immunization.vaccineCode](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.vaccineCode)
- Impfschutz - Codeable Concepts, [Immunization.protocolApplied.targetDisease](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.protocolApplied.targetDisease)
- Dosis - [Immunization.protocolApplied.doseNumberPositiveInt](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.protocolApplied.doseNumber[x]:doseNumberPositiveInt)
- Status - CodeableConcept, [Immunization.status](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.status)
- Lotnummer - [Immunization.lotnumber](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.lotNumber)
- Grund der Impfung - CodeableConcept, [Immunization.reasonCode](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.reasonCode)
- Geimpft von - [Immunization.performer -> PractitionerRole.practitioner](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.performer), resolved per reference.
- Organisation - [Immunization.performer -> PractitionerRole.organization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.performer), resolved per reference.
- Kommentar - [Immunization.note](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization-definitions.html#Immunization.note) with time, text und author.name

siehe [CH VACD Immunization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization.html)

### Adverse effects

- Datum - [AllergyIntolerance.recordedDate](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.recordedDate)
- Nebenwirkung - [AllergyIntolerance.code](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.code)
- Kritikalität - [AllergyIntolerance.criticality](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.criticality)
- Klinischer Status - CodeableConcept, [AllergyIntolerance.clinicalStatus](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.clinicalStatus)
- Verifiziert - CodeableConcept, [AllergyIntolerance.verificationStatus](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.verificationStatus)
- Art - CodeableConcept, [AllergyIntolerance.type](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.type)
- Erfasst von - [AllergyIntolerance.recorder -> PractitionerRole.practitioner](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.recorder), resolved per reference.
- Organisation - [AllergyIntolerance.recorder -> PractitionerRole.organization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.recorder), resolved per reference.
- Kommentar - [AllergyIntolerance.note](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.note) with time, text und author.name
- *Kategorie* - [AllergyIntolerance.category](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances-definitions.html#AllergyIntolerance.category) (fix value set to **medical**)

see [CH VACD AllergyIntolerance](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances.html)

### Infectious deseases

- Datum - [PastIllness.recordedDate](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.recordedDate)
- Infektionskrankheit - CodeableConcept [PastIllness.code](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.recordedDate)
- Beginn - [PastIllness.onset[0].onsetDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.onset)
- Ende - [PastIllness.abatement[0].abatementDateTime](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.abatement)
- Klinischer Status - CodeableConcept, [PastIllness.clinicalStatus](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.clinicalStatus)
- Verifiziert - CodeableConcept, [PastIllness.verificationStatus](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.verificationStatus)
- Erfasst von - [PastIllness.recorder -> PractitionerRole.practitioner](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.recorder), resolved per reference.
- Organisation - [PastIllness.recorder -> PractitionerRole.organization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.recorder), resolved per reference.
- Kommentar - [PastIllness.note](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.note) with time, text und author.name

see [CH VACD Past Illness](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses-definitions.html#Condition.recordedDater.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses.html)

### Medical Problems 
TODO
