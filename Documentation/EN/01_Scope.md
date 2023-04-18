## Scope of the vaccination module

### Overview
eHealth Suisse supports the aim to establish an electronic vaccination record for all inhabitants based on the Swiss EPR. Major challenges are the distributed architecture and the EPR compliant authorisation on the document level, especially:  

- Health professionals store vaccination document in their community. The vaccination documents of one patient may therefore be distributed over many EPR communities which do all match the patients home community.
- Vaccination documents can have a lifecyle and can relate each other in the case of correction, addition or when comments are added. In some cases the related documents will be stored in separate communities.
- The options to modify metadata of the documents are restricted. For example health professionals cannot edit edit metadata of documents stored in remote communities.

This adds requirements to the portals to consistently combine the data from the various vaccination documents to present the vaccination record to health professionals and patients. Portals must:
- Implement the rules and algorithms to respect the lifecycle and the relations between the vaccination documents.
- Provide user interfaces to add and update vaccination data.
- Support the FHIR profiles for vaccination used in the EPR.

The implementation of the requirements is simplified by integrating the vaccination module provided by eHealth Suisse, which implements the required functions and can be integrated to the EPR portals:  

![Figure: Vaccination module usage](Images/scope-1.JPG)

Figure: Vaccination module usage

### Objective
The vaccination module is an add on to the Web Portals provided by the communities and implements the functions required to combine the vaccination document respecting the document lifecycle and the User Interface to view, edit and update vaccination data.

The vaccination module implements:   
- The transactions to retrieve and store vaccination documents.
- The rules and algorithms to combine the data from various documents respecting the document lifecycle.
- User interfaces the create, update and edit vaccination data in the EPR.
- FHIR profiles for vaccination data in the EPR.
- EPR compliant Authentication with SAML 2.0 Single Sign On.

### Intended User
The vaccination module may be used by the regular user of the Swiss EPR:
1. Healthcare professionals
2. Assistants
3. Patients
4. Representatives
