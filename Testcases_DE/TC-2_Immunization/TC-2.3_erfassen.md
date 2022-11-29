## TC 2.3: Impfung erfassen
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Impfungen. Dem Benutzer werden alle Einträge mit Impfungen zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt.

Der Benutzer öffnet das Formular zur Erfassung einer neuen Impfung.

### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).

### Durchführung:
Der Test wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Impfungen im Menu auswählen.
- Die Ansicht für Impfungen öffnen.
- Das Formular zur Erfassung neuer Impfungen öffnen.
- Eine neue Impfung erfassen
- Verifizieren, dass die Impfung im EPD gespeichert wurde und in der Tabelle der Impfungen angezeigt wird.

### Erwartetes Ergebnis:
Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul zeigt alle Impfungen aus dem EPD Testsystem zeilenweise an und die neu erfasste Impfung erscheint in der Tabelle der Impfungen.
- Die Detailansicht der Impfung öffnet sich per Doppelklick in der Zeile der neue erfassten Impfung und die Attribute in der Detailansicht sind identisch mit den eingegeben Werten.

Eine neue Impfung muss mit den folgenden Attributen erfasst werden können:  
1.  Zeitpunkt der Impfung (occurenceDateTime)
2.  Impfstoff (vaccineCode)
3.	Impfschutz (targetDesease)
4.	Dosisnummer (doseNumberPositiveInt)
5.	Name der Gesundheitsfachperson, welche die Impfung verabreicht hat (performer).
6.	1..N Kontaktdaten der Gesundheitsfachperson,  welche die Impfung verabreicht hat (performer.#).
7.	Status (status).
8.	Lotnummer (lotnumber, optional).
9.	Grund für die Impfung (reasonCode, optional).
