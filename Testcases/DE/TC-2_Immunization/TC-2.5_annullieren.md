## TC 2.5: Impfung annullieren
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten
und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die
Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen
Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Impfungen. Dem Benutzer werden alle Einträge mit Impfungen
zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht
berücksichtigt, d.h. alle Einträge für Impfungen aus allen Dokumenten im EPD Testsystem werden
angezeigt.

Der Benutzer wählt eine Impfung aus und öffnet die Detailansicht. Der Benutzer öffnet das Formular
zum Annullieren, gibt einen Kommentar zur Begründung ein und bestätigt die Annulation.

In der Liste die Impfungen erscheint die annullierte Impfung mit Kommentar. Im Impfausweis hingegen
wird der annullierte Eintrag nicht mehr angezeigt.

### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Unverträglichkeiten, Infektionskrankheiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.


### Durchführung:

Der Test wie folgt durchgeführt:

- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Impfungen im Menu auswählen.
- Die Ansicht für Impfungen öffnen.
- Einen Eintrag in der Liste auswählen und das Formular zur Bearbeitung via Kontextmenü öffnen.
- Einen Eintrag in der Liste auswählen und das Formular zur Annullation per Kontextmenu öffnen.
- Eine Begründung eingeben und die Annullierung bestätigen.
- Verifizieren, dass der originale Eintrag in der Liste der Impfungen angezeigt wird.
- Verifizieren, dass der annullierte Eintrag nicht mehr im Impfausweis angezeigt wird.   


### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Formular zum Annullieren kann aus der Detailansicht geöffnet werden.
- Ein Begründung muss eingegeben werden.
- Die Änderung annulliert den originalen Eintrag im Impfausweis.
- Der originale Eintrag und die Annulation werden in der Liste der Impfungen angezeigt.
- Der annullierte Eintrag wird nicht mehr im Impfausweis angezeigt.
