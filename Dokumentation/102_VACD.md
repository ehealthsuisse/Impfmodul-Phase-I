## Anzeige der Daten aus dem Austauschformat

### Impfungen

- Datum - Immunization.occurenceDateTime
- Impfstoff - Immunization.vaccineCode, gespeichert als CodeableConcept
- Impfschutz - Immunization.protocolApplied -> targetDisease, aus einer Liste von Codeable Concepts
- Dosis - Immunization.protocolApplied -> doseNumberPositiveInt
- Status - Immunization.status, gespeichert als CodeableConcept
- Lotnummer - Immunization.lotnumber
- Grund der Impfung - Immunization.reasonCode, gespeichert als CodeableConcept
- Geimpft von - Immunization.performer -> PractitionerRole.practitioner, über mehrere Referenzen realisiert.
- Organisation - Immunization.performer -> PractitionerRole.organization, über mehrere Referenzen realisiert.
- Kommentar - Immunization.note mit time, text und author.name

siehe [CH VACD Immunization](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-immunization.html)

### Allergien

- Datum - AllergyIntolerance.recordedDate
- Allergie - AllergyIntolerance.code
- Kritikalität - AllergyIntolerance.criticality
- Klinischer Status - AllergyIntolerance.clinicalStatus, gespeichert als CodeableConcept
- Verifiziert - AllergyIntolerance.verificationStatus, gespeichert als CodeableConcept
- Art - AllergyIntolerance.type, gespeichert als CodeableConcept
- Erfasst von - AllergyIntolerance.recorder -> PractitionerRole.practitioner, über mehrere Referenzen realisiert.
- Organisation - AllergyIntolerance.recorder -> PractitionerRole.organization, über mehrere Referenzen realisiert.
- Kommentar - AllergyIntolerance.note mit time, text und author.name
- Kategorie - AllergyIntolerance.category (Nicht sichtbar in der UI, immer mit Wert **medical**)

siehe [CH VACD AllergyIntolerance](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-allergyintolerances.html)

### Vorerkrankungen

- Datum - PastIllness.recordedDate
- Vorerkrankung - PastIllness.code, gespeichert als CodeableConcept
- Beginn - PastIllness.onset
- Ende - PastIllness.abatementDateTime
- Klinischer Status - PastIllness.clinicalStatus, gespeichert als CodeableConcept
- Verifiziert - PastIllness.verificationStatus, gespeichert als CodeableConcept
- Erfasst von - PastIllness.recorder -> PractitionerRole.practitioner, über mehrere Referenzen realisiert.
- Organisation - PastIllness.recorder -> PractitionerRole.organization, über mehrere Referenzen realisiert.
- Kommentar - PastIllness.note mit time, text und author.name

siehe [CH VACD Past Illness](http://fhir.ch/ig/ch-vacd/StructureDefinition-ch-vacd-pastillnesses.html)
