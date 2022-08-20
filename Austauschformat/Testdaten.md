## Testdaten

### Patient

#### TC patient 1
- Name: Wegmueller
- Vorname: Monika
- Geb.: 1967-02-10
- Adm. Geschlecht (gender): female

### GFP

#### TC_HCP1_C1
- GLN = "7601888888884"
- Name = "Bereit"
- Vorname = "Allzeit"
- Titel = "Dr. med."
- Strasse = "Doktorgasse 2"
- Ort = "Musterhausen"
- PLZ = "8888"
- Land = "CH"

#### TC_HCP2_C2

- GLN = "7601999999998"
- Name = "Gesund"
- Vorname = "Meist"
- Titel = "Dr. med."
- Strasse = "Aerztehaus"
- Ort = "Beispielen"
- PLZ = "7890"
- Region = "ZH"
- Land = "CH"

### Impfungen

#### Testfile [A-D1-P-C1](../Testfiles/Bundle-A-D1-P-C1.json)

Bundle Inhalt:
- date: June 1, 2021
- author: M. Wegmüller
- patient: M. Wegmüller

Performer:
- performer: TC_HCP1_C1
- practitioner: TC_HCP1_C1 Bereit Allzeit
- organization: TC_ORG1 Gruppenpraxis CH

Inhalt:
- 1 Impfung

#### Testfile [B-D1-HCP1-C1](../Testfiles/Bundle-B-D1-HCP1-C1.json)

Bundle Inhalt:
- date: June 8, 2021
- author: TC_HCP1_C1
- patient: M. Wegmüller

Performer (beide Impfungen):
- performer: TC_HCP1_C1
- practitioner: TC_HCP1_C1 Bereit Allzeit
- organization: TC_ORG1 Gruppenpraxis CH

Inhalt:
- 2 Impfungen

#### Testfile [A-D2-HCP1-C1](../Testfiles/Bundle-A-D2-HCP1-C1.json)

Bundle Inhalt:
- date: June 15, 2021
- author: TC_HCP1_C1
- patient: M. Wegmüller

Performer:
- performer: TC_HCP1_C1
- practitioner: TC_HCP1_C1 Bereit Allzeit
- organization: TC_ORG1 Gruppenpraxis CH

Inhalt:
- 1 Impfung

#### Testfile [A-D3-HCP2-C2](../Testfiles/Bundle-A-D3-HCP2-C2.json)

Bundle Inhalt:
- date: August 1, 2021
- author: TC_HCP2_C2
- patient: M. Wegmüller

Performer:
- performer: TC_HCP2_C2
- practitioner: TC_HCP2_C2
- organization: TC_ORG2

Inhalt:
- 1 Impfung

#### Testfile [A-D4-HCP2-C2](../Testfiles/Bundle-A-D4-HCP2-C2.json)

Bundle Inhalt:
- date: August 20, 2021
- author: TC_HCP2_C2
- patient: M. Wegmüller

Performer:
- performer: TC_HCP2_C2
- practitioner: TC_HCP2_C2
- organization: TC_ORG2

Inhalt:
- 1 Impfung als 2te Dosis (zu [A-D1-P-C1](#testfile-a-d1-p-c1))


#### Testfile [A-D5-P-C1](../Testfiles/Bundle-A-D5-P-C1.json)

Bundle Inhalt:
- date: September 1, 2021
- author: M. Wegmüller
- patient: M. Wegmüller

Performer:
- performer: TC_HCP2_C2
- practitioner: TC_HCP2_C2
- organization: TC_ORG2

Inhalt:
- 1 Impfung als 2te Dosis (zu [A-D3-HCP2-C2](#testfile-a-d3-hcp2-c2))


### Unverträglichkeiten (Allergien)

#### Testfile [B-D2-HCP1-C1](../Testfiles/Bundle-B-D2-HCP1-C1.json)

Bundle Inhalt:
- date: October 6, 2021
- author: TC_HCP1_C1
- patient: M. Wegmüller

Recorder:
- recorder: TC_HCP1_C1
- practitioner: TC_HCP1_C1 Bereit Allzeit
- organization: TC_ORG1 Gruppenpraxis CH

Inhalt:
- 1 Unverträglichkeit (Allergy to component of vaccine product containing Salmonella ...
- onset: 2021-10-06
- recordedDate: 2021-10-06
- lastOccurrence: 2021-10-06
- clinicalStatus: Active
- verificationStatus: Confirmed


### Durchgemachte Infektionserkankungen

#### Testfile [B-D3-HCP1-C1](../Testfiles/Bundle-B-D3-HCP1-C1.json)

Bundle Inhalt:
- date: October 6, 2021
- author: TC_HCP1_C1
- patient: M. Wegmüller

Recorder:
- recorder: TC_HCP1_C1
- practitioner: TC_HCP1_C1 Bereit Allzeit
- organization: TC_ORG1 Gruppenpraxis CH

Inhalt: 1 Infektionskrankheit
- code: Varicella
- onset: 2015-05-30
- recordedDate: 2015-05-30

### Kommentare

TODO bisher keine Testfiles
