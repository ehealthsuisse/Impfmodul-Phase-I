## Authorisierung

Das Impfmodul implementiert keine eigene Autorisierung. Das Impfmodul nutzt
aussschliesslich die, von der Gemeinschaft implementierte Autorisierung von
Zugriffen auf des EPD gemäss EPDV-EDI.

### Informationsverarbeitung

Zur Abfrage der Impfdokumente aus der Gemeinschaft benötigt das Impmodul eine
gültige X-User Assertion zur Autorisierung.

Gemäss EPDV-EDI benötigt das Impfmodul dazu eine gültige IdP Assertion der im
Portal oder Primärsystem angemeldeten Benutzers oder der Benutzerin, welche
das Impfmodul aufgerufen haben. Mit dem Starten aus einem Portal oder
Primärsystem über den Web Link, übernimmt das Impfmodul die Daten zur
Authentisierung und nutzt diese zur Abfrage der IdP Assertion.

Das Impfmodul nutzt die IdP Assertion zur Authentisierung der Abfrage der
X-User Assertion aus dem X-Assertion Provider der Gemeinschaft gemäss EPDV-EDI. 
Der X-Assertion Provider der Gemeinschaft löst die User Id (Name Id der IdP Assertion) 
zur GLN von Gesundheitsfachpersonen bzw. EPD-SPID von Patienten und Patientinnen auf.   

Das Impfmodul sendet die X-User Assertion im Security Header der Abfragen der
Impfdokumente Dokumente oder der Dokument Metadaten gemäss EPDV-EDI.

Die Gemeinschaft authorisiert die Zugriffe des Impfmoduls bzw. die Zugriffe
des Benutzers oder der Benutzerin über das Impfmodul auf das EPD der Patienten
und Patientinnen gemäss EPDV-EDI.   
