## TC 3.4: Unverträglichkeit bearbeiten

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Unverträglichkeiten. Dem Benutzer werden alle Einträge mit Unverträglichkeiten zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt, d.h. alle Einträge für Unverträglichkeiten aus allen Dokumenten im EPD Testsystem werden angezeigt.

Der Benutzer wählt eine Unverträglichkeit aus und öffnet das Formular zur Bearbeitung einer ausgewählten Unverträglichkeit per Kontextmenü. Der Benutzer ändert oder ergänzt die Attribute der Unverträglichkeit und bestätigt die Änderungen. Das Impfmodul speichert ein neues Dokument mit allen Attributen nach Änderung und Bezug zum Original im EPD.

In der Liste die Unverträglichkeiten erscheint die Änderungen als neuer Eintrag. Im Impfausweis hingegen wird nur der neue Eintrag mit den Änderungen angezeigt.


### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).

### Durchführung:

Der Test wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Unverträglichkeiten im Menu auswählen.
- Die Ansicht für Unverträglichkeiten öffnen.
- Einen Eintrag in der Liste auswählen und das Formular zur Bearbeitung via Kontextmenü öffnen.
- Ausgewählte Attribute des Eintrags ändern und die Änderung bestätigen.
- Verifizieren, dass die Änderungen in einem neuen Dokument im EPD gespeichert wurde.
- Verifizieren, dass der geänderte Eintrag in der Liste die Unverträglichkeiten erscheint.
- Verifizieren, dass die Änderung den originalen Eintrag im Impfausweis überschreibt.


### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Formular kann per Kontextmenü geöffnet werden.
- Alle Attribute können bearbeitet werden.
- Es wird ein Dialog zur Bestätigung angezeigt.
- Der geänderte Eintrag erscheint in der Liste der Unverträglichkeiten.
- Die Änderung überschreibt den originalen Eintrag im Impfausweis.
- Die Kommentare zur Unverträglichkeit wurden aus dem Original übernommen.
