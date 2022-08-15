## TC 3.3: Infektionskrankheit erfassen
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen. Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Infektionskrankheiten. Dem Benutzer werden alle Einträge mit Infektionskrankheiten zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt.

Der Benutzer öffnet das Formular zur Erfassung einer neuen Infektionskrankheit mit dem Button.

### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).


### Durchführung:

Der Test wird wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Infektionskrankheiten im Menu auswählen.
- Die Ansicht für Infektionskrankheiten öffnen.
- Das Formular zur Erfassung neuer Infektionskrankheiten öffnen.
- Eine neue Infektionskrankheit erfassen.  
- Verifizieren, dass die Infektionskrankheit im EPD gespeichert wurde und in der Tabelle der Infektionskrankheiten angezeigt wird.


### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul zeigt alle Infektionskrankheiten aus dem EPD Testsystem zeilenweise an und die neu erfasste Infektionskrankheit erscheint in der Tabelle der Infektionskrankheiten.
- Die Detailansicht einer Infektionskrankheit öffnet sich per Doppelklick in der Zeile der neue erfassten Infektionskrankheit und die Attribute in der Detailansicht sind identisch mit den eingegeben Werten.

Eine neue Infektionskrankheit kannn mit den folgenden Attributen erfasst werden:  
- Zeitpunkt der Erkrankung (PastIllness.onset[].onsetDateTime)
- Krankheit (PastIllness.code)
- Klinischer Status (PastIllness.clinicalStatus)
- Verifizierungsstatus (PastIllness.verificationStatus)
- Schweregrad (PastIllness.severity, optional)
- Endzeitpunkt (PastIllness.abatement[].abatementDateTime, optional)
- Name der Gesundheitsfachperson, welche die Infektionskrankheit erfasst hat (PastIllness.recorder).
- 1..N Kontaktdaten der GFP (optional)  
