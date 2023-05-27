## TC 6.3: Kommentar erfassen
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für Impfungen, Unverträglichkeiten oder für den Impfausweis. Dem Benutzer werden die Impfungen oder Unverträglichkeiten und ggfs. erfasste Kommentare in der Liste angezeigt.

Der Benutzer wählt eine Impfung, Unverträglichkeit oder durchmachte Infektionskrankheit aus und öffnet die Detailansicht des ausgewählten Eintrags. Der Benutzer öffnet das Formular zur Erfassung und gibt einen Kommentar ein.

### Preparation:
Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Unverträglichkeiten, Infektionskrankheiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.

### Test run:

The test shall be run as follows:

Iteration 1:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Impfungen, Unverträglichkeiten oder den Impfausweis im Menu auswählen.
- Eine Impfung oder Unverträglichkeit auswählen und das Formular zur Erfassung eines Kommentars via Kontextmenü öffnen.
- Verifizieren, dass der Kommentar mit den erforderlichen Attributen erfasst werden kann.
- Verifizieren, dass der neue Kommentar in der Ansicht der Impfungen, Unverträglichkeiten und dem Impfausweis angezeigt wird.

Iteration 2:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Impfungen, Unverträglichkeiten oder den Impfausweis im Menu auswählen.
- Im View eine kommentierte Impfung oder Unverträglichkeit auswählen und die Ansicht für die Kommentare aus dem Kontextmenü aufrufen.
- Das Formular zur Erfassung von Kommentaren per Button öffnen.
- Verifizieren, dass der Kommentar mit den erforderlichen Attributen erfasst werden kann.
- Verifizieren, dass der neue Kommentar in der Ansicht der Impfungen, Unverträglichkeiten und dem Impfausweis angezeigt wird.

### Expected result:
The expected result is:

Der Kommentar kann mit den folgenden Attributen erfasst werden:  
1.	Zeitpunkt des Kommentars (note/time)
2.	Kommentar (note/text)
3.	Name der Gesundheitsfachperson, welche den Kommentar erfasst hat
4.	1 .. N Kontaktdaten der Gesundheitsfachperson
