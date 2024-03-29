## TC 6.2: Kommentare Detailansicht
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten
und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die
Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen
Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für Impfungen, Unverträglichkeiten, Infektionskrankheiten  
oder für den Impfausweis. Dem Benutzer werden die Impfungen, Unverträglichkeiten und
Infektionskrankheitem in einer Liste angezeigt.

Der Benutzer wählt einen Eintrag aus und öffnet die Detailansicht. Der Benutzer öffnet die Liste der
Kommentare und wählt einen Kommentar aus. Der Kommentar wird dem Benutzer mit allen Attributen
angezeigt.

### Preparation:
Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Unverträglichkeiten, Infektionskrankheiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.

### Test run:
The test shall be run as follows:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Impfungen, Unverträglichkeiten oder den Impfausweis im Menu auswählen.
- Im View einen Eintrag auswählen und die Detailansicht öffnen.  
- Ansicht für die Kommentare öffnen.
- Einen Kommentar auswählen und die Detailansicht zum Kommentar via Mausklick auswählen.
- Verifizieren, dass der Kommentar mit den erforderlichen Attributen angezeigt wird.

### Expected result:
The expected result is:

Der Kommentar wird mit den folgenden Attributen angezeigt:  
1.	Zeitpunkt des Kommentars (note/time)
2.	Kommentar (note/text)
3.	Name der Gesundheitsfachperson, welche den Kommentar erfasst hat
4.	1 .. N Kontaktdaten der Gesundheitsfachperson
