## TC 2.1: Impfungen anzeigen
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Impfungen (Immunization Administration). Dem Benutzer werden alle Einträge für Impfungen zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt.


### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Unverträglichkeiten, Infektionskrankheiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.


### Durchführung:

Der Test wird wie folgt durchgeführt:

Iteration 1:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Impfungen im Menu auswählen.
- Die Ansicht für Impfungen öffnen.
- Verifizieren, dass die Impfungen aus dem EPD Testsystem (Gazelle, EPD Playground, Testsystem einer Gemeinschaft) in einer Tabelle angezeigt werden.


### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul zeigt eine Tabelle mit allen Impfungen (1 Zeile pro Impfung) aus dem EPD Testsystem an.
- Die Impfungen werden zeitgeordnet vom neuesten zum ältesten Zeitpunkt der Impfung angezeigt.  
- Die Impfungen werden in der Tabelle mit den folgenden Attributen angezeigt:
1. Zeitpunkt der Impfung (occurenceDateTime)
2. Impfstoff (vaccineCode)
3. Impfschutz (targetDesease)
4. Dosisnummer (doseNumberPositiveInt)
5. Name der Gesundheitsfachperson, welche die Impfung verabreicht hat (performer).
