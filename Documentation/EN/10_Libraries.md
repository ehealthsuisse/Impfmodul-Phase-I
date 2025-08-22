## 3rd Party Libraries

### Client 3rd Party Libraries

The presentation layer of the vaccination module uses the following 3rd party libraries:

- Angular 20.1.4, MIT
- ngx-translate 17.0.0, MIT
- dayjs 1.11.3, MIT
- lodash 4.17.21, MIT
- ng-multiselect-dropdown 1.0.0, MIT
- ngx-mat-select-search 8.0.2, MIT
- ngx-ordered-initializer 6.0.0, MIT
- ngx-webstorage 20.0.0, MIT
- rxjs 7.5.0, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- sonar-scanner 3.1.0, MIT
- tslib 2.3.0, [BSD Zero Clause License](https://opensource.org/licenses/0BSD)
- zone.js 0.15.1, MIT

### Web App 3rd Party Libraries
The business and communication layer of the vaccination module uses the following 3rd party libraries: 

- Lombok 1.18.20.0, MIT
- Spring Boot Starter Parent 3.4.8, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- Spring Core 6.2.9, [Apache 2.0](https://github.com/spring-projects/spring-framework/blob/main/LICENSE.txt)
- Project Husky 3.1.0, [Eclipse Public License 1.0](https://www.eclipse.org/org/documents/epl-v10.php)
- HAPI Fire 8.0.0, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- Springdoc-openapi-ui 1.8.0, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- OpenSAML 5.1.3, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- Hazelcast 5.5.0, [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
- PDFBox 2.0.30 [Apache 2.0](https://github.com/apache/pdfbox/blob/trunk/LICENSE.txt)
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
