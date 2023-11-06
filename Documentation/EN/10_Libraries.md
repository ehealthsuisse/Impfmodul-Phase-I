## 3rd Party Libraries

### Client 3rd Party Libraries

The presentation layer of the vaccination module uses the following 3rd party libraries:

- Angular 15.2.9, MIT
- ngx-translate 14.0.0, MIT
- dayjs 1.11.3, MIT
- lodash 4.17.21, MIT
- ng-multiselect-dropdown 1.0.0-beta.19, MIT
- ngx-mat-select-search 4.2.1, MIT
- ngx-ordered-initializer 4.0.0, MIT
- ngx-webstorage 10.0.1, MIT
- rxjs 7.5.0, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- sonar-scanner 3.1.0, MIT
- tslib 2.3.0, [BSD Zero Clause License](https://opensource.org/licenses/0BSD)
- zone.js 0.11.4, MIT

### Web App 3rd Party Libraries
The business and communication layer of the vaccination module uses the following 3rd party libraries: 

- Lombok 1.18.30.0, MIT
- Spring Boot Starter Parent 2.7.17, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- Spring Core 5.3.30, [Apache 2.0](https://github.com/spring-projects/spring-framework/blob/main/LICENSE.txt)
- Project Husky 2.0.0, [Eclipse Public License 1.0](https://www.eclipse.org/org/documents/epl-v10.php)
- HAPI Fire 6.2.5, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- Springdoc-openapi-ui 1.6.15, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- OpenSAML 4.3.0, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- Haselcast 5.1.7, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- PDFBox 2.0.29 [Apache 2.0](https://github.com/apache/pdfbox/blob/trunk/LICENSE.txt)
- Boxable 1.7.0 [Apache 2.0](https://github.com/dhorions/boxable/blob/master/COPYING)


### Development Process

As the vaccination module is used in the sensitive area of Public Health, it must be ensured that the contents of the deliverables are well-protected against modifications.

Following mechanisms are used to prevent the source code from unauthorized modifications and the libraries:
* The 4-eyes-principal is used during the development process, i.e. there is no new code without any review. This ensures that the code performs as intended.
* The source code is published in an closed repository on Github, which is read-only by default. Only users authorized by eHealth Suisse are allowed to provide new code or modify existing code. Github is a professional platform to host source code and implements state of the art security features [Github Security Features](https://docs.github.com/en/code-security/getting-started/github-security-features).
* The source code is provided by Sopra Steria on behalf of eHealth Suisse. Before the source code is published, Sopra Steria uses it own propritary development infrastructure - the digital enabling platform - which follows best practices and is used by over 50'000 employees around the globe.
* 3rd party libraries are taken from the Maven Repositories. Maven is a centralized dependency management framework which allows developers to specify all dependencies in a centralized file, here [pom.xml](https://github.com/ehealthsuisse/Impfmodul-Phase-I/blob/main/Implementation/vaccination-module-backend/pom.xml). This way, it is ensured that all dependencies have been previously checked by the repository owners as those repositories have their own protection mechanisms.
* 3rd party libraries are updated if new versions of the libraries are available and an update is recommended, especially when security issues have been fixed by the 3rd parties. Updates of the 3rd party libraries result in a new release of the vaccination module which is published on Github. 
* Platform providers are informed timely if updates or new versions of the vaccination module are available in the operational control group under which is organized and administrated by eHealth Suisse.
