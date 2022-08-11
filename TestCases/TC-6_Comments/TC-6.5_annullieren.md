## TC 6.5: Kommentar annullieren
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für Impfungen, Unverträglichkeiten oder den Impfausweis, wählt einen Eintrag aus und öffnet den Dialog zum Annullieren des Kommentars. Der Benutzer bestätigt die Annulation.  Das System schreibt ein neues Dokument zur Annulation des originalen Eintrags mit Bezug zum Original.  

In der Liste der Kommentar erscheint der annullierte Kommentar, sowie die Annullierung. Im Impfausweis wird der annullierte Kommentar nicht mehr angezeigt.

### Vorbereitung:
Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).

### Durchführung:
Der Test wird wie folgt durchgeführt:

Iteration 1:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Impfungen im Menu auswählen.
- Die Ansicht für Impfungen öffnen.
- Einen Eintrag in der Liste auswählen und die Anzeige der Kommentare öffnen.
- Einen Kommentar auswählen und die Annullierung bestätigen.
- Verifizieren, dass der originale Eintrag in der Liste der Kommentare angezeigt wird.
- Verifizieren, dass die Annulation als Kommentar angezeigt wird.
- Verifizieren, dass der annullierte Kommentar nicht mehr im Impfausweis angezeigt wird.

Iteration 2:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Unverträglichkeiten im Menu auswählen.
- Die Ansicht für Unverträglichkeiten öffnen.
- Einen Eintrag in der Liste auswählen und die Anzeige der Kommentare öffnen.
- Einen Kommentar auswählen und die Annullierung bestätigen.
- Verifizieren, dass der originale Eintrag in der Liste der Kommentare angezeigt wird.
- Verifizieren, dass die Annulation als Kommentar angezeigt wird.
- Verifizieren, dass der annullierte Kommentar nicht mehr im Impfausweis angezeigt wird.

### Erwartetes Ergebnis:
Das erwartete Resultat des Tests ist wie folgt:
- Das Formular kann per Kontextmenü zu einem ausgewählten Kommentar geöffnet werden.
- Die Annulation kann bestätigt werden.
- Die Änderung annulliert den originalen Kommentar im Impfausweis.
- Die Annulation und das Original werden in der Ansicht für Kommentare angezeigt.
