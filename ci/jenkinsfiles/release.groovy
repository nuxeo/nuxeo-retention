/*
* (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* Contributors:
*     Abdoul BA <aba@nuxeo.com>
*/

def appName = 'nuxeo-retention'
def pipelineLib
def repositoryUrl = 'https://github.com/nuxeo/nuxeo-retention/'

properties([
  [
    $class: 'BuildDiscarderProperty',
    strategy: [
      $class: 'LogRotator',
      daysToKeepStr: '15', numToKeepStr: '10',
      artifactNumToKeepStr: '5'
    ]
  ],
  [
    $class: 'GithubProjectProperty', projectUrlStr: repositoryUrl
  ],
  disableConcurrentBuilds(),
])

String currentVersion() {
  return readMavenPom().getVersion()
}

String getReleaseVersion(String version) {
  return version.replace('-SNAPSHOT', '')
}

pipeline {
  agent {
    label 'builder-maven-nuxeo-11'
  }
  parameters {
    string(name: 'rcVersion', description: 'Version to be promoted')
    string(name: 'reference', description: 'Reference branch to be bumped after releasing')
    booleanParam(
      name: 'dryRun', defaultValue: true,
      description: 'if true all steps will be run without publishing the artifact'
    )
  }
  environment {
    APP_NAME = "${appName}"
    BACKEND_FOLDER = "${WORKSPACE}/nuxeo-retention"
    BRANCH_NAME = GIT_BRANCH.replace('origin/', '')
    BRANCH_LC = "${BRANCH_NAME.toLowerCase().replace('.', '-')}"
    CLEANUP_PREVIEW = 'true'
    CONNECT_PROD_URL = 'https://connect.nuxeo.com/nuxeo'
    CHART_DIR = 'ci/helm/preview'
    CURRENT_VERSION = currentVersion()
    ENABLE_GITHUB_STATUS = 'false'
    FRONTEND_FOLDER = "${WORKSPACE}/nuxeo-retention-web"
    JENKINS_HOME = '/root'
    MAVEN_DEBUG = '-e'
    MAVEN_OPTS = "${MAVEN_OPTS} -Xms512m -Xmx3072m"
    NUXEO_VERSION = '2021.0'
    NUXEO_BASE_IMAGE = "docker-private.packages.nuxeo.com/nuxeo/nuxeo:${NUXEO_VERSION}"
    ORG = 'nuxeo'
    PREVIEW_NAMESPACE = "retention-${BRANCH_LC}"
    REFERENCE_BRANCH = '2021'
    IS_REFERENCE_BRANCH = "${BRANCH_NAME == REFERENCE_BRANCH}"
    VERSION = getReleaseVersion(CURRENT_VERSION)
  }
  stages {
    stage('Check parameters') {
      steps {
        script {
          if (!params.rcVersion || params.rcVersion == '') {
            currentBuild.result = 'ABORTED'
            currentBuild.description = 'Missing required version parameter, aborting the build.'
            error(currentBuild.description)
          }

          if (!params.reference || params.reference == '') {
            currentBuild.result = 'ABORTED'
            currentBuild.description = 'Missing required reference parameter, aborting the build.'
            error(currentBuild.description)
          } else {
            echo '''
              ----------------------------------------
              Update Reference Branch
              ----------------------------------------
            '''
            env.REFERENCE_BRANCH = params.reference
          }

          if (params.dryRun && params.dryRun != '') {
            env.DRY_RUN_RELEASE = params.dryRun
            if (env.DRY_RUN_RELEASE == 'true') {
              env.SLACK_CHANNEL = 'infra-napps'
            }
          } else {
            env.SLACK_CHANNEL = 'pr-napps'
            env.DRY_RUN_RELEASE = 'false'
          }
          env.RC_VERSION = params.rcVersion
          env.PACKAGE_BASE_NAME = "nuxeo-retention-package-${VERSION}"
          env.PACKAGE_FILENAME = "nuxeo-retention-package/target/${PACKAGE_BASE_NAME}.zip"
          echo """
            -----------------------------------------------------------
            Release nuxeo retention connector
            -----------------------------------------------------------
            ----------------------------------------
            Retention package:   ${PACKAGE_BASE_NAME}
            Build version:    ${RC_VERSION}
            Current version:  ${CURRENT_VERSION}
            Release version:  ${VERSION}
            Reference branch: ${REFERENCE_BRANCH}
            ----------------------------------------
          """
        }
      }
    }
    stage('Load Common Library') {
      steps {
        container('maven') {
          script {
            pipelineLib = load 'ci/jenkinsfiles/common-lib.groovy'
          }
        }
      }
    }
    stage('Notify promotion start on slack') {
      steps {
        script {
          String message = "Starting release ${VERSION} from build ${env.RC_VERSION}: ${BUILD_URL}"
          pipelineLib.setSlackBuildStatus("${SLACK_CHANNEL}", "${message}", 'gray')
        }
      }
    }
    stage('Set Labels') {
      steps {
        container('maven') {
          script {
            pipelineLib.setLabels()
          }
        }
      }
    }
    stage('Setup') {
      steps {
        container('maven') {
          script {
            pipelineLib.setup()
            sh 'env'
          }
        }
      }
    }
    stage('Fetch Release Candidate') {
      steps {
        container('maven') {
          sh "git fetch origin 'refs/tags/v${RC_VERSION}*:refs/tags/v${RC_VERSION}*'"
        }
      }
    }
    stage('Checkout') {
      steps {
        container('maven') {
          script {
            pipelineLib.gitCheckout("v${RC_VERSION}")
          }
        }
      }
    }
    stage('Update Version') {
      steps {
        container('maven') {
          script {
            pipelineLib.updateVersion("${VERSION}")
          }
        }
      }
    }
    stage('Compile') {
      steps {
        container('maven') {
          script {
            pipelineLib.compile()
          }
        }
      }
    }
    stage('Linting') {
      steps {
        container('maven') {
          script {
            pipelineLib.lint()
          }
        }
      }
    }
    stage('Run Unit Tests') {
      steps {
        script {
          def stages = [:]
          stages['backend'] = pipelineLib.runBackEndUnitTests()
          parallel stages
        }
      }
    }
    stage('Build Docker Image') {
      steps {
        container('maven') {
          script {
            pipelineLib.buildDockerImage()
          }
        }
      }
    }
    stage('Buid Helm Chart') {
      steps {
        container('maven') {
          script {
            pipelineLib.buildHelmChart("${CHART_DIR}")
          }
        }
      }
    }
    stage('Deploy Retention Preview') {
      steps {
        container('maven') {
          script {
            pipelineLib.deployPreview(
              "${PREVIEW_NAMESPACE}", "${CHART_DIR}", "${CLEANUP_PREVIEW}", "${repositoryUrl}", "${IS_REFERENCE_BRANCH}"
            )
          }
        }
      }
    }
    stage('Run Functional Tests') {
      steps {
        container('maven') {
          script {
            try {
              retry(2) {
                pipelineLib.runFunctionalTests("${FRONTEND_FOLDER}", "${PREVIEW_NAMESPACE}")
              }
            } catch(err) {
              throw err
            } finally {
              //retrieve preview logs
              pipelineLib.getPreviewLogs("${PREVIEW_NAMESPACE}")
              cucumber (
                fileIncludePattern: '**/*.json',
                jsonReportDirectory: "${FRONTEND_FOLDER}/ftest/target/cucumber-reports/",
                sortingMethod: 'NATURAL'
              )
              archiveArtifacts (
                allowEmptyArchive: true,
                artifacts: 'nuxeo-retention-web/target/**, logs/*.log' //we can't use full path when archiving artifacts
              )
            }
          }
        }
      }
      post {
        always {
          container('maven') {
            script {
              //cleanup the preview
              pipelineLib.cleanupPreview("${PREVIEW_NAMESPACE}")
            }
          }
        }
      }
    }
    stage('Publish') {
      when {
        allOf {
          not {
            branch 'PR-*'
          }
          not {
            environment name: 'DRY_RUN', value: 'true'
          }
        }
      }
      environment {
        MESSAGE = "Release ${VERSION}"
        TAG = "v${VERSION}"
      }
      stages {
        stage('Git Commit and Tag') {
          steps {
            container('maven') {
              script {
                pipelineLib.gitCommit("${MESSAGE}", '-a')
                pipelineLib.gitTag("${TAG}", "${MESSAGE}")
              }
            }
          }
        }
        stage('Publish Retention Package') {
          steps {
            container('maven') {
              script {
                echo """
                  -------------------------------------------------
                  Upload Retention Package ${VERSION} to ${CONNECT_PROD_URL}
                  -------------------------------------------------
                """
                if (DRY_RUN_RELEASE == 'false') {
                  pipelineLib.uploadPackage("${VERSION}", 'connect-preprod', "${CONNECT_PROD_URL}")
                }
              }
            }
          }
          post {
            always {
              archiveArtifacts (
                allowEmptyArchive: true,
                artifacts: 'nuxeo-retention-package/target/nuxeo-retention-package-*.zip'
              )
            }
          }
        }
        stage('Git Push') {
          steps {
            container('maven') {
              echo """
                --------------------------
                Git push ${TAG}
                --------------------------
              """
              script {
                if (DRY_RUN_RELEASE == 'false') {
                  pipelineLib.gitPush("${TAG}")
                }
              }
            }
          }
        }
      }
    }
    stage('Release and Bump reference branch') {
      when {
        allOf {
          not {
            environment name: 'DRY_RUN', value: 'true'
          }
        }
      }
      steps {
        container('maven') {
          script {
            //cleanup files updated by other stages
            sh 'git reset --hard'
            // increment minor version
            String nextVersion =
              sh(returnStdout: true, script: "perl -pe 's/\\b(\\d+)(?=\\D*\$)/\$1+1/e' <<< ${CURRENT_VERSION}").trim()
            echo """
              -----------------------------------------------
              Update ${REFERENCE_BRANCH} version from ${CURRENT_VERSION} to ${nextVersion}
              -----------------------------------------------
            """
            pipelineLib.gitCheckout("${REFERENCE_BRANCH}")
            pipelineLib.updateVersion("${nextVersion}")
            pipelineLib.gitCommit("Release ${VERSION}, update ${CURRENT_VERSION} to ${nextVersion}", '-a')
            if (DRY_RUN_RELEASE == 'false') {
              pipelineLib.gitPush("${REFERENCE_BRANCH}")
            }
          }
        }
      }
    }
  }
  post {
    always {
      script {
        currentBuild.description = "Release ${VERSION} from build ${RC_VERSION}"
      }
    }
    success {
      script {
        // update Slack Channel
        String message = "Successfully released ${VERSION} from build ${env.RC_VERSION}: ${BUILD_URL} :tada:"
        pipelineLib.setSlackBuildStatus("${SLACK_CHANNEL}", "${message}", 'good')
      }
    }
    failure {
      script {
        // update Slack Channel
        String message = "Failed to release ${VERSION} from build ${env.RC_VERSION}: ${BUILD_URL}"
        pipelineLib.setSlackBuildStatus("${SLACK_CHANNEL}", "${message}", 'danger')
      }
    }
  }
}
