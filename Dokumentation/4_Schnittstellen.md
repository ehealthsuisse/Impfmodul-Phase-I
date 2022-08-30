## Schnittstellen

Das Impfmodul implementiert die folgenden Schnittstellen:
1. EPD konforme Schnittstelle zu Identity Providern, gemäss EPDV-EDI Anhang 8.
2. EPD konforme Schnittstellen zur EPD Plattform, gemäss EPDV-EDI Anhang 5.
3. Web Aufruf zum Start des Impfmoduls aus dem Portalen der Gemeinschaft.
4. API zur Kommunikation der Präsentationslogik mit der Fachlogik.   

### Schnittstelle zu Identity Providern

Das Impfmodul implementiert eine EPD konforme Schnittstelle zu Identity Providern
mit dem SAML 2.0 Artifact Binding und dem SOAP Backchannel gemäss EPDV-EDI Anhang 8.

Das Impfmodul unterstützt die Protokolle:
1. Authentisierung von Benutzern
2. *IdP Logout*

### Schnittstellen zur EPD Plattform

Das Impfmodul implementiert die folgenden EPD konformen Schnittstelle zu Plattform:
1. Registry Stored Query [ITI-18] - Abfrage der Dokument Metadaten
2. Get X-User Assertion - Abfrage von Autentisierungstoken
3. Provide X-User Assertion [ITI-40] - Übermittlung von Autentisierungstoken
4. Provide and Register Document Set [ITI-41] - Speicherung von Dokumenten
5. Retrieve Document Set [ITI-43] - Abfrage vom Dokumenten
6. PIX V3 Query [ITI-45] - Abfrage des XAD-PID und EPD-SPID

### Web Aufruf

Der Web Abruf orientiert sich an der Web Aufruf Schnittstelle, welche die Plattform der Schweizer Post bereits implementiert hat. 

http GET Schnittstelle zum Aufruf des Impfmoduls aus den Primärsystemen bzw. den Portalen für Gesundheitsfachpersonen, Patientinnen und Patienten. 

Mit dem Aufruf werden die vom Impfmodul benötigten Daten als http GET Parameter aus der aufrufenden Applikation übergeben:

1. Kennung des vom Benutzer im Portal oder Primärsystem genutzten Identity Provider. 
2. Name und Vorname des Benutzers.
3. EPD Rolle des Benutzers.
4. Lokale ID des Patienten oder der Patientin des Primärsystems bzw. des Portals. 
5. Digitale Signatur (HMAC) des aufrufenden Systems bzw. Portals.
6. Zeitstempel.   

Das Impfmodul nutzt die digitale Signatur für: 
* authentisiert das aufrufende Portal bzw. Primärsystem anhand der digitalen Signatur.
* prüft die Integrität des Aufrufs mit der digitalen Signatur.

Hinweis: Für GFP und Hilfspersonen ist die Angabe des Namens und Vornamens des Benutzers nicht ausreichend um die 
XDS.b Metadaten für neue Dokumente zu setzen und das Impfmodul muss die benötigte Information (GLN, Institution, etc.) aus dem 
Response der Get X-User Assertion auslesen. 

### Präsentation API
TBD
