## Schnittstellen

Das Impfmodul implementiert die folgenden Schnittstellen:
* EPD konforme Schnittstelle zu Identity Providern, gemäss EPDV-EDI Anhang 8.
* EPD konforme Schnittstellen zur EPD Plattform, gemäss EPDV-EDI Anhang 5.
* Web Aufruf zum Start des Impfmoduls aus dem Portalen der Gemeinschaft.
* API zur Kommunikation der Präsentationslogik mit der Fachlogik.   

### Schnittstelle zu Identity Providern

Das Impfmodul implementiert eine EPD konforme Schnittstelle zu Identity Providern
mit dem SAML 2.0 Artifact Binding und dem SOAP Backchannel gemäss EPDV-EDI Anhang 8.

Das Impfmodul unterstützt die Protokolle:
* Authentisierung von Benutzern
* *IdP Logout*

### Schnittstellen zur EPD Plattform

Das Impfmodul implementiert die folgenden EPD konformen Schnittstelle zur EPD Plattform:
* Patient Demographics Query HL7 V3 [ITI-47] - Abfrage des XAD-PID und EPD-SPID.
* Get X-User Assertion - Abfrage der XUA Assertion zur Authorisierung.
* Registry Stored Query [ITI-18] mit Provide X-User Assertion [ITI-40] - Abfrage der Dokument Metadaten mit Übermittlung der XUA Assertion.
* Provide and Register Document Set [ITI-41] mit Provide X-User Assertion [ITI-40] - Speicherung von Dokumenten mit Übermittlung der XUA Assertion.
* Retrieve Document Set [ITI-43] mit Provide X-User Assertion [ITI-40] - Abfrage vom Dokumenten mit Übermittlung der XUA Assertion.


### Web Aufruf

http GET Schnittstelle zum Aufruf des Impfmoduls aus den Primärsystemen bzw. den Portalen für Gesundheitsfachpersonen, Patientinnen und Patienten.

Mit dem Aufruf werden die vom Impfmodul benötigten Daten als http GET Parameter aus der aufrufenden Applikation übergeben:
* **lpid** - lokale ID des Patienten oder der Patientin im Portal ober Primärsystem als String
* **laaoid** - OID der Local Assigning Autority der locale ID des Portals ober Primärsystem als String
* **ufname** - Nachname des Benutzers als String
* **ugname** - Vorname des Benutzers als String
* **utitle** - Titel des Benutzers als String
* **lang** - Sprache des Benutzers im Format {lang}_{locale} (z.B. EN_us)
* **role** - EPD Rolle des Benutzers aus Value Set { HCP, PAT, ASS, REP }
* **purpose** - der PurposeOfUse aus Value Set { NORM, EMER}
* **idp** - Kennung des IdP mit dem sich der Benutzer authentisiert hat als String
* **principalId** - für die Rolle ASS, die GLN der verantwortlichen GFP im GLN Format
* **principalName** - für die Rolle ASS, der Name der verantwortlichen GFP als String
* **timestamp** - Millisekunden seit 01.01.1970 (UTC) als Long
* **sig** - HMAC SHA-256 Signatur des Query Strings, d.h. substring der URL („?", &sig)


Das Impfmodul nutzt die digitale Signatur für:
* Authentisierung das aufrufenden Portals bzw. Primärsystems anhand der digitalen Signatur.
* Integritätsprüfung des Aufrufs anhand der digitalen Signatur.


### Präsentation API

Das Impfmodul ist als Rich Internet Applikation mit Javascript Client Komponenten (Angular) implement.

Zur Kommunikation der Javascript Client Komponenten implementiert das Impfmodul die folgenden Restful API:
* CRUD API zur Anzeige und Bearbeitung von Impfungen.
* CRUD API zur Anzeige und Bearbeitung von Unverträglichkeiten (Nebenwirkungen).
* CRUD API zur Anzeige und Bearbeitung von Infektionskrankheiten.    
