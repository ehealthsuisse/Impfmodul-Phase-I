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