## Klassifikation der Daten

Das Impfmodul speichert keine Personendaten. Das Impfmodul übernimmt Personendaten aus dem aufrufenden Portal oder Primärsystem und nutzt die Personendaten nur für die Dauer der Session des authentifizierten Benutzers. 

### Personendaten 

Das Impfmodul übernimmt die folgenden Personendaten aus dem aufrufenden Portal oder Primärsystem:
- Die lokale ID des Patienten oder der Patientin im Portal ober Primärsystem 
- Vor-, Nachname und Titel des authentifizierten Benutzers
- Die EPD Rolle des Benutzers

Für Benutzer der EPD Rolle Assistent, zusätzlich: 
- die GLN der verantwortlichen GFP
- der Name der verantworlichen GFP

Das Impfmodul benötigt die o.g. Personendaten für den Betrieb, insbesondere der Abfrage der X-User Assertion 
aus der Gemeinschaft.  

Das Impfmodul nutzt die o.g. Personendaten nur für die Dauer der Session des authentifizierten Benutzers. Nach Ablauf der 
Session des angeldeten Benutzers werde die Personendaten gelöscht. Die o.g. Personendaten werden insbesondere nicht 
persistent gespeichert. 
