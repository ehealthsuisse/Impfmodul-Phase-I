## Value Sets

The vaccination module uses the following value sets for coded values:

### Vaccination

- vaccineCode - [Swissmedic code for vaccine code](http://fhir.ch/ig/ch-vacd/ValueSet-ch-vacd-vaccines-vs.html)
- targetDisease - [TargetDiseasesAndIllnessesUndergoneForImmunization](http://fhir.ch/ig/ch-vacd/ValueSet-ch-vacd-targetdiseasesandillnessesundergoneforimmunization-vs.html)
- status - [ImmunizationStatusCodes](http://hl7.org/fhir/R4/valueset-immunization-status.html)
- verificationStatus - [ImmunizationVerificationStatusCodes](https://fhir.ch/ig/ch-vacd/ValueSet-ch-vacd-verification-status-vs.html)
- reasonCode - [ImmunizationReasonCodes](http://hl7.org/fhir/R4/valueset-immunization-reason.html)


### Adverse effects

- code - [NebenwirkungsAndIntolerancesForImmunization](http://fhir.ch/ig/ch-vacd/ValueSet-ch-vacd-immunization-allergyintolerances-vs.html)
- criticality - [AllergyIntoleranceCriticality](http://hl7.org/fhir/R4/valueset-allergy-intolerance-criticality.html)
- clinicalStatus - [AllergyIntoleranceClinicalStatusCodes](http://hl7.org/fhir/R4/valueset-allergyintolerance-clinical.html)
- type - [AllergyIntoleranceType](http://hl7.org/fhir/R4/valueset-allergy-intolerance-type.html)
- verificationStatus - [AllergyIntoleranceVerificationStatusCodes](http://hl7.org/fhir/R4/valueset-allergyintolerance-verification.html)
- category - [AllergyIntoleranceCategory](http://hl7.org/fhir/R4/valueset-allergy-intolerance-category.html)


### Infectious desease

- Condition.clinicalStatus - [ConditionClinicalStatusCodes](http://hl7.org/fhir/R4/valueset-condition-clinical.html)
- Condition.code - [TargetDiseasesAndIllnessesUndergoneForImmunization](http://fhir.ch/ig/ch-vacd/ValueSet-ch-vacd-targetdiseasesandillnessesundergoneforimmunization-vs.html)
- optional, Condition.severity - [Condition/DiagnosisSeverity](http://hl7.org/fhir/R4/valueset-condition-severity.html)
- optional, Condition.evidence - [ManifestationAndSymptomCodes](http://hl7.org/fhir/R4/valueset-manifestation-or-symptom.html)
- optional, Condition.verificationStatus - [ConditionVerificationStatusCodes](http://hl7.org/fhir/R4/valueset-condition-ver-status.html)

### Medical Problems
TODO


### Comments

Comments don't use coded data.
