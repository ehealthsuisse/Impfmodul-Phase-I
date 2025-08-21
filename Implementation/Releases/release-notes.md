# August 2025 - Release 1.8.0-RC1
This release candidate contains major changes for all artifacts (frontend, backend, FHIR library).

### Changes Fhir Library
* Updated Java to version 21.
* Updated Spring Boot to version 3.5.0.
* A new extension was added for Immunizations that contains the verification status.
* The RelatesTo section was restored in the Composition resource for editing, validating, and deleting operations.
* Updated default GLN value.

### Changes Vaccination Module Backend
* Updated Java to version 21.
* Updated Spring Boot to version 3.4.8, updated further third party libraries
* Updated Husky Library to the new version 3.1.0.
* Import of vaccination record if no immunization documents are found in the profile
* Enable IDP-specific keystore and truststore settings.
* Enabled propagation and forwarding of logout requests to another url if the session is not found locally. This setting needs to be only used in case the application is run in a cluster.
* Added a new record validation rule: if a PAT edits or comments on a validated record created by an HCP, the record retains its validated status.
* Verification status is set based on the role of the author, as well by taking into account the new validation rule.
* Corrections regarding translations.
* Updated the value lists according to current ballot 6 version.

### Changes Vaccination Module Frontend
* Updated Angular to Version 20.
* The 'Reason for vaccination' field was removed from the edit view but remains visible in the detail view, defaulting to 'Prevention' if no other reason is specified in the document.
* Redesigned comment view so that only one comment will be visible in the comment view, with previous comments accessible only in older document versions.
* When 'Save and create another' button is clicked, the form will be reset to the default values.
* Replaced tooltips with buttons in the list overview, when adding new records also mobile buttons were redesigned.
* The confidentiality logic was moved from the confirmation dialog to the edit view.
* Validation status is now visible in the vaccination certificate overview.
* Added grouping functionality for updated records, with a dropdown to display previous records.
* In the vaccination certificate overview, 'Save' and 'Download' buttons were renamed and consolidated under a new dropdown button labeled 'More'.
* Verification status is now set based on the role of the author.
* Fixed various styling issues.
* Improved translations and error validation.

# May 2025 - Hotfix 1.7.1
This release fixes an issue which was hindering the SAML logout to be proceed via the backchannel.
 * Fixed the above mentioned issue
 * Improved logging for the SAML logout messages, enabling the DEBUG loglevel for the SAMLController class now outputs the full SAML LogoutResponse including the SAML envelope.

# February 2025 - Release 1.7.0
This release wraps up all the RC-feedback for the next stable release, along with few frontend bugfixes for Safari.
* Fixed the issue that the JSESSIONID cookies of other applications running on the same domain were deleted upon login on Safari.

# January 2025 - Release 1.7.0-RC2
This release candidate provides some fixes, especially regarding tomcat compatibility.
* Tomcat 11 compatibility has been fixed, there had been an issue with the CSRF token not being correctly configured.
* Upgraded to the official husky release 3.0.2
* Upgraded few other dependencies on the backend module
* Fixed some testing annotation which had been marked as deprecated.
* Fixed an issue that changing the language of the UI was not persisted after a refresh. 
* Fixed a lay outing issue with the validation texts for the date fields

# November 2024 - Release 1.7.0-RC1
This release candidate contains major changes for all artifacts (frontend, backend, FHIR library).
<br>Starting with 1.7.0, it is necessary to deploy the application on a new tomcat version. Installation guide has been updated.

### Changes Fhir Library
* Refactored FHIR bundle structure based on the new ballot
* Cross References have been reworked, they are no longer linked via compositions but via URL Extensions only. The library will process old and new way of cross referencing alike, i.e. it is backwards compatible.
* Fixed an issue where the wrong GLN was added if an HCP user/author was creating an entry for another HCP who performed the vaccination or did the diagnoses.


### Changes Vaccination Module Backend 
* Updated various libraries, most important were Spring Boot to 3.x and Spring 6.x
* Updated Husky Library to 3.x to use new Convenience API simplifying the Husky requests. Dependency was manually added due to a release problem on husky side. Next husky release will be taken via maven dependency again.
* Refactored Backend API URLs to remove patientIdentifier and localAssigningAuthorityOID as those parameters are retrieved from the session
* Modified the generation of the PDF export, data is now reloaded by the Backend to avoid modifications
* Corrections regarding translations 
* Updated the value lists

### Changes Vaccination Module Frontend  
* Updated Angular to Version 17 
* Updated several other libraries, especially Material to correspond to new Angular version
* Styling adjustments to correspond to the new UI
* Adjusted header for the confirmation dialog
* Fixed the risk factor selection search 
* Fixed a scrolling issue when many entries were present on the list views.
* Added some more date validators for infectious diseases and risk factors
* Value lists are now sorted alphabetically for each language
* Change language to use German when Portal sends RM (Retoromanisch)
* Improved translations

# September 2024 - Hotfix 1.6.3 
This hotfix contains a small extension to the XUA request made to the EPR-backend.
* The XUA request field 'purpose of use' has been extended to set the codeSystemName to "EprPurposeOfUse".

# July 2024 - Hotfix 1.6.2
This hotfix only contains a small but relevant security fix. It is strongly recommended to upgrade to this version.  

In this context, following adaption have been made to the logout request:
* The received logoutRequestMessage is not allowed to contain any DOCTYPE annotations which caused above-mentioned misbehavior. If such an annotation is detected, the request is denied.
* Field LogoutReponseMessage.destination is now filled by the vaccination logout URL if a logout request was sent for an unknown sessionId. 

# June 2024 - Hotfix Frontend 1.6.1
During customer validation, following issues have been identified and fixed. 

* Upon customer request, the Italian language was reintegrated. There will be another update in the future to finalize the texts.
* Fixed an issue linked to the entry validation. 

# June 2024 - Hotfix Backend 1.6.1
During testing, few issues have been identified in the backend module which were fixed in this version.  

Additionally, following points changed:
* To improve maintainability for the providers, some ERROR loglevels have been reduced to warn as those cases could occure during regular operations 
* Fixed an issue linked to the local logout
* Fixed a potential memory leak linked to the session management

# June 2024 - Release 1.6.0
This release wraps up all the RC-feedback for the next stable release.

Additionally, following points changed:
* Added footer to PDF output to show patient name and page number
* Added the possibility to configure a logout forward URL which will forward the user 2 seconds after logout
* Added the possibility to run the vaccination module without IDP authentication against the EPR playground
* Improved validation of EPD data such that only valid data will be displayed
* Fixed the issue that search in dropdown valuesets was only working for lower-case input
* Fixed an issue where logged out IDP sessions tried to be prolonged 
* Fixed an issue that CSRF header is no longer sent when value is empty (only during initial call)
* Fixed validation to allow saving entries with only organization (without performer/recorder)
* Reduced log output to not log personal data
* Major review for DE, EN and FR translations
* IT translation have been temporary removed, they will return next release after a thorough review
* Minor adaptions for the value lists and their translations
* Attention for Chrome browser, using an older version has caused crashes when closing a dropdown, please update to latest version.

# April 2024 - Release 1.6.0-RC3
Included feedbacks provided by the providers and users
* Adjusted value lists to allow addition older vaccins (of myvaccins list)
* Improved robustness in case of unwanted logouts
* Reduced number of requests to backend regarding static information 
* Fixed an issue where adding data with only organization was not corrected saved
* Fixed an issue what validated documents be validated over and over again.
* Fixed few translations in FR and DE

# April 2024 - Release 1.6.0-RC2
Included feedbacks provided by the providers
* Reduced CSRF protection on /signature/validation endpoint, cookie is not known so far
* Fixed another issue on CSRF when hosts are running on different domains
* Allow SAML configuration for ClockSkew

# April 2024 - Release 1.6.0-RC1

### Changes Fhir Library
* Added more validations on the FHIR Adapter to ensure data integrity. 
* Allow log pattern to be set using LOG_DATEFORMAT_PATTERN environment variable. See documentation for more information

### Changes Vaccination Module Backend 
* Added possibility to hand over the user organization (attribute organization) from the portal in the initial web call.<br>
  <b>Important:</b> From next feature release onwards, the field will be mandatory for roles HCP/ASS.
* Added new configuration for samlMessageLifetime (see idp-config-local.yml) which was hard-coded before to 2000 ms
* Adapted value lists for the PDF Output allowing to set a legal remark at the end of the PDF and to display vaccinations in an particular order. 
* Fixed an issue where non-mentioned vaccination proctections on the pdf-output.yml were not added at the end.
* Added language support for the EPR logo shown on the PDF output 
* Added language support for the gender displayed on the PDF output
* Fixed an layout issue with the PDF output when many entries had to be displayed.
* Improved security by implementing CSRF Protection 
* Improved security by adding a logout functionality. The logout will log out the user out of the vaccination module and it will stop the prolonging the extension of the IDP token. It is however not performing an IDP logout. 
* Improved the non-productive local mode by allowing different users to log in.
* Improved security by protecting against ID guessing. On the next release, it will also be planned to remove identifier from rest URLs completely.
* Fixed an issue with the SAML Logout handling

### Changes Vaccination Module Frontend  
* Removed several header values send to the backend. Instead, new X-XSRF-TOKEN header will be send to the backend to ensure the CSRF protection.
* Allow possibility to show/hide the logout button in the configuration
* Added input validation for the provided dates. 
* Added version information on the UI showing frontend and backend version.
* Added support to create new vaccination entries using vaccine code from the myvaccines valuelist incl. their mapping to the vaccination protections
* Added language support to display different company logos.
* Fixed navigation issue that the user was not corrected forwarded on the last visited page when editing or creating a new entry.
* Fixed a language issue on the error page.
* Fixed issues with the ordering of items. Items are now ordered by date. If multiple entries have same date, they are grouped by the used code with newest document first.

# February 2024 - Release 1.5.0

### Changes Fhir Library
* Adapted patient creation for the FHIR documents, using now the correct prefix urn:oid for the system url. 

### Changes Vaccination Module Backend 
* Redesigned adverse events on the PDF report, thereby also fixed an issue where the text was not correctly linewrapped.
* Support the sorting of the valuelists based on priorities (highest first). Priorities can be added as last csv value on each line.
Values with same priorities are sorted alphabetically (based on english translation), Values without priority receive priority 0.
* Changed mime type on back channel logout response to text/xml
* Added few validations on input values to avoid errors in fhir bundles. 
* Minor adaption to the vaccination code valuelist. 

### Changes Vaccination Module Frontend  
* Fixed some translation issues

# February 2024 - Hotfix Release 1.4.2
The new version contains mostly bugfixes which were discoverd during the official audit.   
It is important to fix those topics before going live. 

### Changes Fhir Library
* Adapted bundle creation to correctly retrieve organizations and practitioners, even if practitionerRole is not present.
* Added few input validation

### Changes Vaccination Module Backend 
* Allow logging of the SAML logout request before it is processed, see logging.properties file. 
* Fixed a performance issue when loading the vaccination record
* Fixed the issue which allowed a document to be deleted multiple times
* Fixed the issue which allowed an attacker to read patient information when a foreign session was hijacked.
* Decreased some log-levels to DEBUG to reduce log volume
* Added a read-only protection to the generated PDFs
* Fixed the issue which lead to the wrong system URL being used for the confidentiality code
* Fixed the issue that documents could not be uploaded if EPD setting was put to only allow restricted or secrets documents

### Changes Vaccination Module Frontend  
* Add an organization parameter to the initial call to prefill it in the UI. This feature will be refined in the spring release. 
* Fixed the issue that the wrong confidentiality code was used in a delete call
* Adapted few translations

# January 2024 - Hotfix Release 1.4.1
Version was not published, all changes are included in 1.4.2. 

# January 2024 - Release 1.4.0
Happy New Year everyone, we hope you had a good start!

For the vaccination module, there are some news regarding the release 1.4.0.
Both frontend and backend version are affected:
* Redesign medical problems - in corresponding with eHealth Suisse, the medical problems have been renamed to risk factors to improve the understanding of the data. Hence, a lot of translations were adjusted.
* Next to the value list of the risk factors, we have been updating the vaccination module (and the fhir library) to use the latest Fhir Implementation guide version, i.e. there are some changes in the document structure and in the value list content to be compliant to the CH:VACD validator [Link](https://ehealthsuisse.ihe-europe.net/evs/fhir/validator.seam?standard=41). There is one issue known for the validation. For the risk factor value list, there exists 2 values (Codes 72291000195105 and 72291000195108) which currently do not comply to the validator as we would need to name the swiss extension explicitely in the fhir format. We will do so with the next version, promised!
* Added the possibility to export a vaccination record into an immunization administration document. This temporary feature is provided to some partners which are exporting data into the EPR.
* Up to last version, we had to manually include the Husky Framework into our project. Now, we can officially use their maven dependency.
* Additionally, there have been some bugfixes. 
 * Fixed the issue that the vaccination module stops loading when the content of an entry was not correct.
 * Fixed the issue that the vaccination module stops loading when the references within one document is wrong.
 * Updated Spring Boot (2.7.18), FHIR (6.10.2), Husky (2.2.0) and some other dependencies. Note: We have not been using Spring Boot 3.x yet due to dependencies with Tomcat 10 (Jakarta) and the husky framework which is not ready yet. We are looking into it for the next version.
 * Fixed some more translations

# December 2023 - Hotfix Release 1.3.2

During the tests of several providers, a few bugs were discovered which we fixed in this version.
Both frontend and backend version are concerned:
* Concurrent Transaction: There was an error on the internally utilized Husky Library [Link](https://github.com/project-husky/husky/issues/94) which lead to errors if multiple transactions had to be processed in parallel. This issue was fixed and an intermediate version of husky was added as the release cycle of the husky project takes some more time.
* White Screen during login: An timing issue has been fixed which could lead to a white screen upon login.
* Spinner disappeared to early: Fixed an issue where the entries of the vaccination record could not be seen but the spinner had already disappeared
* Fixed the PDF export which was not executed if the application was built as JAR.
* Fixed some UI related issues

# November 2023 - Hotfix Release 1.3.1

This release contains a hotfix for the vaccination-module-backend incl. its valuelists.
In the valuelists, many system urls have been updated according to the FHIR implementation guide.
Additionally, it is now again possible to choose a vaccine code from the list. Due to a mismatch with the corresponding value list, no entry was selectable.

# November 2023 - Release 1.3

Perfect present for Halloween, **Version 1.3** of the vaccination module is available.

Delivery consists of:
* Frontend Code and compiled code
* Backend Code and compiled JAR (new) and WAR deployment
* FHIR Adapter version 1.0.2

## Features ##
Version 1.3 mainly focussed on stability topics to ensure a succesful Go-Live.

**Health Data**  
All EPD transactions have been validated using the test environment of one EPR community.
This includes both local and cross community transactions. Additionally, some improvements have been made to the transaction handling and created documents.

**Integration Enhancements**  
Since May, tests have been running to improve the integration of the vaccination module for the EPR communities.
With the current version, all current IDPs are now supported by the vaccination module, i.e. SwissSign, HIN and ELCA IDPs.
The SAML support includes:
* Authentication via AuthNRequest
* Artifact Resolve
* Refresh IDP token automatically
* Handling of logout via SOAP Backchannel

Next to the SAML integration, the initial webcall needed by the portal applications was made more flexible to ease the use for portal applications to integration this feature.
Last but not least, some dedicated features (like Emergency Mode) are now supported by the vaccination module. 

**Improved Visibility**  
In order to analyse the vaccination modules behaviour in an easier fashion, logging properties have been introduce which easily allow administrators to set the log level according to their needs.
The properties file can be found on the config folder and contains some demo examples. 

**Increased Security**  
To dispell possible security concerns, an official pentest was run against the vaccination model. 
The mechanism around the initial webcall and data loading has been reworked to suppress illegal data access.

## Configuration ##
**Improved Installation Guide**  
During the testing phase, new parameters have been introduced to ease the integration of the vaccination module with the portal applications. 
Therefor, check out the updated installation guide.

## Further Bugfixes & Improvements ##
* Library itextpdf was replaced by PDFBox and Boxable due to license issues when embedded the vaccination module code in your own application.
* Patient's Confidentiality setting in the EPR is now considered, if a document cannot be saved, it is tried to store it with higher confidentiality.
* Validation of the record is now condering EPR metadata
* Signalisation Controller has been replaced by the Portal Controller.
* Updated config examples to highlight the new properties 
* Many smaller issues fixed.

# May 2023 - Release 1.2

Here is what's new in **Version 1.2** of the vaccination module.

## Features ##
Version 1.2 offers a lot of new features and improvements.

**Improved health data**  
To get a more detailed overview on the health situation of a person, it is now possible to insert information about medical problems.
Furthermore, after receiving feedback from possible endusers, some fields have been removed and modified. 
Additionally, health professionals and assistants are now able to validate patient information on all entities (vaccinations, adverse events, infectious diseases and medical problems).

**EPR Historization**  
Improved readability of the EPR historization by utilizing icons to show status (validated, updated by newer entry, deleted).

**Enter multiple entries**  
The new version allows to enter multiple entries without entering all information from the beginning. 
This should ease the initial effort to bring all your e.g. vaccinations into the system.

**Download EPR information**  
You want to know exactly what is stored in the EPR if you for example save a vaccination? No problem, 
you can now download the original document data which stored as an HL7 JSON format.

**Printversion vaccination record**  
The printversion of the vaccination record received a new layout, so it becomes more inline with the EPR design.

**Responsive Design**  
Are you working on mobile devices? If yes, this version is for you. There is now a flexible responsive design enabling the use of tablets and mobiles.

**Availability of the FHIR Adapter Library**  
The vaccination module contains features to convert business objects to FHIR HL7 bundles and vice versa. 
This functionality is now available as library so if you are interested to use HL7, feel free to use our library.  
We additionally uploaded the source code. As it is not directly linked to the vaccination module, we might transfer it to another repository in the future.  
**Note:** It is not necessary to install the library if you only want to use the vaccination module.

**Web accessibility**  
The usability of the vaccination was improved by respecting known web accessibility standards, e.g. it is no possible to use screenreaders.

**Increased Security**  
In order to respect the EPR security rules, several enhancements were implemented.

## Configuration ##
 - Backend configuration can now be stored outside the deployment folder
 - Improved configuration properties and updated the installation guide accordingly. 

## Bugfixes ##
 - Confidentiality attribute is now considered for deleting records.
 - Improved handling and transactions to the EPR.
 - Value lists and their corresponding translations have been updated according to the HL7 pages
 - Several design and translation issues were fixed.


***  

# December 2022 - Release 1.1 #

A first prototype of the vaccination was published.
