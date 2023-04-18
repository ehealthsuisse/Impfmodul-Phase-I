## Authorization

The vaccination module uses the EPR compliant interfaces for authentication of the user.

### Information Processing

The vaccination module requires a valid X-User Assertion to retrieve the vaccination data from the EPR communities. To retrieve the X-User Assertion the vaccination module using the Get X-Assertion transactions with a valid IdP Assertion in the SOAP security header. At startup the vaccination module therefore retrieves the required data to perform the EPR compliant authentication flow.

The X-Assertion Provider of the EPR community resolves the User ID (Name Id of the IdP Assertion)
to the GLN of health professional or the EPR-SPID of the patient. The EPR community authorises the access to the vaccination documents by using the information provided with the X-User Assertion and the patient consent stored in the patient home community.
