## TC 3.3: Nebenwirkung erfassen
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Nebenwirkungen. Dem Benutzer werden alle Einträge mit Nebenwirkungen zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt.

Der Benutzer öffnet das Formular zur Erfassung einer neuen Nebenwirkung mit dem Button.

### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).


### Durchführung:

Der Test wird wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Nebenwirkungen im Menu auswählen.
- Die Ansicht für Nebenwirkungen öffnen.
- Das Formular zur Erfassung neuer Nebenwirkungen öffnen.
- Eine neue Nebenwirkung erfassen.  
- Verifizieren, dass die Unverträglichkeit im EPD gespeichert wurde und in der Tabelle der Nebenwirkung angezeigt wird.


### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul zeigt alle Nebenwirkungen aus dem EPD Testsystem zeilenweise an und die neu erfasste Nebenwirkung erscheint in der Tabelle der Nebenwirkungen.
- Die Detailansicht einer Nebenwirkung öffnet sich per Doppelklick in der Zeile der neue erfassten Nebenwirkung und die Attribute in der Detailansicht sind identisch mit den eingegeben Werten.

Eine neue Unverträglichkeit muss mit den folgenden Attributen erfasst werden können:  
1.	Zeitpunkt (recordedDate)
2.	Name der Nebenwirkung (code)
3.	Kritikalität (criticality, optional)
4.	Status (clinicalStatus, optional)
5.	Gesundheitsfachperson (recorder)
6.	Art der Allergie (type, Allergie oder Intoleranz, optional)
7.	Verifiziert (verificationStatus, optional)
8.	Kategorie (category, nur Medication)
