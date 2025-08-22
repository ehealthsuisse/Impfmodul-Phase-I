## Overview

This documentation covers the following documents:

### General documentation

1. [Scope of the vaccination module](./01_Scope.md)
2. [Classification of Data](./02_Data.md)
3. [Vaccination module architecture](./03_Architecture.md)
4. [Interfaces and API](./04_Interfaces.md)
5. [Authentication](./05_Authentication.md)
6. [Authorization](./06_Authorization.md)
7. [Installation Guide](./07_Installation.md)
8. [Licence](./09_License.md)
9. [3rd Party Libraries](./10_Libraries.md)


### Additional documentation

1. [Lifecycle management of vaccination data](./101_Lifecycle.md)
2. [Visualization of vaccination data](./102_VACD.md)
3. [Value Sets](./103_ValueSets.md)

### Releases
All detailed release notes can be found [here](https://github.com/ehealthsuisse/Impfmodul-Phase-I/blob/main/Implementation/Releases/release-notes.md).

### August 2025 - Release 1.8.0-RC1
The module has been enhanced by incorporating feedback from users, providers, and audits, while also integrating the latest technical advancements.

1. Upgrade the frontend application to use Angular 20.
2. Upgrade the backend application as well as the fhir library to use Java 21.
3. Backend application now uses the new Husky library version 3.1.0.
4. Added a new parameter in the IdPs configuration files.
5. Enable IDP-specific keystore and truststore settings.
6. FHIR value lists update along with various bugfixes.

### May 2025 - Hotfix 1.7.1
An issue has been identified which was hindering the SAML logout to be proceed via the backchannel.

### February 2025 - Release 1.7.0
This release wraps up all the RC-feedback for the next stable release, along with few frontend bugfixes for Safari.

### January 2025 - Release 1.7.0-RC2
The 2nd release candidate contains a fix for Tomcat 11 compatibility as well as few dependency upgrades and bugfixes.

### November 2024 - Release 1.7.0-RC1
Improving the vaccination module based on new FHIR regulations and technical evolutions.

1. Upgrade the frontend application to use Angular 17
2. Upgrade the backend application to use Spring Boot 3.x and Spring 6.x. This major change has an impact on the environment. Starting with 1.7.0, the vaccination module needs to be run with Tomcat 10+. This version was tested to run with Tomcat 11.0.1
3. Various upgrades of the FHIR bundle structure. These updates mainly concern metaURLs and the cross-references. Old bundle structures will be backwards compatible.
4. Various bugfixes

### September 2024 - Hotfix 1.6.3 
This hotfix contains a small extension to the XUA request made to the EPR-backend.

### Juli 2024 - Hotfix 1.6.2 
This hotfix contains an important security fix. It is strongly recommended to upgrade your version.

### June 2024 - Hotfixes 1.6.1 for frontend and backend
Hotfix for 1.6.0, fixing few issues spotted on provider's infrastructure.

Additionally added updated help-files into the documentation.

#### April-June 2024 - Release 1.6.0 and 1.6.0-RCs
Improving the vaccination module based on user, provider and audit feedback.

1. Added possibility to create vaccinations based on older vaccine codes.
2. Optimized PDF Output (one more time) to fix layouting issues when having many entries
3. Improved input validation and usability
4. Improved security features
5. Secured correct visibility of migrated data of meineImpfungen.ch
6. Revised translations for the help files.

#### February 2024 - Release 1.5.0
Extension of the vaccination module with the following functions:

1. New Design of Adverse Event section in the PDF report
2. Allow configurational sorting of the value lists
3. Slight adjustments for the FHIR bundle creation and value lists.

#### February 2024 - Hotfix Release 1.4.2
Improving the vaccination module based on user, provider and audit feedback.

#### January 2024 - Hotfix Release 1.4.1
Version was not published, all changes are included in 1.4.2. 

#### Release 1.4, January 2024
Extension of the vaccination module with the following functions:

1. Redesign medical problems 
2. Usage of the last FHIR Implementation guide 
3. Improvement of the user interfaces and backend according to user feedback


#### Hotfix Release 1.3.1 and 1.3.2, November 2023
There have been 2 hotfix release for version 1.3. focussing mainly on issues arosen during the EPR tests. 


#### Release 1.3, November 2023
Version 1.3 mainly focussed on stability and robustness. Following areas were improved:

1. SAML Integration
2. Security aspects
3. Logging for analysing purposes
4. PDF generation
4. User manual


#### Release 1.2, Mai 2023
Extension of the vaccination module with the following functions:

1. Responsive Design for Mobile und Tablets
2. Addition of visualisation and editing of medical problems
3. Java library and API of the business functions
4. Improvement of the user interfaces according to user feedback


#### Release 1.1, Dezember 2022
Extension of the vaccination module with the following functions:

1. UI improvement according to user feedback,
2. Addition of a test mode with local storage,
3. Web Accessibility improvement,
4. Selection of the confidentiality of documents created.


#### Release 1.0, Oktober 2022
First version of the vaccination module with the core functions for authentication, for the communication with EPR platforms and the visualisation of the vaccination data.
