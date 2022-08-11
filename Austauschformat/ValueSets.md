## Value Sets

Es müssen die folgenden Value Sets verwendet werden:


### Impfungen

- Impfstoff (vaccineCode) - [Swissmedic code for vaccine code](http://fhir.ch/ig/ch-vacd/ValueSet-ch-vacd-vaccines-vs.html)
- Impfschutz (targetDesease) - [TargetDiseasesAndIllnessesUndergoneForImmunization](http://fhir.ch/ig/ch-vacd/ValueSet-ch-vacd-targetdiseasesandillnessesundergoneforimmunization-vs.html)
- Status (status) - [ImmunizationStatusCodes](http://hl7.org/fhir/R4/valueset-immunization-status.html)
- Grund für die Impfung (reasonCode) - [ImmunizationReasonCodes](http://hl7.org/fhir/R4/valueset-immunization-reason.html)


### Unverträglichkeiten (Allergien)

- Name der Allergie (code) - [AllergiesAndIntolerancesForImmunization](http://fhir.ch/ig/ch-vacd/ValueSet-ch-vacd-immunization-allergyintolerances-vs.html)
- Kritikalität (criticality) - [AllergyIntoleranceCriticality](http://hl7.org/fhir/R4/valueset-allergy-intolerance-criticality.html)
- Status (clinicalStatus) - [AllergyIntoleranceClinicalStatusCodes](http://hl7.org/fhir/R4/valueset-allergyintolerance-clinical.html)
- Art der Allergie (type) - [AllergyIntoleranceType](http://hl7.org/fhir/R4/valueset-allergy-intolerance-type.html)
- Verifiziert (verificationStatus) - [AllergyIntoleranceVerificationStatusCodes](http://hl7.org/fhir/R4/valueset-allergyintolerance-verification.html)
- Kategorie (category, nur Medication) - [AllergyIntoleranceCategory](http://hl7.org/fhir/R4/valueset-allergy-intolerance-category.html)


### Durchgemachte Infektionserkankungen

- Klinischer Status (Condition.clinicalStatus) - [ConditionClinicalStatusCodes](http://hl7.org/fhir/R4/valueset-condition-clinical.html)
- Erkrankung (Condition.code) - [TargetDiseasesAndIllnessesUndergoneForImmunization](http://fhir.ch/ig/ch-vacd/ValueSet-ch-vacd-targetdiseasesandillnessesundergoneforimmunization-vs.html)
- Schweregrad (optional, Condition.severity) - [Condition/DiagnosisSeverity](http://hl7.org/fhir/R4/valueset-condition-severity.html)
- Symptome (optional, Condition.evidence) - [ManifestationAndSymptomCodes](http://hl7.org/fhir/R4/valueset-manifestation-or-symptom.html)
- Verifizierungsstatus (optional, Condition.verificationStatus) - [ConditionVerificationStatusCodes](http://hl7.org/fhir/R4/valueset-condition-ver-status.html)


### Kommentare

Keine
