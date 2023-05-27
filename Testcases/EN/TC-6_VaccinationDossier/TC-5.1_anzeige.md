
## TC 5.1: Impfausweis anzeigen

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für den Impfausweis. Dem Benutzer werden alle Einträge für Impfungen, Nebenwirkungen und Kommentaren zu Impfungen und Nebenwirkungen zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management berücksichtigt.


### Preparation:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Nebenwirkungen und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.
- Die Testdokumente im EPD Testsystem enthalten die erforderlichen Einträge für Impfungen, Nebenwirkungen und Kommentare zu Impfungen und Unverträglichkeiten.


### Test run:

The test shall be run as follows:

- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für den Impfausweis im Menu auswählen.
- Die Ansicht für Impfausweis öffnen.
- Verifizieren, dass die Impfungen, Nebenwirkungen und Infektionskrankheiten aus dem EPD Testsystem (Gazelle, EPD Playground, Testsystem einer Gemeinschaft) unter Berücksichtigung des Lifecycle Management in einer Sicht angezeigt werden.


### Expected result:

The expected result is:
- Das Impfmodul zeigt die Impfungen, Nebenwirkungen und Infektionskrnakheiten aus dem EPD Testsystem an.
- Die Impfungen werden zeitgeordnet vom neuesten zum ältesten Eintrag angezeigt. Die zeitliche Ordnung nutzt das Attribut "occurence" des Impfeintrags.
- Die Nebenwirkungen werden zeitgeordnet vom neuesten zum ältesten Eintrag angezeigt. Die zeitliche Ordnung nutzt das Attribut "recorderDate".
- Die Infektionskrankheiten werden jeweils zeitgeordnet vom neuesten zum ältesten Eintrag angezeigt. Die zeitliche Ordnung nutzt das Attribut "Zeitpunkt der Erkrankung (PastIllness.onset[].onsetDateTime)".
- Die Reihenfolge der Impfungen, Unverträglichkeiten und Infektionskrankheiten kann invertiert werden (ältester zum neuesten Eintrag).

Impfungen werden mit den folgenden Attributen angezeigt (vgl. TC 2.1: Impfungen anzeigen):
1.	Zeitpunkt der Impfung (occurenceDateTime)
2.	Impfstoff (vaccineCode)
3.	Impfschutz (targetDesease)
4.	Dosisnummer (doseNumberPositiveInt)
5.	Name der Gesundheitsfachperson, welche die Impfung verabreicht hat (performer).

Nebenwirkungen werden mit den folgenden Attributen angezeigt (vgl. TC 3.1: Nebenwirkungen anzeigen):
1.	Zeitpunkt (recordedDate)
2.	Name der Nebenwirkung (code)
3.	Kritikalität (criticality)
4.	Status (clinicalStatus)
5.	Gesundheitsfachperson (recorder)

Infektionskrankheiten werden mit den folgenden Attributen angezeigt (vgl. TC 4.1: Infektionskrankheiten anzeigen):
1. Zeitpunkt der Erkrankung (PastIllness.onset[].onsetDateTime)
2. Krankheit (PastIllness.code)
3. Klinischer Status (PastIllness.clinicalStatus)
4. Verifizierungsstatus (PastIllness.verificationStatus)
5. Name der Gesundheitsfachperson, welche die Infektionskrankheit erfasst hat (PastIllness.recorder).

