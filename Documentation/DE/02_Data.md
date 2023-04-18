## Klassifikation der Daten

### Stammdaten

Das Impfmodul nutzt die folgenden Stammdaten, welche mit dem Impfmodul deployed werden
müssen und im Deployment Paket enthalten sind:
- Konfigurationsdaten, insbesondere die Datem für die Kommunikation mit der Gemeinschaft
- Metadaten mit den Value Sets für die codierten Attribute des Austauschsformats
- I18n Dateien für die Übersetzung der Texte in den UI des Impfmoduls

Für die Installation müssen die Konfigurationsdaten für die Gemeinschaft vom Betreiber
passend eingestellt werden.   

### Bewegungsdaten

Im laufenden Betrieb lädt das Impfmodul die Impfdaten aus dem EPD der Patienten und
Patientinnen und behält die Impfdaten nur für die Dauer der Session im Arbeitsspeicher.
Nach Ablauf der Session des angemeldeten Benutzers werden die Impfdaten sämtlich
gelöscht. Die Impfdaten werden insbesondere nicht persistent ausserhalb des EPD
gespeichert.

### Personendaten

Das Impfmodul speichert keine Personendaten. Das Impfmodul übernimmt Personendaten
aus dem aufrufenden Portal oder Primärsystem und nutzt die Personendaten nur für die
Dauer der Session des authentifizierten Benutzers.

Das Impfmodul übernimmt die folgenden Personendaten aus dem aufrufenden Portal oder Primärsystem:
- Die lokale ID des Patienten oder der Patientin im Portal oder Primärsystem
- Vor-, Nachname und Titel des authentifizierten Benutzers
- Die EPD Rolle des Benutzers

Für Benutzer der EPD Rolle Assistent, zusätzlich:
- die GLN der verantwortlichen GFP
- der Name der verantworlichen GFP

Das Impfmodul benötigt die o.g. Personendaten für den Betrieb, insbesondere der
Abfrage der X-User Assertion aus der Gemeinschaft.  

Das Impfmodul nutzt die o.g. Personendaten nur für die Dauer der Session des
authentifizierten Benutzers. Nach Ablauf der Session des angemeldeten Benutzers werden
die Personendaten gelöscht. Die Personendaten werden insbesondere nicht
persistent gespeichert.
