## TC 3.5: Nebenwirkung annullieren

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Nebenwirkungen. Dem Benutzer werden alle Einträge mit Nebenwirkungen zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt.

Der Benutzer wählt eine Nebenwirkung aus und öffnet die Detailansicht. Der Benutzer öffnet das Formular zum Annullieren. Der Benutzer gibt einen Kommentar zur Begründung ein und bestätigt die Annullation.   

In der Liste die Unverträglichkeiten erscheint die annullierte Nebenwirkung mit Kommentar. Im Impfausweis hingegen wird der annullierte Eintrag nicht mehr angezeigt.


### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Nebenwirkungen, Infektionskrankheiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.


### Durchführung:

Der Test wird wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Nebenwirkungen im Menu auswählen.
- Die Ansicht für Nebenwirkungen öffnen.
- Einen Eintrag in der Liste auswählen und die Detailansicht öffnen.
- Das Formular zur Annullation öffnen.
- Eine Begründung eingeben und die Annullierung bestätigen.
- Verifizieren, dass der originale Eintrag in der Liste der Impfungen angezeigt wird.
- Verifizieren, dass der annullierte Eintrag nicht mehr im Impfausweis angezeigt wird.

### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Formular kann aus der Detailansicht geöffnet werden.
- Eine Begründung muss hinzugefügt werden.
- Die Änderung annulliert den originalen Eintrag im Impfausweis.
- Der originale Eintrag und die Annulation werden in der Liste der Nebenwirkungen angezeigt.
- Der annullierte Eintrag wird nicht mehr im Impfausweis angezeigt.
