## TC 3.5: Infektionskrankheit annullieren

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Infektionskrankheiten. Dem Benutzer werden alle Einträge mit Infektionskrankheiten zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt.

Der Benutzer wählt eine Infektionskrankheit aus und öffnet die Detailansicht. Der Benutzer öffnet das Formular zum Annullieren. Der Benutzer gibt einen Kommentar zur Begründung ein und bestätigt die Annullation.   

In der Liste die Infektionskrankheiten erscheint die annullierte Infektionskrankheit mit der
Begründung. Im Impfausweis hingegen wird der annullierte Eintrag nicht mehr angezeigt.


### Preparation:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).


### Test run:

The test shall be run as follows:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Infektionskrankheiten im Menu auswählen.
- Die Ansicht für Infektionskrankheiten öffnen.
- Einen Eintrag in der Liste auswählen und die Detailansicht öffnen.
- Das Formular zur Annullation öffnen.
- Eine Begründung eingeben und die Annullierung bestätigen.
- Verifizieren, dass der originale Eintrag in der Liste der Infektionskrankheiten angezeigt wird.
- Verifizieren, dass der annullierte Eintrag nicht mehr im Impfausweis angezeigt wird.

### Expected result:

The expected result is:
- Das Formular kann aus der Detailansicht geöffnet werden.
- Eine Begründung muss hinzugefügt werden.
- Die Änderung annulliert den originalen Eintrag im Impfausweis.
- Der originale Eintrag und die Annulation werden in der Liste der Infektionskrankheiten angezeigt.
- Der annullierte Eintrag wird nicht mehr im Impfausweis angezeigt.
