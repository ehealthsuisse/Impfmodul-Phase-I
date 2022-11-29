## TC 3.1: Unverträglichkeiten anzeigen
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Unverträglichkeiten. Dem Benutzer werden alle Einträge mit Unverträglichkeiten zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt.

### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Unverträglichkeiten, Infektionskrankheiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.
- Die Testdokumente im EPD Testsystem enthalten Einträge mit Unverträglichkeiten.

### Durchführung:

Der Test wird wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Unverträglichkeiten im Menu auswählen.
- Die Ansicht für Unverträglichkeiten öffnen.
- Verifizieren, dass alle Unverträglichkeiten aus dem EPD Testsystem (Gazelle, EPD Playground, Testsystem einer Gemeinschaft) in einer Tabelle angezeigt werden.

### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul zeigt alle Unverträglichkeiten aus dem EPD Testsystem zeilenweise an.
- Unverträglichkeiten werden zeitgeordnet vom neuesten zum ältesten Eintrag angezeigt.  

Unverträglichkeiten werden mit den folgenden Attributen angezeigt:
1.	Zeitpunkt (recordedDate)
2.	Name der Allergie (code)
3.	Kritikalität (criticality)
4.	Status (clinicalStatus)
5.	Gesundheitsfachperson (recorder)
