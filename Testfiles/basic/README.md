## Testdaten

### Patient

#### TC patient 1
- Name: Wegmueller
- Vorname: Monika
- Geb.: 1967-02-10
- Adm. Geschlecht (gender): female

### Gesundheitsfachpersonen (GFP)

#### TC_HCP1_C1
- GLN = "7601888888884"
- Name = "Müller"
- Vorname = "Peter"
- Titel = "Dr. med."
- Strasse = "Grabenstrasse 2"
- Ort = "Zürich"
- PLZ = "8005"
- Land = "CH"

#### TC_HCP2_C2

- GLN = "7601999999998"
- Name = "Meier"
- Vorname = "Gabriela"
- Titel = "Dr. med."
- Strasse = "Werthgasse 34"
- Ort = "Bern"
- PLZ = "3000"
- Region = "BE"
- Land = "CH"

### Impfungen

#### Testfile [A-D1-P-C1](./Bundle-A-D1-P-C1.json)

Bundle Inhalt:
- date: June 1, 2021
- author: M. Wegmüller
- patient: M. Wegmüller

Performer:
- performer: TC_HCP1_C1
- organization: TC_ORG1

Inhalt:
- 1 Impfung

#### Testfile [B-D1-HCP1-C1](./Bundle-B-D1-HCP1-C1.json)

Bundle Inhalt:
- date: June 8, 2021
- author: TC_HCP1_C1 Dr. Peter Müller
- patient: M. Wegmüller

Performer (beide Impfungen):
- performer: TC_HCP1_C1 Dr. Peter Müller
- organization: TC_ORG1

Inhalt:
- 2 Impfungen

#### Testfile [A-D2-HCP1-C1](./Bundle-A-D2-HCP1-C1.json)

Bundle Inhalt:
- date: June 15, 2021
- author: TC_HCP1_C1 Dr. Peter Müller
- patient: M. Wegmüller

Performer:
- performer: TC_HCP1_C1 Dr. Peter Müller
- organization: TC_ORG1

Inhalt:
- 1 Impfung

#### Testfile [A-D3-HCP2-C2](./Bundle-A-D3-HCP2-C2.json)

Bundle Inhalt:
- date: August 1, 2021
- author: TC_HCP2_C2 Dr. Gabriela Meier
- patient: M. Wegmüller

Performer:
- performer: TC_HCP2_C2 Dr. Gabriela Meier
- organization: TC_ORG2

Inhalt:
- 1 Impfung

#### Testfile [A-D4-HCP2-C2](./Bundle-A-D4-HCP2-C2.json)

Bundle Inhalt:
- date: August 20, 2021
- author: TC_HCP2_C2 Dr. Gabriela Meier
- patient: M. Wegmüller

Performer:
- performer: TC_HCP2_C2 Dr. Gabriela Meier
- organization: TC_ORG2

Inhalt:
- 1 Impfung als 2te Dosis (zu [A-D1-P-C1](#testfile-a-d1-p-c1))


#### Testfile [A-D5-P-C1](./Bundle-A-D5-P-C1.json)

Bundle Inhalt:
- date: September 1, 2021
- author: M. Wegmüller
- patient: M. Wegmüller

Performer:
- performer: TC_HCP2_C2 Dr. Gabriela Meier
- organization: TC_ORG2

Inhalt:
- 1 Impfung als 2te Dosis (zu [A-D3-HCP2-C2](#testfile-a-d3-hcp2-c2))


#### Testfile F-D1-HCP1-C1

Bundle Inhalt:
- date: September 1, 2021
- author: Dr. Peter Müller
- patient: M. Wegmüller

Performer:
- performer: TC_HCP1_C1 Dr. Peter Müller
- organization: Gruppenpraxis Müller

Inhalt:
- 1 Impfung Havrix 1440
- Datum: 2021-05-27
- LotNumber: AHAVB946A

### Unverträglichkeiten (Allergien)

#### Testfile [B-D2-HCP1-C1](./Bundle-B-D2-HCP1-C1.json)

Bundle Inhalt:
- date: October 6, 2021
- author: TC_HCP1_C1 Dr. Peter Müller
- patient: M. Wegmüller

Recorder:
- recorder: TC_HCP1_C1 Dr. Peter Müller
- organization: TC_ORG1

Inhalt:
- 1 Unverträglichkeit (Allergy to component of vaccine product containing Salmonella ...
- onset: 2021-10-06
- recordedDate: 2021-10-06
- lastOccurrence: 2021-10-06
- clinicalStatus: Active
- verificationStatus: Confirmed

#### Testfile [F_D3_HCP1_C1](./Bundle-F-D3-HCP1-C1.json)

Bundle Inhalt:
- date: September 1, 2021
- author: TC_HCP1_C1 Dr. Peter Müller
- patient: M. Wegmüller

Recorder:
- recorder: TC_HCP1_C1 Dr. Peter Müller
- organization: TC_ORG1

Inhalt:
- 2 Unverträglichkeiten
- onset: 2021-10-06
- recordedDate: 2021-10-06
- lastOccurrence: 2021-10-06
- clinicalStatus: Active
- verificationStatus: Confirmed


### Durchgemachte Infektionserkankungen

#### Testfile [B-D3-HCP1-C1](./Bundle-B-D3-HCP1-C1.json)

Bundle Inhalt:
- date: October 6, 2021
- author: TC_HCP1_C1 Dr. Peter Müller
- patient: M. Wegmüller

Recorder:
- recorder: TC_HCP1_C1 Dr. Peter Müller
- organization: TC_ORG1

Inhalt:
- 1 Infektionskrankheit
- code: Varicella
- onset: 2015-05-30
- recordedDate: 2015-05-30

#### Testfile [F-D5-HCP1-C1](./Bundle-F-D5-HCP1-C1.json)

Bundle Inhalt:
- date: October 6, 2021
- author: TC_HCP1_C1 Dr. Peter Müller
- patient: M. Wegmüller

Recorder:
- recorder: TC_HCP1_C1 Dr. Peter Müller
- organization: TC_ORG1

Inhalt:
- 1 Infektionskrankheit
- code: Varicella
- onset: 2015-05-30
- recordedDate: 2015-05-30


### Kommentare

#### Testfile [F-D2-HCP1-C1](./Bundle-F-D2-HCP1-C1.json)

Kommentar zu einer vorab erfassten Impfung

Bundle Inhalt:
- date: 2021-05-27
- author: TC_HCP1_C1 Dr. Peter Müller
- patient: M. Wegmüller

Performer:
- TC_HCP1_C1 Dr. Peter Müller

Inhalt:
- 1 Impfung mit Kommentar (zur Impfung in F-D1-HCP1-C1)


#### Testfile [F-D4-HCP1-C1](./Bundle-F-D4-HCP1-C1.json)

Kommentar zu einer vorab erfassten Unverträglichkeit (in Testfile F-D3-HCP1-C1)

Bundle Inhalt:
- date: June 15, 2021
- author: TC_HCP1_C1 Dr. Peter Müller
- patient: M. Wegmüller

Recorder:
- TC_HCP1_C1 Dr. Peter Müller

Inhalt:
- 1 Unverträglichkeit mit Kommentar (zur Unverträglichkeit in F-D3-HCP1-C1)
- onset: 2021-10-06
- recordedDate: 2021-10-06


#### Testfile [F-D6-HCP1-C1](./Bundle-F-D6-HCP1-C1.json)

Kommentar zu einer vorab erfassten Vorerkrankung (in Testfile F-D5-HCP1-C1)

Bundle Inhalt:
- date: October 6, 2021
- author: TC_HCP1_C1 Dr. Peter Müller
- patient: M. Wegmüller

Recorder:
- TC_HCP1_C1 Dr. Peter Müller

Inhalt:
- 1 Vorerkrankung mit Kommentar (zur Unverträglichkeit in F-D5-HCP1-C1)
- onset: 2015-05-30
- recordedDate: 2015-05-30
