## TC 3.4: Infektionskrankheit bearbeiten

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Infektionskrankheiten. Dem Benutzer werden alle Einträge mit Infektionskrankheiten zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt, d.h. alle Einträge für Infektionskrankheiten aus allen Dokumenten im EPD Testsystem werden angezeigt.

Der Benutzer wählt eine Infektionskrankheit aus und öffnet die Detailansicht. Der Benutzer öffnet das Formular zur Bearbeitung der ausgewählten Infektionskrankheit.

Der Benutzer ändert oder ergänzt die Attribute der Infektionskrankheit und bestätigt die Änderungen. Das Impfmodul speichert ein neues Dokument mit allen Attributen nach Änderung und Bezug zum Original im EPD.

In der Liste die Infektionskrankheiten erscheint die Änderung als neuer Eintrag. Im Impfausweis hingegen wird nur der neue Eintrag mit den Änderungen angezeigt.


### Preparation:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Infektionskrankheiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.

### Test run:

Der Test wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Infektionskrankheiten im Menu auswählen.
- Die Ansicht für Infektionskrankheiten öffnen.
- Einen Eintrag in der Liste auswählen und die Detailansicht öffnen.
- Das Formular zum Bearbeiten öffnen.  
- Ausgewählte Attribute des Eintrags ändern und die Änderung bestätigen.
- Verifizieren, dass die Änderungen in einem neuen Dokument im EPD gespeichert wurde.
- Verifizieren, dass der geänderte Eintrag in der Liste die Infektionskrankheiten erscheint.
- Verifizieren, dass die Änderung den originalen Eintrag im Impfausweis überschreibt.


### Expected result:

The expected result is:
- Das Formular kann per Kontextmenü geöffnet werden.
- Alle Attribute können bearbeitet werden.
- Es wird ein Dialog zur Bestätigung angezeigt.
- Der geänderte Eintrag erscheint in der Liste der Infektionskrankheiten.
- Die Änderung überschreibt den originalen Eintrag im Impfausweis.
- Die Kommentare zur Infektionskrankheit wurden aus dem Original übernommen.
