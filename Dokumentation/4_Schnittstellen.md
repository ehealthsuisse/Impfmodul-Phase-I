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

### Präsentation API
