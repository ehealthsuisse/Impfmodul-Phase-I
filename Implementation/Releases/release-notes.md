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
