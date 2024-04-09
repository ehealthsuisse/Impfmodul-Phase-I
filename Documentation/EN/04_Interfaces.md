## Interfaces and API

The vaccination module implements the following interfaces:
* EPR compliant interface to certified identity provider.
* EPD compliant interfaces to EPR platforms.
* Web link to start the vaccination module from EPR portals.
* API for the communication of the presentation logic with the business and communication components.   

### Authentication

The vaccination module implements EPR compliant interfaces to certified identity provider using the
SAML 2.0 Artifact Bindung and SOAP Backchannel protocol.

The vaccination module supports the following protocols:
* User Authentication
* SAML 2.0 Logout

### Interfaces to EPR platforms

The vaccination module implements the following EPR compliant interfaces to EPR platforms:
* Patient Demographics Query HL7 V3 [ITI-47] - Request the XAD-PID and EPR-SPID.
* Get X-User Assertion - Request a XUA Assertion for authorization.
* Registry Stored Query [ITI-18] with Provide X-User Assertion [ITI-40] - Request the document metadata.
* Provide and Register Document Set [ITI-41] mit Provide X-User Assertion [ITI-40] - Store vaccination documents.
* Retrieve Document Set [ITI-43] mit Provide X-User Assertion [ITI-40] - Request vaccination documents by id.  

**Note:** The vaccination modules 2 endpoints (internal, external) each for ITI-18 and ITI-43 as some communities distinguish between local data loading and a cross community data loading.

### Startup from EPR portal

http GET request to initialize the vaccination module from a primary system or the portal of the EPR communities.

The GET request uses the following parameters:
* **lpid** - Patient local ID in the portal or primary system
* **laaoid** - OID of the Patient Local Assigning Authority used by the portal or primary system
* **ufname** - Family/Last name of the User as String
* **ugname** - Given/First name of the User as String
* **utitle** - Title of the user as String (optional)
* **organization** - Name of the user's organization as String (optional)
* **ugln** - Gln of the user, used for HCP and ASS (mandatory for roles HCP/ASS)
* **lang** - Language {lang}_{locale} (z.B. EN_us)
* **role** - EPR role of the user taken from Value Set { HCP, PAT, ASS, REP }
* **purpose** - The users PurposeOfUse from Value Set { NORM, EMER}
* **idp** - String identifying the Identity Provider
* **principalId** - The GLN of the health care professional the user assists
* **principalName** - The Name of the health care professional the user assists
* **timestamp** - UNIX timestamp (UTC) in Milliseconds or seconds (check portal-config.yml)
* **sig** - HMAC SHA-256 signature of the query string, i.e. substring of the URL („?", &sig) either Base64 or Hex encoded


The signature is used to:
* Authenticate the calling portal or primary system.
* Verify the integrity of the call.


### Presentation API

The vaccination module is implemented as Rich Internet Applikation with Javascript Client Components (Angular).

The business logic implements the as Restful API for the UI components:
* CRUD API to display and edit vaccination entries.
* CRUD API to display and edit adverse effects.
* CRUD API to display and edit infectious diseases.
* CRUD API to display and edit medical problems.
