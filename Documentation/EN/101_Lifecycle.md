## Lifecycle management of vaccination data

The regulatory and technical boundary conditions add special requirements to the implementation of the vaccination module, i.e. :
- Documents can be stored in the local community only
- Patients may delete documents from their EPR
- Patients may reduce the visibility of documents by setting the confidentiality code to restricted or secret
- Patients must authorise health professionals (or groups of health professionals) to view the patients documents, e.g. the vaccination documents
- related documents may be distributed over many communities

### Implementation

The vaccination module fulfils the requirements as follows:

When started from a portal or primary system, the vaccination module loads all vaccination documents, evaluates relation and the documents lifecycle and displays the data in UI of the module. The documents retrieved depend on the privacy settings made by the patients, i.e.:
- Patients always retrieve all vaccination documents from their EPR
- Health professionals retrieve only the documents the health professional was authorised to view by the patient, either directly or via a groups.

### Lifecycle Management

In addition to the above rules and regulations in the EPR the following restrictions must be taken into account:

- The vaccination module can store vaccination documents only in the local community
- EPR documents cannot be changed but only replaced by newer versions and only in the local community
- Health professionals cannot delete documents and therefore cannot delete vaccination documents
- Health professionals can update document metadata only in their local community  

To visualise the vaccination data the vaccination module must respect the rules and relations of the documents lifecycle.

The following relations may be established between vaccination documents:
- Vaccination data in one document may correct or extend vaccination data stored in anther document
- Vaccination data in one document may nullify vaccination data stored in anther document
- Vaccination data in one document may add comments to vaccination data stored in anther document

This lifecycle information is managed with a specific set of attributes in the FHIR vaccination profile as follows:

#### Composition Level

A composition element in the FHIR vaccination profile may refer a composition element in another document. The relation is stored in the **relatesTo** element with the following attributes:

- **relatesTo[0].code** : The vaccination module uses  **replaces** only
- **relatesTo[0].targetReference.reference** : The vaccination module uses the UUID of the composition referred to

The referral must be unique, i.e. one composition may only refer a single composition in another document.

##### Example

Siehe line 56 im [update test](../Testfiles/lifecycle/Update-f852a5a7-16ea-46a2-9f0b-e1805b3e96b1.json):

```
"relatesTo": [ {
  "code": "replaces",
  "targetReference": {
    "reference": "urn:uuid:e1141328-d12a-46bf-8034-a301fbfb8734"
  }
 } ]
```

The relation in the example refers to the composition in another file [create test](../Testfiles/lifecycle/Create-6214bb05-3858-480c-aa63-2450dde50e25.json).


#### Immunization Ressource Level

A FHIR resource of type Immunization may refer to another FHR resource of type Immunization. The referral is defined in the FHIR **extension** element with the following attributes:

- **url** : Must be equal **http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference** to be understood as referral.

The referred element must be defined in the **extension** child element with following attributes:

##### Entry

- **extension.url** : Must be equal to **entry**.
- **extension.valueReference.reference** : UUID of the FHIR Immunization resource the element refers to.

##### Document

- **extension.url** : Must be equal to **document**.
- **extension.valueReference.reference** : UUID of the FHIR Composition the element refers to.

##### Relation Code

- **extension.url** : Must be equal to **relationcode**.
- **extension.valueCode** : Must be equal to **replaces**.


##### Beispiel

See line 176 in [update test](../Testfiles/lifecycle/Update-f852a5a7-16ea-46a2-9f0b-e1805b3e96b1.json):

```
"extension": [
{
  "url": http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference,
  "extension": [ {
    "url": "entry",
      "valueReference": {
        "reference": "urn:uuid:1d1a586c-7526-4101-bb8e-174610e7babc"
      }
    }, {
      "url": "document",
      "valueReference": {
        "reference": "urn:uuid:e1141328-d12a-46bf-8034-a301fbfb8734"
      }
    }, {
      "url": "relationcode",
      "valueCode": "replaces"
    } ]
]    
```

The relation refers to a FHIR Immunization resource in a FHIR Composition in file [create test](../Testfiles/lifecycle/Create-6214bb05-3858-480c-aa63-2450dde50e25.json).


### Vaccination module implementation

The vaccination module uses the above mentioned references for the lifecycle management of the vaccination data, i.e.,
for the following use cases:

- Edit and append entries
- Add comments to entries
- Nullify entries

#### Edit and Append

If a user edits or appends a vaccination entry the vaccination module creates a new document of type **ImmunizationAdministration** and stores it in the local community. The document contains a new entry and a referral to the original entry and composition.

The new document replaces the changed entry which is referred to with relation **replaces** and the reference to the composition which contains the original entry.

#### Comments

If a user comments a vaccination entry the vaccination module creates a new document of type **ImmunizationAdministration** and stores it in the local community. The document contains a new entry and a referral to the original entry and composition.

The new document replaces the changed entry which is referred to with relation **replaces** and the reference to the composition which contains the original entry.

#### Nullify

If a user nullifies a vaccination entry the vaccination module creates a new document of type **ImmunizationAdministration** and stores it in the local community. The document contains a new entry and a referral to the original entry and composition.

The new entry is marked with status **enteredInError**.

The new document replaces the changed entry and which referred to with relation **replaces** and the reference to the composition which contains the original entry.
