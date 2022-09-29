## Authentifizierung

Zur Abfrage der Impfdokumente aus der Gemeinschaft benötigt das Impmodul eine
gültige X-User Assertion zur Autorisierung. Gemäss EPDV-EDI benötigt das Impfmodul
dazu eine gültige IdP Assertion der im Portal oder Primärsystem angemeldeten
Benutzers oder der Benutzerin, welche das Impfmodul aufgerufen haben.  

Das Impfmodul implementiert dazu die Schnittstellen zur Authentisierung der Benutzer und Benutzerinnen gemäss EPDV-EDI, Anhang 8, Ausgabe 2.1:

* SAML 2 http Artifact Binding und Artifact Resolution Protokoll via SOAP Backchannel,  
* IdP Renew via SOAP Backchannel,
* SAML 2 Logout Protokoll mit dem SOAP Backchannel Binding.


### Informationsverarbeitung

#### Authentisierung

Das Impfmodul implementiert die Anforderungen zur Authentisierung von Benutzern
aus EPDV-EDI, Anhang 8, Ausgabe 2.1.

Mit dem Starten aus einem Portal oder Primärsystem über den Web Link, übernimmt das
Impfmodul die Daten zur Authentisierung und nutzt diese zur Abfrage der IdP Assertion.   

Dazu sendet das Impfmodul einen SAML Authentication Request and den Identity Provider
an dem sich der Benutzer oder die Benutzerin aus dem Portal oder Primärsystem
authentisiert hat.

Verfügt der Benutzer oder die Benutzerin über eine gültige Session am IdP, erhält
das Impfmodul das SAML Artifact mit dem Response des IdP. Falls nicht, wird der
Benutzer oder die Benutzerin an die Login Seite des IdP verwiesen.

Das Impfmodul löst das SAML Artifact über den SAML Artifact Resolve Request über
den SOAP Backchannel am IdP auf und erhält einen gültige IdP Assertion mit den
Authentifizierungsdaten des Benutzers oder der Benutzerin.


#### IdP Renew

Das Impfmodul implementiert die Anforderungen zum IdP Renew aus EPDV-EDI,
Anhang 8, Ausgabe 2.1.

Im laufenden Betrieb erneuert das Impfmodul die IdP Assertion, welche für die
Abfrage der X-User Assertion erforderlich ist, über die IdP Renew Transaktion
via SOAP Backchannel.   

#### IdP Logout

Das Impfmodul implementiert einen Endpunkt zur Übernahme von Logout Requests der
IdP gemäss EPDV-EDI, Anhang 8, Ausgabe 2.1.

Wenn der Benutzer oder die Benutzerin die IdP Session terminiert und der der
IdP eine Session basiertes SSO nutzt, empfängt das Impfmodul den SAML Logout
Request des IdP und terminiert die User Session im Impfmodul.
