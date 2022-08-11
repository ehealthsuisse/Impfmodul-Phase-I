## TC 3.2: Unverträglichkeiten Detailansicht

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Unverträglichkeiten. Dem Benutzer werden alle Einträge mit Unverträglichkeiten zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt.

Der Benutzer wählt aus der Liste der angezeigten Unverträglichkeiten genau eine Unverträglichkeit aus und öffnet die Detailansicht der Unverträglichkeit.

### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Unverträglichkeiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.
- Die Testdokumente im EPD Testsystem enthalten Einträge mit Unverträglichkeiten.

### Durchführung:

Der Test wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Unverträglichkeiten im Menu auswählen.
- Die Ansicht für Unverträglichkeiten öffnen.
- Genau eine Unverträglichkeit in der Liste per Mausklick selektieren und die Detailansicht der Unverträglichkeit öffnen.
- Verifizieren, dass alle erforderlichen Attribute der Unverträglichkeit angezeigt werden.

### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul zeigt alle Unverträglichkeiten aus dem EPD Testsystem zeilenweise an.
- Die Unverträglichkeiten werden zeitgeordnet vom neuesten zum ältesten Eintrag angezeigt.  
- Die Detailansicht einer Unverträglichkeit öffnet sich per Doppelklick in der Zeile.

Unverträglichkeiten müssen mit den folgenden Attributen angezeigt werden:
1.	Zeitpunkt (recordedDate)
2.	Name der Allergie (code)
3.	Kritikalität (criticality)
4.	Status (clinicalStatus)
5.	Gesundheitsfachperson (recorder)
6.	Art der Allergie (type)
7.	Verifiziert (verificationStatus)
8.	Kategorie (category, nur Medication)
