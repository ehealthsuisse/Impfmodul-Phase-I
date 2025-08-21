# Vaccination Module Frontend

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 20.1.4.

## Development

Used to set the editor configuration. so all developers can use the same settings.

### pre-start

Before you can build this project, you must **INSTALL** and configure the following dependencies on your machine:

1. [node.js](https://nodejs.org/en/): We use Node to run a development web server and build the project.

2. [npm](https://www.npmjs.com/): We use npm to install and manage our dependencies.

3. [Angular CLI](https://cli.angular.io/): We use Angular CLI to generate client code.

if you do not have node and npm installed on your machine, you can install them with the Node Version Manager [NVM](https://github.com/nvm-sh/nvm#install--update-script).

After installing Node, you should be able to run the following command to install development tools.
You will only need to run this command when dependencies change in [package.json](package.json).

```
npm install
```

npm script and Angular CLI are used as build system.

#### Development server

Run the following command to start the development server. the development server will automatically build the project and serve it from the root of the project.
and open a browser to http://localhost:4200. The application will automatically reload if you change any of the source files.

```
ng serve -o
```

#### script commands

| Name                    | purpose                                                                         |
| ----------------------- | :------------------------------------------------------------------------------ |
| ng build                | build the project. The build artifacts will be stored in the `dist/` directory. |
| ng test                 | execute the unit tests via [Karma](https://karma-runner.github.io).             |
| npm run lint            | execute the linting configuration.                                              |
| npm run lint:fix        | used to configure the linting configuration                                     |
| npm run prettier:check  | check if the code is match with the style guide the code style                  |
| npm run prettier:format | format code to match style guid                                                 |

### Managing dependencies

Before deciding to install any dependencies should do the following:

1. Check if the dependency is already installed.
2. check if the dependency is updated regularly.
3. use [bundle phobia](https://bundlephobia.com/) to check the following

   - [x] the size of the bundle
   - [x] is it tree-shakable
   - [x] dose the package support Gzip compression

to install a dev dependency, run the following command:

```
npm install <package-name> -D
```

to install a prod dependency, run the following command:

```
npm install <package-name>
```

For example, to add the dependency moment.js, run the following command:

```
npm install moment --save
```

### Using Angular CLI

You can also use [Angular CLI][] to generate some custom client code.

For example, the following command:

```
ng generate component x-component
```

will generate few files:

```
create src/main/webapp/app/my-component/x-component.component.html
create src/main/webapp/app/my-component/x-component.component.ts
create src/main/webapp/app/my-component/x-component.component.spec.ts
create src/main/webapp/app/my-component/x-component.component.scss
update src/main/webapp/app/app.module.ts
```

## Project Structure

The project root directory contains the following

#### config files/directories

| Name            | purpose                                                                                              |
| --------------- | :--------------------------------------------------------------------------------------------------- |
| .browserslistrc | used by the build system to adjust CSS and JS output to support the specified browsers listed in it. |
| .editorconfig   | used to set the editor configuration. so all developers can use the same settings.                   |
| .eslintignore   | used to instruct eslint to not wast time linting the mentioned file inside.                          |
| .eslintrc.json  | used to configure the linting configuration                                                          |
| .gitlab-ci.yml  | used to configure the CI system. development ci system // only for soprasteria                       |
| .prettierrc     | enforces a consistent code style for all developer                                                   |
| .prettierignore | ignore checking code style                                                                           |
| .husky          | used as git hook command                                                                             |
