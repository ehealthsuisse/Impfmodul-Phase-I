## TC 6.1: Kommentare anzeigen

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für Impfungen, Unverträglichkeiten oder für den Impfausweis. Dem Benutzer werden die Impfungen oder Unverträglichkeiten und in der Liste angezeigt.

Der Benutzer wählt einen Eintrag aus und öffnet die Detailansicht. Der Benutzer öffnet die Liste der Kommentare aus der Detailansicht.

### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Unverträglichkeiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.
- Die Testdokumente im EPD Testsystem enthalten Einträge mit Impfungen, Unverträglichkeiten und Kommentaren.

### Durchführung:

Der Test wird wie folgt durchgeführt:

Iteration 1:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Impfungen im Menu auswählen.
- Im View eine kommentierte Impfung auswählen und die Ansicht für die Kommentare aus dem Kontextmenü aufrufen.
- Verifizieren, dass alle Kommentare zur Impfung angezeigt werden.

Iteration 2:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Unverträglichkeiten im Menu auswählen.
- Im View eine kommentierte Unverträglichkeit auswählen und die Ansicht für die Kommentare aus dem Kontextmenü aufrufen.
- Verifizieren, dass alle Kommentare zur Unverträglichkeiten angezeigt werden.

Iteration 3:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für den Impfausweis im Menu auswählen.
- Im View eine kommentierte Unverträglichkeit auswählen und die Ansicht für die Kommentare aus dem Kontextmenü aufrufen.
- Verifizieren, dass alle Kommentare zur Unverträglichkeiten angezeigt werden.

### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:

- Die Ansicht für die Kommentare kann aus der Detailansicht des ausgewählten Eintrags geöffnet werden.
- Alle zum Eintrag erfassten Kommentare werden zeitgeordnet angezeigt (vom neuesten zum ältesten Kommentar).

Die Spalten der Liste zeigen die folgenden Daten:  
1.	Zeitpunkt des Kommentars (note/time)
2.	ersten N Zeichen des Kommentars (note/text)
3.	Name der Gesundheitsfachperson, welche den Kommentar erfasst hat
