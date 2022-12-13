## Installationshandbuch
Die Impfmodule Software besteht aus 2 Teilen: 
 - einer Angular Frontend Anwendung und
 - einer Java Backend Anwendung

### Voraussetzungen
Um die Anwendung installieren und starten zu können sind folgende Voraussetzungen zu erfüllen:
 - Bereitstellung eines Application Servers
 - Java 17 Runtime Environment
 - Integration in eine vollständige EPD Backend Infrastruktur inkl. Identity Provider (IDP) 

Im Rahmen dieser Anleitung gehen wir davon aus, dass Ihnen beide Anwendungen als fertiges Deployment vorliegen, d.h. Sie haben
 - ein Web Archive (WAR) für die Backend Applikation (z.B. *vaccination-module-\<version\>.war*)
 - ein Distributions Order mit den transkompilierten JavaScript Code für die Frontend Anwendung.

Die Software kann sowohl auf Windows als auch auf Unix Systemen deployt werden.
Für die weitere Anleitung wird zur Einfachheit angenommen, dass ein Apache Tomcat Application Server (Version 9.x.x) auf einer Windows Umgebung verwendet wird. 

#### Manueller Build 
Liegen Ihnen diese Artifakte nicht vor, sind weitere Schritte und Konfigurationen notwendig. Zur Ausführung wird ein Hintergrund in der Software Entwicklung empfohlen.

**Frontend**

1. Installation von [npm](https://www.npmjs.com/)
2. Installation der [Angular CLI](https://cli.angular.io/)
3. Kompilieren der Anwendung mittels `npm run buildProd`
4. Das Distributionsverzeichnis befindet sich dann im *dist* Subverzeichnis.
 
**Backend**
1. Installation eines [Java 17 SDK](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
2. Installation des Build-Tools [Maven](https://maven.apache.org/download.cgi)
3. Kompilieren der Anwendung mittels `mvn clean install -Pwar` 
4. Das erstellte WAR-Archive befindet sich dann im *target* Subverzeichnis.

**Hinweis**: Wird das Backend ohne ein Profil gebaut (*mvn clean install*), so wird ein lokal startbares Jar generiert. 

Mehr Informationen finden Sie in den jeweiligen [README Dateien](https://github.com/ehealthsuisse/Impfmodul-Phase-I/tree/main/Implementation) der Software Repositories 

### Installation
Zur Installation der Anwendung kopieren Sie das WAR-Archive sowie den Vaccination-Module-Frontend-Ordner in das Deployment-Verzeichnis Ihres Application Servers (`%CATALINA_HOME%\webapps`). Der *dist* Ordner sollte zuvor sinnvoll umbenannt werden, da Name des Verzeichnisses unmittelbare Auswirkung auf die URL hat. 

Der Application Server deployt dann automatisch sowohl Frontend als auch Backend, jedoch ist weitere Konfiguration notwendig, um die Anwendung verwenden zu können. 

### Konfiguration 
#### Frontend  
Für die Konfiguration des Frontend gibt es eine Konfigurationsdatei. Sie enthält 2 Konfigurationsparamter und befindet sich unter `assets\config.json`
```
{
  "backendURL": "https://this.is.my.server.url/vaccination-module-backend",
  "communityId": "EPDBackend"
}
``` 
Der Parameter *backendURL* gibt an, unter welcher URL das Backend erreichbar ist. \
Der Parameter *communityId* gibt an, welche EPD Backend Community verwendet werden soll. Die EPD Backend Community ist Teil der folgenden Backend Konfiguration.

#### Backend

Das Web Archive des Backends beinhaltet einen vollständigen `config` Ordner verschiedensten Konfigurationen:
 - `config\testfiles` und `config\testfiles\json` werden nur im Testbetrieb benötigt.
 - `config\valuelists` fachliche Konfiguration 
 - `fhir.yml` Konfiguration zum FHIR Austauschformat
 - `husky.yml` Konfiguration der Husky Bibliothek
 - `idp-config.yml` Konfiguration der verwendeten Identity Provider 
 - `portal-config.yml` Konfiguration der Portal-Schnittstelle.

Weiterhin gibt es zwei Umgebungsparameter, die konfiguriert werden müssen. Im Folgenden werden alle Konfigurationen detaillierter beschrieben.

**Konfiguration Umgebungsparameter**  
Wird das Backend gestartet, erscheint im Log zuerst das Banner.

```
,-----.     ,---.    ,----.      ,--.   ,--.                        ,--.                     ,--.   ,--.                  
|  |) /_   /  O  \  '  .-./       \  `.'  /   ,--,--.  ,---.  ,---. `--' ,--,--,   ,--,--. ,-'  '-. `--'  ,---.  ,--,--,  
|  .-.  \ |  .-.  | |  | .---.     \     /   ' ,-.  | | .--' | .--' ,--. |      \ ' ,-.  | '-.  .-' ,--. | .-. | |      \ 
|  '--' / |  | |  | '  '--'  |      \   /    \ '-'  | \ `--. \ `--. |  | |  ||  | \ '-'  |   |  |   |  | ' '-' ' |  ||  | 
`------'  `--' `--'  `------'        `-'      `--`--'  `---'  `---' `--' `--''--'  `--`--'   `--'   `--'  `---'  `--''--' 
                                                                                                                          
Vaccination Module for the EPR 
profile: local 
config: ${vaccination_config}

Powered by Spring Boot 2.7.5
``` 

Unterhalb werden zwei der Umgebungsparameter angezeigt:
 - *spring.profiles.active*: Gibt an, in welcher Umgebung sich die Applikation befindet. Defaultwert *local*   
 Für einen Produktivbetrieb **muss** dieser Wert auf **prod** gesetzt werden. Hierdurch werden z.T. Security Richtlinien sowie Zugriffe auf die Entwicklertools unterbunden.
 - *vaccination_config*: Dieser Parameter gibt an, wo sich der oben genannte Konfigurationsordner befindet. Wir empfehlen diesen Ordner aus Sicherheitsgründen an einen beliebigen Ort ausserhalb des `webroot` Verzeichnisses zu verschieben. Defaultwert *config* 
 
Weiterhin gibt es noch folgende weitere Parameter:
 - *server.port*: Gibt an, auf welchem Port die Backendanwendung läuft, default ist 8080.
 - *FRONTEND_URL*: Gibt an, von welcher Frontend URL auf das Backend zugegriffen werden darf. Kann verwendet werden, um die Sicherheit zu erhöhen. Default * (alle URLs erlauben)

**Konfiguration Testbetrieb**  
Im Backend ist ein Test- resp. lokaler Betriebsmodus integriert. Dieser kann aktiviert werden, indem das Profil auf *local* gesetzt wird und der Testmodus mit dem GET-Webaufruf *\<backendURL>/utility/setLocalMode/true* aktiviert wird. Der Testmodus is per default **deaktiviert**.
Im lokalen Betriebsmodus benötigt es keine EPD Infrastruktur und es werden alle beliebige (valide) FHIR-Austauschformate unterhalb der beiden Verzeichnisse 
`config\testfiles` und `config\testfiles\json` als Datenbasis verwendet. Werden neue Einträge gespeichert, so werden diese ebenfalls dort abgelegt. 

**Konfiguration Wertelisten**  
Im Subverzeichnis `config\valuelists` liegen alle Wertelisten resp. ValueSets, welche gemäss [FHIR Austauschformat](http://fhir.ch/ig/ch-vacd/terminology.html) für die abgebildeten Entitäten verwendet werden. Eine manuelle Anpassung ist <ins>nicht</ins> notwendig.

**Konfiguration FHIR**  
Diese Konfiguration bildet einige Basiskonfigurationen für das FHIR Austauschformat an. Eine manuelle Anpassung ist <ins>nicht</ins> notwendig.


**Konfiguration Husky**  
[Husky](https://github.com/project-husky/husky) ist eine Bibliothek zum vereinfachten Zugriff auf die EPD Backend Infrastruktur. Für die korrekte Konfiguration müssen verschiedene Parameter gesetzt werden.

Folgend ist eine vereinfachte Konfiguration dargestellt: 
``` 
epdbackend:
  sender:
    applicationOid: "1.2.3.4"
    facilityOid: 

  communities:
    # Identfier must correspond to communityId given in the Frontend 
    - identifier: EPDBackend
      globalAssigningAuthorityOid: "1.2.3.4.5.6.7"
      spidEprOid: "1.2.3.4.5.6.7"
      repositories:
        # PDQ: Query the master patient ID and EPR-SPID for patients
        - identifier: PDQ
          uri: https://my.epd.backend.url/pdq
          receiver:
            applicationOid: "1.2.3.4.5.6.7"
            facilityOid: "1.2.3.4.5.6.7"

        # Registry Stored Query: Get and display document metadata
        - identifier: RegistryStoredQuery
          [...]

        # Retrieve Document Set: Get and display document contents
        - identifier: RetrieveDocumentSet
          [...]

        # Submit Document Set: Submit a document
        - identifier: SubmitDocument
          [...]
		  
        # ATNA: EPD Logging
        - identifier: ATNA
          [...]

        # XUA / STS: Get X-User Asstion
        - identifier: XUA
          {...]	
``` 
Die einzelnen Parameter haben folgende Bedeutung:
 - sender.applicationOid: Eindeutige OID dieser Anwendung innerhalb des EPD. Über die [refdata](https://oid.refdata.ch/) sind allfällig neue OIDs zu melden.
 - sender.facilityOid: Diese OID kann optional zur feineren Granularität der OID verwendet werden.
 - communities: Das Backend ist funktional so erstellt, dass verschiedene EPD Backend Communities parallel unterstützt werden können. Im Regelfall braucht aber nur eine Community konfiguriert werden, da jede Instanz im Produktivbetrieb aus Performanzgründen nur mit der lokalen EPD Backend Infrastruktur kommunizieren sollte. Pro Community sind verschiedene SOAP Endpunkt zu konfigurieren.
 
 Alle weiteren Parameter beziehen sich auf die Konfiguration einer einzelnen Community:
 - identifier: Eindeutiger Bezeichner einer Community.
 - globalAssigningAuthorityOid: Globale oder Root OID des Master Patient Index, welcher für die jeweilige Community verantwortlich ist.
 - SpidEprOid: Assigning Authority für die SpidEpr 
 - repositories: Oberbegriff über die verschiedenen Soap Endpunkte
 - repositories.identifier: **nicht überschreiben**, dienen zur Verbindung zwischen Code und Konfiguration.
 - uri: Pro Endpunkt ist die URL anzugeben, unter die der Endpunkt verfügbar ist.
 - receiver.applicationOid/facilityOid: Pro Endpunkt kann die genaue OID des Receivers hinterlegt werden.
 
**Konfiguration IDP**  
Der IDP wird verwendet, um den Anwender eindeutig zu identifizieren. Im Unterschied zur EPDBackend Konfiguration **muss** das Impfmodul jeden Provider unterstützen, der auch von den Impfportalen, in die die Applikation eingebettet wird, unterstützt wird. 

Hierbei ist die Konfiguration nur mittels expliziter Integration der IDPs möglich, d.h. zwischen Betrieber und IDP müssen Daten ausgetauscht werden (sogeannte SAML-Metadaten). Diese Daten beinhalten die Informationen über die genaue Konfiguration.

Folgend ist eine vereinfachte Konfiguration dargestellt: 
``` 
# Identity Provider
idp:
  # SP Entity ID that is known to the IdP
  knownEntityId: myVaccinationModule

  # Provide per Provider
  # HTTP-Post Binding URL
  # SAML2.0-SOAP Binding URL 
  supportedProvider:
  - identifier: GAZELLE
    authnrequestURL: https://ehealthsuisse.ihe-europe.net:4443/idp/profile/SAML2/Redirect/SSO
    artifactResolutionServiceURL: https://ehealthsuisse.ihe-europe.net:4443/idp/profile/SAML2/SOAP/ArtifactResolution 
  - identifier: [...]
  
# Service Provider - this application
sp:
  assertionConsumerServiceUrl: https://my.backend.url/saml/sso
    
  # keystore containing our private key
  keystore:
   keystore-path: path.to.keystore.p12
   keystore-password: password 
   sp-alias: spkeyAlias
```
Die einzelnen Parameter haben folgende Bedeutung:
- knownEntityId: Eindeutiger, mit dem IDP vereinbarter Identifier. Wir empfehlen die Zielurl des Systems als Identifier zu verwenden, um die Eindeutigkeit zu gewährleisten.
- supportedProvider: Oberbegriff über alle Provider
- supportedProvider.identifier: Eindeutiger Identifier eines Providers.  
  **Wichtig:** Der Identifier wird während des initialen Webaufrufes vom Portal übergeben, d.h. diese Werte sind mit dem Betreiber der Portalapplikation abzugleichen.
- supportedProvider.authnrequestURL: URL des Providers unter der dieser das AuthNRequest annimmt.
- supportedProvider.artifactResolutionServiceURL: URL des Providers, unter der dieser die SAML Artefakte entgegennimmt und gegen die IDP Tokens auflöst. 
- assertionConsumerServiceUrl: URL der Anwendung, um die Antwort eines Providers entgegen zu nehmen.  
**Wichtig:** Der Suffix */saml/sso* muss bestehen bleiben, nur die Server URL ist anzupassen.
- keystore: Allgemeine Einstellungen zum Keystore, welcher diese Applikation ggü. dem IDP identifiziert. 
**Wichtig:** Hier sind sensitive Informationen enthalten, daher nochmals die Empfehlung, den Konfigurationsordner nicht innerhalb des `webroot`-Verzeichnisses zu belassen.
- keystore.keystore-path: Pfad zur Keystore Datei
- keystore-password: Passwort des Keystores
- sp-alias: Alias Name des privaten Schlüssels, der für die Kommunikation zum IDP benötigt wird. 

**Konfiguration Portal**  
Das Impfmodul wird initial von einer Dritt-Applikation aufgerufen, meistens sogenannten Impf-Portalen. Der initiale Aufruf enthält verschiedene Parameter, um beispielsweise den Patienten zu identifizieren.
Zur Absicherung wird der initiale Webaufruf mit einer HMAC gesichert, um die Integrität des aufrufenden Systems zu prüfen. Weiterhin ist ein Timestamp Check aktiv, der absichert, dass ein Request nicht abgehört und anderweitig eingespielt wird. 
 
```
# Keystore used for the weblink
portal:
  # Shared secret between portal and vaccination modul to verify webcall signature
  hmacpresharedkey: "portalKey"
  # Must be set to true to ensure that web calls are not older than 2 seconds
  activateTimestampCheck: "true"
```
Die einzelnen Parameter haben folgende Bedeutung:
- hmacpresharedkey: Eindeutiger Key, welcher zwischen Portal- und Impfmodule Applikation verabredet werden muss. Wird verwendet um eine HMAC Signature an den initialen Webaufruf zu hängen.
- activateTimestampCheck: Kann zu Testzwecken auf *false* gestellt werden, um die Prüfung der Timestampdaten zu unterdrücken.  
**Wichtig** Im Produktivbetrieb muss der Wert auf *true* verbleiben.

**Konfiguration Security**  
Neben diesen spezifischen Einstellung sind weitere Einstellung für die Verbindungssicherheit notwendig. Im Rahmen der Kommunikation innerhalb des EPDs wird mit viel mittels digitaler Signaturen abgesichert. Um diese Sicherheit zu gewährleisten, müssen entsprechende Key- und Truststores konfiguriert werden.

Die Stores werden mittels weiterer Umgebungsvariablen definiert:
 - *javax.net.ssl.keyStore*: Pfad zum Keystore für MTLS 
 - *javax.net.ssl.keyStorePassword*: Passwort zum o.g. Keystore
 - *javax.net.ssl.trustStore*: Pfad zum Keystore für MTLS 
 - *javax.net.ssl.trustStorePassword*: Passwort zum o.g. Keystore
 
Im Keystore ist derjenige private Key zu hinterlegen, dessen Public Key in den SAML-Metadaten angegeben wurde. Im Truststore sind die Zertifikate aller gegenstellen (IDP, EDP Backend Infrastructur) zu hinterlegen, damit das MTLS (mutual TLS) akzeptiert wird. 