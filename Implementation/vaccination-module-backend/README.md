# Vaccination Module Backend

![license](https://img.shields.io/badge/License-MIT-green)
![JDK17](https://img.shields.io/badge/java-JDK17-blue)

Spring Boot Backend Application

## Getting started

Install the project with mvn clean install

## Start the application

mvn spring-boot:run

## Swagger
Only available in profile local
- [Swagger Local](https://localhost:8080/swagger-ui/index.html)

## License

See License.md

## Overview

### Components
The application is based on REST controller and external libraries.

![](docs/plantuml/components.png)


### Sequences
The sequences of the data exchange of the application is the following:

![](docs/plantuml/sequences.png)

## Authentication
The authentication is based on [oasis-open.org](http://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0.html).
5.1.3 SP-Initiated SSO: POST/Artifact Bindings
* /saml/login: Registering the IDP (will be changed in the future)
* /saml/sso: Handling the AuthnResponse from IDP 
* /logout: Handle the logout SAML request


### Deployment
The application contains 3 main libraries:

![](docs/plantuml/deployment.png)

* HAPI/FHIR: to handle the document in json format.
* Husky: to access repositories.
* Saml: To manage the saml token.