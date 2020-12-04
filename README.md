[![Build Status](https://jenkins.napps.dev.nuxeo.com/buildStatus/icon?job=nuxeo%2Fnuxeo-retention%2Fmaster)](https://jenkins.napps.dev.nuxeo.com/job/nuxeo/job/nuxeo-retention/job/master/)
[![Preview Link](https://img.shields.io/badge/preview-available-blue)](http://preview-retention-master.napps.dev.nuxeo.com/)
[![License](https://img.shields.io/github/license/nuxeo/nuxeo-retention)](http://www.apache.org/licenses/LICENSE-2.0.html)

# Nuxeo Retention Management

Retention management refers to the control of a document's lifecycle through rules and policies. Documents can be kept or deleted depending on a set of rules which may pertain to time, events or specific metadata contained in the document type.

For more details around functionalities, requirements, installation and usage please consider this addon [official documentation](https://doc.nuxeo.com/n/Kvd).

## Context
Nuxeo Retention Management is an addon that can be plugged to Nuxeo. 

It is bundled as a marketplace package that includes all the backend and frontend contributions needed for [Nuxeo Platform](https://github.com/nuxeo/nuxeo) and [Nuxeo Web UI](https://github.com/nuxeo/nuxeo-web-ui).

## Sub Modules Organization

- **ci**: CI/CD files and configurations responsible to generate preview environments and running Retention Management pipeline
- **nuxeo-retention**: Backend contribution for Nuxeo Platform
- **nuxeo-retention-package**: Builder for [nuxeo-retention](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-retention) marketplace package. This package will install all the necessary mechanisms to integrate Retention Management capabilities into Nuxeo
- **nuxeo-retention-web**: Frontend contribution for Nuxeo Web UI

## Build

Nuxeo's ecosystem is Java based and uses Maven. This addon is not an exception and can be built by simply performing:

```shell script
mvn clean install
```

This will build all the modules except _ci_ and generate the correspondent artifacts: _`.jar`_ files for the contributions, and a _`.zip_ file for the package.

### Frontend Contribution

`nuxeo-retention-web` module is also generating a _`.jar`_ file containing all the artifacts needed for an integration with Nuxeo's ecosystem.
Nevertheless this contribution is basically generating an ES Module ready for being integrated with Nuxeo Web UI.

It is possible to isolate this part of the build by running the following command:

```shell script
npm run build
```

It is using [rollup.js](https://rollupjs.org/guide/en/) to build, optimize and minify the code, making it ready for deployment.

## Test

In a similar way to what was written above about the building process, it is possible to run tests against each one of the modules.

Here, despite being under the same ecosystem, the contributions use different approaches.

### Backend Contribution

#### Unit Tests

```shell script
mvn test
```

### Frontend Contribution

#### Unit Tests

```shell script
npm run test
```

[Web Test Runner](https://modern-web.dev/docs/test-runner/overview/) is the test runner used to run this contribution unit tests.
The tests run against bundled versions of Chromium, Firefox and Webkit, using [Playwright](https://www.npmjs.com/package/playwright)

#### Functional Tests

```shell script
npm run ftest
```

To run the functional tests, [Nuxeo Web UI Functional Testing Framework](https://github.com/nuxeo/nuxeo-web-ui/tree/master/packages/nuxeo-web-ui-ftest) is used.
Due to its inner dependencies, it only works using NodeJS `lts/dubnium`, i.e., `v10`.

## Development Workflow

### Frontend

*Disclaimer:* In order to contribute and develop Nuxeo Retention Management UI, it is assumed that there is a Nuxeo server running with Nuxeo Retention Management package installed and properly configured according the documentation above.

#### Install Dependencies  

```sh
npm install
```

#### Linting & Code Style

The UI contribution has linting to help making the code simpler and safer.

```sh
npm run lint
```

To help on code style and formatting the following command is available. 

```sh
npm run format
```

Both `lint` and `format` commands run automatically before performing a commit in order to help us keeping the code base consistent with the rules defined.

#### Integration with Web UI

Despite being an "independent" project, this frontend contribution is build and aims to run as part of Nuxeo Web UI. So, most of the development will be done under that context.
To have the best experience possible, it is recommended to follow the `Web UI Development workflow` on [repository's README](https://github.com/nuxeo/nuxeo-web-ui/blob/master/README.md).

Since it already contemplates the possibility of integrating packages/addons, it is possible to serve it with `NUXEO_PACKAGES` environment variable pointing to the desired packages/addons.

Not being perfect, the suggested way of integration is to create a _symlink_ inside addons folder, under your checkout of Web UI repo and then serve it with `nuxeo-retention` package/addon.

***Creating the Symlink***
```shell script
cd ${yourWebUiCheckout}/addons
ln -s ~/${yourRetentionManagementCheckout}/nuxeo-retention/nuxeo-retention-web nuxeo-retention
```

***Serving Web UI with Retention Management***

```shell script
NUXEO_PACKAGES="nuxeo-retention" npm run start
```

## CI/CD

Continuous Integration & Continuous Deployment(and Delivery) are an important part of the development process.

Nuxeo Retention Management integrates [Jenkins pipelines](https://jenkins.napps.dev.nuxeo.com/job/nuxeo/job/nuxeo-retention/) for each maintenance branch, for _master_ (fast track) and also for each opened PR. 

The following features are available:
- Possibility of having a dedicated preview environment for a PR by using the tag GitHub PR tag `preview`
- Each PR merge to _master_ branch will generate a "release candidate" package
- A preview aligned on _master_ branch code is available

### Localization Management

Nuxeo Retention Management, as well as Nuxeo Web UI, handles multilingual content. In order to manage all that in an effective way and integration with [Crowdin](https://crowdin.com/) is used.

This integration is also handled under our CI/CD context with [a dedicated pipeline](https://jenkins.napps.dev.nuxeo.com/job/nuxeo/job/crowdin/job/nuxeo-retention/) benefiting from automatic translations and correspondent commits.

# About Nuxeo

The [Nuxeo Platform](http://www.nuxeo.com/products/content-management-platform/) is an open source customizable and extensible content management platform for building business applications. It provides the foundation for developing [document management](http://www.nuxeo.com/solutions/document-management/), [digital asset management](http://www.nuxeo.com/solutions/digital-asset-management/), [case management application](http://www.nuxeo.com/solutions/case-management/) and [knowledge management](http://www.nuxeo.com/solutions/advanced-knowledge-base/). You can easily add features using ready-to-use addons or by extending the platform using its extension point system.

The Nuxeo Platform is developed and supported by Nuxeo, with contributions from the community.

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).