[![Build Status](https://jenkins.platform.dev.nuxeo.com/buildStatus/icon?job=nuxeo%2Fnuxeo-retention%2Flts-2021)](https://jenkins.platform.dev.nuxeo.com/job/retention/job/nuxeo-retention/job/lts-2021/)

# Nuxeo Retention

The Nuxeo Retention addon adds the capability to create and attach retention rules to documents in order to perform advanced record management

For more details around functionalities, requirements, installation and usage please consider this addon [official documentation](https://doc.nuxeo.com/nxdoc/nuxeo-retention-management/).

## Context
Nuxeo Retention is an addon that can be plugged to Nuxeo. 

It is bundled as a marketplace package that includes all the backend and frontend contributions needed for [Nuxeo Platform](https://github.com/nuxeo/nuxeo-lts) and [Nuxeo Web UI](https://github.com/nuxeo/nuxeo-web-ui).

## Sub Modules Organization

- **ci**: CI/CD files and configurations responsible to generate preview environments and running Retention pipeline
- **nuxeo-retention**: Backend contribution for Nuxeo Platform
- **nuxeo-retention-package**: Builder for [nuxeo-retention](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-retention) marketplace package. This package will install all the necessary mechanisms to integrate Retention capabilities into Nuxeo
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

#### Functional Tests

```shell script
npm run ftest
```

To run the functional tests, [Nuxeo Web UI Functional Testing Framework](https://github.com/nuxeo/nuxeo-web-ui/tree/maintenance-3.0.x/packages/nuxeo-web-ui-ftest) is used.
Due to its inner dependencies, it only works using NodeJS `v14`.

## Development Workflow

### Frontend

*Disclaimer:* In order to contribute and develop Nuxeo Retention UI, it is assumed that there is a Nuxeo server running with Nuxeo Retention package installed and properly configured according the documentation above.

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
To have the best experience possible, it is recommended to follow the `Web UI Development workflow` on [repository's README](https://github.com/nuxeo/nuxeo-web-ui/tree/maintenance-3.0.x).

Since it already contemplates the possibility of integrating packages/addons, it is possible to serve it with `NUXEO_PACKAGES` environment variable pointing to the desired packages/addons.


## CI/CD

Continuous Integration & Continuous Deployment(and Delivery) are an important part of the development process.

Nuxeo Retention integrates [Jenkins pipelines](https://jenkins.platform.dev.nuxeo.com/job/retention/job/nuxeo-retention/) for each maintenance branch and for each opened PR. 

The following features are available:
- Each PR merge to _lts-2021_/_lts-2023_ branch will generate a "release candidate" package

### Localization Management

Nuxeo Retention manages multilingual content with a [Crowdin](https://crowdin.com/) integration.

The [Crowdin](.github/workflows/crowdin.yml) GitHub Actions workflow handles automatic translations and related pull requests.

# About Nuxeo

The [Nuxeo Platform](http://www.nuxeo.com/products/content-management-platform/) is an open source customizable and extensible content management platform for building business applications. It provides the foundation for developing [document management](http://www.nuxeo.com/solutions/document-management/), [digital asset management](http://www.nuxeo.com/solutions/digital-asset-management/), [case management application](http://www.nuxeo.com/solutions/case-management/) and [knowledge management](http://www.nuxeo.com/solutions/advanced-knowledge-base/). You can easily add features using ready-to-use addons or by extending the platform using its extension point system.

The Nuxeo Platform is developed and supported by Nuxeo, with contributions from the community.

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).