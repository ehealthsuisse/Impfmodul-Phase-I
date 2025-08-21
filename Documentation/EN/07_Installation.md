## Installation Guide

The delivery package of the vaccination module consists of two parts:
 - A front end application based on Angular
 - A Java based back end application
 
Additionally, starting with version 1.2, the FHIR adapter library is provided. This library is already embedded in the backend version, but can be used independently to transform FHIR bundles (xml/json) to domain objects and vice versa.

### Preliminaries

The vaccination module requires the following pre-installed elements:
 - A Java Web Application Server
 - Java 21 Runtime Environment
 - Network connection to an IdP and an EPR platform

The vaccination module is provided as the following artifacts:
 - a Web Archive (WAR) of the Backend Application (z.B. *vaccination-module-\<version\>.war*)
 - a folder with the JavaScript code of the Frontend part.

The vaccination module can be deployed on a Unix or Windows platform.

This installation guide documents the installation on a Windows system and a Apache Tomcat Application Server (Version 11.x.x). The adaptation for other platforms should be self explaining.


#### Build Manually

The vaccination module can be build manually. You may use the following steps for building manually:

**Frontend**

1. Install [npm](https://www.npmjs.com/)
2. Install of the [Angular CLI](https://cli.angular.io/)
3. Compile the vaccination module using `npm run buildProd` in the terminal
4. After compiling the vaccination module software will be stored in the *dist/vaccination-module-frontend* subfolder.

**Backend**
1. Install [Java 21 SDK](https://developers.redhat.com/products/openjdk/download)
2. Install the Build-Tools [Maven](https://maven.apache.org/download.cgi)
3. Compile the backend component using `mvn clean install -Pwar`
4. A WAR-Archive file will created and stored in the *target* subfolder.

**Remark**: When using *mvn clean install* a jar file of the vaccination module backend will be created.

You will find additional information in the [README Files](https://github.com/ehealthsuisse/Impfmodul-Phase-I/tree/main/Implementation) of the Software Repositories.


### Install

To install the vaccination module copy the the WAR archive and the Vaccination-Module-Frontend folder to the deployment folder of application server (`%CATALINA_HOME%\webapps`). The folder *vaccination-module-frontend* should be renamed beforehand  since the name affects the URL of the vaccination module.

After copying the files the application server should deploy the vaccination module software automatically.

### Configuration

#### Frontend  

The configuration of the vaccination module frontend is stored in file `assets\config\production-config.json`.

```
{
  "backendURL": "https://this.is.my.server.url/vaccination-module-backend",
  "communityId": "EPDBackend",
  "allowStartWithoutInitialCall": false,
  "isLogoutButtonVisible": true,
  "logoutForwardUrl": ""
}
```
It configs 2 parameter to be configured:
- *backendURL* : Defines the URL of the vaccination module backend.
- *communityId* : The OID of the community the vaccination module is connected to.
- *allowStartWithoutInitialCall* : Allows the start of the vaccination module without the initial webcall.<br>
**Important**: Put *allowStartWithoutInitialCall* to false for productive use.
- *isLogoutButtonVisible* : Allows showing/hiding the logout button.
- *logoutForwardUrl* : Possibility to add an URL to which the user will be forwarded. If kept empty, user will stay on the logout page.

#### Backend

The Web Archive of the vaccination module backend uses the following configuration files in the `config` folder:
 - `config\testfiles` and `config\testfiles\json` required for testing purposes only.
 - `config\valuelists` all files apart from the following 2 are configurations of the value sets used. 
 - `config\valuelists\pdf-output.yml` configuration of pdf-output used for printing.
 - `config\valuelists\vaccines-to-targetdiseases.yml` mapping tables to map vaccines to targetdiseases.
 - `fhir.yml` configuration of the FHIR profile for vaccination.
 - `husky.yml` configuration of the communication components of the Husky Library.
 - `idp-config-local.yml` configuration of the Identity Providers to be used.
 - `logging.properties` log configuration to adjust the log level for particular Java packages.
 - `portal-config.yml` configuration of the Portal-Interface to start the module.

There are two additional parameter which must be set as environment variables described below.

**Configuration of Environment Variables**  

At startup of the vaccination module backend a banner is displayed as follows.

```
,-----.     ,---.    ,----.      ,--.   ,--.                        ,--.                     ,--.   ,--.                  
|  |) /_   /  O  \  '  .-./       \  `.'  /   ,--,--.  ,---.  ,---. `--' ,--,--,   ,--,--. ,-'  '-. `--'  ,---.  ,--,--,  
|  .-.  \ |  .-.  | |  | .---.     \     /   ' ,-.  | | .--' | .--' ,--. |      \ ' ,-.  | '-.  .-' ,--. | .-. | |      \
|  '--' / |  | |  | '  '--'  |      \   /    \ '-'  | \ `--. \ `--. |  | |  ||  | \ '-'  |   |  |   |  | ' '-' ' |  ||  |
`------'  `--' `--'  `------'        `-'      `--`--'  `---'  `---' `--' `--''--'  `--`--'   `--'   `--'  `---'  `--''--'

Vaccination Module for the EPR
profile: local
config: ${vaccination_config}

Powered by Spring Boot 3.4.8
```

The following environment variables must be set appropriately:
 - *spring.profiles.active*: Defines the environment the vaccination module is operated with. The default is set  *local*, which must be changed to **prod** for productive usage. By setting to **prod** the debug and testing functions will be removed to increase the operation security.
 - *vaccination_config*: Defines the location of the configuration. The default is set to *config*. It's recommended to locate the config folder somewhere outside the `webroot` folder for security reasons.
 - *server.port*: Port number of the vaccination backend. The default is set to 8080.
 - *FRONTEND_URL*: Defines which frontend URL may connect to the vaccination backend. The default is set to \* which allows all URL. The value may be set to a specific URL Whitelist for security reasons.
 - *LOG_DATEFORMAT_PATTERN*: Allows provider to define their own dateformat for the log entries. The format follows ISO8601.

**Configuration of Test Operation Mode**  

The vaccination module supports a test operation mode in which all vaccination documents are read from and written to a specific folder of the local file system to folder `config\testfiles` and `config\testfiles\json` instead of writing or reading documents from the EPR.  

The test mode is activated by setting the parameter *LOCALMODE* and *HUSKYLOCALMODE* to true. Additionally, it can be initialized with a GET request to *\<backendURL>/utility/setLocalMode/true*. The default mode is **deactivated** by default. Localmode cannot be activated for spring profile *prod*.

**Value Set Configuration**  

The vaccination module uses all value sets stored in folder `config\valuelists` with the [FHIR vaccination profile](http://fhir.ch/ig/ch-vacd/terminology.html). 

**FHIR Configuration**
Configures some aspects of the [FHIR vaccination profile](http://fhir.ch/ig/ch-vacd/terminology.html) used by the vaccination module.

**Husky Configuration**  
The vaccination module uses the [Husky](https://github.com/project-husky/husky) library to communicate with the EPR platform. To work properly all endpoints of the EPR platform required to operate the vaccination module must be configured.

A simplified configuration example is shown below:   

```
epdbackend:
  # Logging of soap messages, for testing purpose only, put to false on prod machines.
  activateSoapLogging: false
  soapLoggingPath: ${vaccination_config:config}/log/

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
        - identifier: InternalRegistryStoredQuery
          [...]

        # Retrieve Document Set: Get and display document contents - own community
        - identifier: InternalRetrieveDocumentSet
          [...]

        # Registry Stored Query: Get and display document metadata - cross community
        - identifier: ExternalRegistryStoredQuery
          [...]

        # Retrieve Document Set: Get and display document contents - cross community
        - identifier: ExternalRetrieveDocumentSet
          [...]

        # Submit Document Set: Submit a document
        - identifier: SubmitDocument
          [...]

        # XUA / STS: Get X-User Asstion
        - identifier: XUA
          {...]

# Configuration of the ATNA Logging provided by the husky framework
ipf:
  atna:
  {...]
```

The parameter in the config file are interpreted as follows:
 - activateSoapLogging: Enables logging of the soap messages sent and received by the EPR endpoints.
 - soapLoggingPath: Path, where the 2 logfiles (incomingSoapMessages.log and outgoingSoapMessages.log) are logged. During boot up path is shown in the logs, search for IpfApplicationConfig.

 - sender.applicationOid: OID of the vaccination module used in the EPR context.
 - sender.facilityOid: OID to optionally add a more fine grained information in addition to the vaccination module OID.
 - communities: Configuration of the community endpoints required to operate the vaccination module.

The community settings are as follows:
 - identifier: OID of the community.
 - globalAssigningAuthorityOid: Global oder root OID of the Master Patient Index of the community.
 - SpidEprOid: Assigning authority of the ZAS for the EPR-SPID.
 - repositories: Section of endpoints the vaccination module shall connect to. Please note that for there are two RegistryStoredQuery and for the RetrieveDocumentSet endpoints. If a community combined both functionality into a single call, just leave the URL for the external endpoints empty. **Do not remove them!** 
 - repositories.identifier: **don't override**, since the value is used internally to link the application code to the configuration.
 - uri: The URL of the community EPR service endpoint.
 - receiver.applicationOid/facilityOid: OID of the receiving endpoint.
 - repositories.xua: This is only relevant for the XUA repository, here the additional settings enable the user to deploy a different keystore for the XUA exchange.
 - ipf.atna: Defines the ATNA Logging which is mandatory for EPR transactions. Make sure to set *ipf.atna.audit-enabled* to true and put in the correct details. When using TLS as transport protocol (which should be done for productive use), MTLS credentials will be taken from the default security key and truststore, see chapter *Security Configuration*.
 
**IdP Configuration**

The vaccination module requires the Identity Provider settings to be able to authenticate users during operation. The configuration covers the SAML Metadata exchanged between the community or platform provider and the Identity Providers. The list of Identity Provider setting is multivalued and the vaccination module supports all Identity Provider configured in the list.

An simplified example of the Identity Provider configuration is shown below.
```
# Identity Provider
idp:
  # SP Entity ID that is known to the IdP
  knownEntityId: myVaccinationModule

  # Configuration how much derivation (in ms) is between sending an SAML request and receiving the response.
  samlMessageLifetime: 2000

  # Provide per Provider
  supportedProvider:
  - identifier: GAZELLE
    authnrequestURL: https://ehealthsuisse.ihe-europe.net:4443/idp/profile/SAML2/Redirect/SSO
    artifactResolutionServiceURL: https://ehealthsuisse.ihe-europe.net:4443/idp/profile/SAML2/SOAP/ArtifactResolution
    securityTokenServiceURL: https://is.not.set.for.gazelle
    entityId: OverwriteDefaultEntityIDIfNecessary
    logoutURL: https://logout.url.set.in.logout.request.destination
    samlKeystore: # IDP specific setting overriding the sp.keystore setting 
      keystoreType: PKCS12
      keystorePath: path.to.keystore.p12
      keystorePassword: password
      spAlias: spkeyAlias
    tlsKeystore: # IDP specific setting overriding the sp.keystore setting 
      keystoreType: JKS
      keystorePath: path.to.keystore.jks
      keystorePassword: password
  - identifier: [...]

# Service Provider - this application
sp:
  assertionConsumerServiceUrl: https://my.backend.url/saml/sso
  forwardArtifactToClientUrl: https://my.frontend.url/saml-acs
  otherNodeLogoutURL: https://otherNode.backend.url/saml/logout

  # keystore containing the private key used for MTLS with the EPD backend
  keystore:
   keystore-type: PKCS12
   keystore-path: path.to.keystore
   keystore-password: keystore.password
   sp-alias: key.alias.used.for.idp
  # Keystore containing the private key used for MTLS with the IDPs
  tlsKeystore:
    keystore-type: JKS
    keystore-path: path.to.keystore.jks
    keystore-password: password 
```

The parameter are interpreted as follows:
- knownEntityId: A Unique identifier of the Identity Provider in the vaccination module. Should be set to the URL of the Identity Provider endpoint.
- samlMessageLifetime: Configuration how much derivation (in ms) is between sending an SAML request and receiving the response. If time is exceeded, there will be an exception in the SAML flow.
- supportedProvider: Grouping element for Identity Provider settings.
- supportedProvider.identifier: Label of the Identity Provider which is used in the Portal at vaccination startup.The label is used by the vaccination model to link to the Identity Provider the user logged in the portal.
- supportedProvider.authnrequestURL: URL of the Identity Provider accepting *AuthNRequest*.
- supportedProvider.artifactResolutionServiceURL: URL of the Identity Providers accepting the SAML Artefact and returning the IdP Assertion..
- supportedProvider.securityTokenServiceURL: URL of the security token service needed to refresh the IDP token.
- supportedProvider.entityId: Allows to override the entity ID for a specific IDP. This was a necessary workaround for HIN IDP to ensure that both vaccination module and portal application resolve to same NameID within the IDP Token.
- supportedProvider.logoutURL: URL string filled into the destination field of the LogoutResponse. If not set, the issuer from the LogoutRequest is used.
- supportedProvider.samlKeystore: Allows for an IDP-specific configuration, if it is not filled, default configuration will be used.
- supportedProvider.samlKeystore.keystoreType: Type of the IdP keystore file (JKS or PKCS12).
- supportedProvider.samlKeystore.keystorePath: Filesystem path to the IdP keystore file.
- supportedProvider.samlKeystore.keystorePassword: Password to access the IdP keystore.
- supportedProvider.samlKeystore.spAlias: Alias of the service provider’s private key within the IdP keystore.
- supportedProvider.tlsKeystore: Configuration of the IdP’s truststore used for mTLS communication.
- supportedProvider.tlsKeystore.keystoreType: Type of the IdP truststore file (JKS or PKCS12).
- supportedProvider.tlsKeystore.keystorePath: Filesystem path to the IdP truststore file.
- supportedProvider.tlsKeystore.keystorePassword: Password to access the IdP truststore.
- assertionConsumerServiceUrl: URL of the vaccination module accepting callbacks from the Identity Provider. Using the path variable *{idp}*, the vaccination module now can look up which IDP configuration it needs to check for the artifact response.
- forwardArtifactToClientUrl: URL of a backend endpoint which will forward the SAMLartifact to the Angular frontend application. This workaround was implemented if it is not possible by the IDP to send a HTTP GET to the Frontend application directly.
- otherNodeLogoutURL: URL of a secondary backend node to forward logout requests when the session cannot be retrieved locally. Enables logout propagation across nodes when configured.
  
**Note:** The suffix */saml/sso* is mandatory and only the root address must be set.
- keystore: General keystore setting which can be overwritten by the IDP-specific setting. If not set, the keystore according to the security configuration chapter (see below) is used. 
- keystore.keystore-type: Type of the Keystore file (either JKS or PKCS12).
- keystore.keystore-path: Path to the Keystore file.
- keystore-password: Password used to access the keystore.
- sp-alias: Alias name of the private key used for the communication with the Identity Provider.

- truststore: General Truststore setting which can be overwritten by the IDP-specific setting. If not set, the keystore according to the security configuration chapter (see below) is used..
- tlsKeystore.keystore-type: Type of the Keystore file (either JKS or PKCS12).
- tlsKeystore.keystore-path: Path to the Keystore file.
- tlsKeystore-password: Password used to access the keystore.

**Logging Property**  
The configuration of the log levels are located in the *logging.properties* file. Each row starting with *logging.level* represents a setting.
Each setting consists of a package name and the corresponding loglevel. The package names are hereby structured hierarchically, i.e. configuration for logging.level.ch includes all subpages unless there is a more specific role.
Following debug level are available
* INFO = general information 
** Be aware that user related information is logged which could violate data protection regularions. Should only be used for error analysis.**
* DEBUG = detailed information incl. privacy information.  
* TRACE = For the SAML configuration, it is possible to log all telegrams going over the interfaces. **Does contain sensitive information**

**Portal Configuration**  

The vaccination module is started from the Portal or Primary System with a GET request which conveys a set of parameters, e.g., to identify the patient or time stamp. The startup request is signed with HMAC to verify the integrity and authenticate the calling application, which is configured in the Portal configuration.

```
# Configuration used for the weblink
portal:
  # Shared secret between portal and vaccination modul to verify webcall signature
  hmacpresharedkey: sharedKey
  # Must be set to true to ensure that web calls are not older than x seconds
  activateTimestampCheck: true
  # Default value 2000 millis 
  timestampAllowedDerivationMillis: 2000
  # Use timestamp in seconds (default milliseconds)
  useTimestampInSeconds: false
  # Encode signature in Base64 (true) or Hex (false) 
  encodeSignatureBase64: true
```
The parameter are interpreted as follows:
- hmacpresharedkey: A unique key shared between the vaccination module and the calling application (e.g., portal). The key is used for the HMAC signature of the startup request.  
- activateTimestampCheck: For testing purposes the value can be set to *false*, which suppresses the verificatio nof the timestamp. The default is *true* for productive use.
- timestampAllowedDerivationMillis: Can be used to tune the validity of the initial request before it is processed by the vaccination module. If it is too short, users could experience an unwanted login denial. If it is too long, an attacker could possible grab the web call and initialte his own instance. Please tune during testing mode.
Please note that milliseconds are cut if parameter useTimestampInSeconds is true.
- useTimestampInSeconds: Flag to indicate whether the portal transmits the timestamp in seconds or milliseconds. Later one is preferred due to better tunability of the duration.
- encodeSignatureBase64: Flag to indicate whether the portal transmits the signature Base64 or Hex-encoded.

**Security Configuration**

This configuration file configures the information used by the vaccination module to access the private and public keys used to secure the communication to the EPR platform and the Identity Provider.

The parameter are interpreted as follows:
 - *javax.net.ssl.keyStore*: Path to the Keystore for securing the TLS connections.
 - *javax.net.ssl.keyStorePassword*: Password for the Keystore.
 - *javax.net.ssl.trustStore*: Path to the trustStore for securing the TLS connections.
 - *javax.net.ssl.trustStorePassword*: Password for the Truststore.

These settings can be overwritten using the configuration in the idp-config-local.yaml 
**Note**: The keystore must contain the private key sibling of the public key defined in the SAML-Metadata. The truststore must contain all public keys used for mTLS of the endpoints of the EPR platform and the identity provider.
