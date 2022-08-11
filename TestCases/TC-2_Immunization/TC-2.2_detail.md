## TC 2.2: Impfungen Detailansicht

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für die Impfungen. Dem Benutzer werden alle Einträge mit Impfungen zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management nicht berücksichtigt, d.h. alle Einträge für Impfungen aus allen Dokumenten im EPD Testsystem werden angezeigt.

Der Benutzer wählt aus der Liste der angezeigten Impfungen genau eine Impfung aus und öffnet die Detailansicht der Unverträglichkeit.


### Vorbereitung:
Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Unverträglichkeiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.
- Die Testdokumente im EPD Testsystem enthalten Einträge mit Impfungen.


### Durchführung:
Der Test wird wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Impfungen im Menu auswählen.
- Die Ansicht für Impfungen öffnen.
- Genau eine Impfung in der Liste per Mausklick selektieren und die Detailansicht der Impfung öffnen.
- Verifizieren, dass alle erforderlichen Attribute der Impfung angezeigt werden.

### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul zeigt alle Impfungen aus dem EPD Testsystem zeilenweise an.
- Die Impfungen werden zeitgeordnet angezeigt.  
- Die Detailansicht einer Impfung öffnet sich per Doppelklick in der Zeile.
- Die Impfung wird mit den folgenden Attributen angezeigt:
1.	Zeitpunkt der Impfung (occurenceDateTime)
2.	Impfstoff (vaccineCode)
3.	Impfschutz (targetDesease)
4.	Dosisnummer (doseNumberPositiveInt)
5.	Name der Gesundheitsfachperson, welche die Impfung verabreicht hat (performer).
6.	1..N Kontaktdaten der Gesundheitsfachperson,  welche die Impfung verabreicht hat (performer.#).
7.	Status (status).
8.	Lotnummer (lotnumber)
9.	Grund für die Impfung (reasonCode)
