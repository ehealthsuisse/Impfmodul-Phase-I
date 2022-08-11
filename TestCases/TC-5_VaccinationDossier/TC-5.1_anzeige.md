
## TC 5.1: Impfausweis anzeigen

Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden alle erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann. Das Zertifikat der digitalen Signatur ist dem Impfmodul bekannt und die Signatur ist gültig.

Der Benutzer öffnet die Ansicht für den Impfausweis. Dem Benutzer werden alle Einträge für Impfungen, Unverträglichkeiten und Kommentaren zu Impfungen und Unverträglichkeiten zeitgeordnet angezeigt. Dabei werden die Informationen für das Lifecycle Management berücksichtigt.


### Vorbereitung:

Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.  
- Das EPD Testsystem ist aufgeschaltet und erreichbar (Gazelle, EPD Playground, Testsystem einer Gemeinschaft, EPD Mock).
- Im EPD Testsystem sind für den Patienten oder die Patientin Testdaten (Impfungen, Unverträglichkeiten und Kommentare) mit dem Dokument Typ Immunization Administration gespeichert.
- Die Testdokumente im EPD Testsystem enthalten die erforderlichen Einträge für Impfungen, Unverträglichkeiten und Kommentare zu Impfungen und Unverträglichkeiten.


### Durchführung:

Der Test wird wie folgt durchgeführt:

- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für den Impfausweis im Menu auswählen.
- Die Ansicht für Impfausweis öffnen.
- Verifizieren, dass die Impfungen, Unverträglichkeiten und Kommentare aus dem EPD Testsystem (Gazelle, EPD Playground, Testsystem einer Gemeinschaft) unter Berücksichtigung des Lifecycle Management in einer Tabelle angezeigt werden.


### Erwartetes Ergebnis:

Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul zeigt eine Tabelle mit allen Impfungen, Unverträglichkeiten und Kommentaren aus dem EPD Testsystem an.
- Die Impfungen werden zeitgeordnet vom neuesten zum ältesten Eintrag angezeigt (Je eine Impfung pro Zeile). Die zeitliche Ordnung nutzt das Attribute "occurence" des Impfeintrags.
- Die zeitliche Reihenfolge der Impfungen kann invertiert werden (ältester zum neuesten Eintrag).

- Impfungen können nach den folgenden Kriterien gefiltert werden:  
1.	status
2.	vaccineCode
3.	TargetDisease

- Zu den Impfungen werden die Unverträglichkeiten und Kommentare angezeigt (Zeitgeordnet unter der Impfung, zuerst Unverträglichkeiten dann Kommentare).    

- Impfungen werden mit den folgenden Attributen angezeigt (vgl. TC 2.1: Impfungen anzeigen):
1.	Zeitpunkt der Impfung (occurenceDateTime)
2.	Impfstoff (vaccineCode)
3.	Impfschutz (targetDesease)
4.	Dosisnummer (doseNumberPositiveInt)
5.	Name der Gesundheitsfachperson, welche die Impfung verabreicht hat (performer).

- Unverträglichkeiten werden mit den folgenden Attributen angezeigt (vgl. TC 3.1: Unverträglichkeiten anzeigen):
1.	Zeitpunkt (recordedDate)
2.	Name der Allergie (code)
3.	Kritikalität (criticality)
4.	Status (clinicalStatus)
5.	Gesundheitsfachperson (recorder)

- Kommentare zu Impfungen und Unverträglichkeiten werden mit den folgenden Attributen angezeigt (vgl. TC 6.1: Kommentare anzeigen):
1.	Zeitpunkt des Kommentars (note/time)
2.	ersten N Zeichen des Kommentars (note/text)
3.	Name der Gesundheitsfachperson, welche den Kommentar erfasst hat
