## TC 1.1: Impfmodul starten

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

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
- Verifizieren, dass die Impfungen, Nebenwirkungen und Kommentare aus dem Testsystem (Gazelle, EPD Playground, Testsystem einer Gemeinschaft) im Impfmodul angezeigt werden.

Iteration 2:
- Das Impfmodul aus einem EPD Portal für Patienten und Patientinnen mit Patientenkontext und der Rolle PATIENT und REPRESENTATIVE starten.
- Verifizieren, dass die Impfungen, Nebenwirkungen und Kommentare aus dem Testsystem (Gazelle, EPD Playground, Testsystem einer Gemeinschaft) im Impfmodul angezeigt werden.

Iteration 3:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und Rolle ADMIN starten.
- Verifizieren, dass die Impfungen, Unverträglichkeiten und Kommentare aus dem Testsystem (Gazelle, EPD Playground, Testsystem einer Gemeinschaft) im Impfmodul angezeigt werden.


### Erwartetes Ergebnis:
Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul startet im gleichen Browser-Fenster oder in einem neuen Browser-Tab.
- Das Impfmodul durchläuft die Sequenz zur Authentisierung des Benutzers.  
- Das Impfmodul führt die Transaktionen PIX, Get X-User Assertion, Registry Stored Query und Retrieve Document Set gegen das EPD Testsystem aus
- Das Impfmodul zeigt die Impfdaten aus dem EPD Testsystem an.
