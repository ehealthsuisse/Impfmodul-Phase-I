## Authentication

The vaccination module requires a EPR compliant IdP Assertion of the user to authenticate the request for a X-User Assertion used by the EPR platform to perform access decisions. The vaccination module therefore implements the EPR compliant protocols to authenticate the user:  
* SAML 2 http Artifact Binding and Artifact Resolution protocol via SOAP Backchannel,  
* IdP Renew via SOAP Backchannel,
* SAML 2 Logout protocol with SOAP Backchannel Binding.


### Information Processing

#### Authentication

The vaccination module implements the requirements for user authentication of the EPDV-EDI, Annex 8.

The vaccination module retrieves the information required to perform the authentication
from the HTTP GET parameter when started from a portal or primary system.

The vaccination module uses the information to retrieve the IdP Assertion of the user currently logged in the portal or primary system. The vaccination module sends a SAML Authentication Request to the Identity Provider which authenticated the user. The Identity Provider returns the SAML Artifact, if the user has a valid session at the Identity Provider. If the user has no valid session at the Identity Provider, the user is delegated to the login page of the Identity Provider.  

The vaccination module resolves the SAML Artifact to the Identity Assertion via the Backchannel by sending a SAML Artifact Resolve Request. The Identity provider responds with the Identity Assertion.


#### IdP Renew

The vaccination module implements the requirements for an IdP Renew as defined in EPDV-EDI,
Annex 8. When the user session is established, the vaccination module refreshes the Identity Assertion via an Backchannel IdP Renew Transaction.  

#### IdP Logout

The vaccination module provides an endpoint to accept Logout Requests send by the IdP as defined in EPDV-EDI, Annex 8.
