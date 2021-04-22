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
*     Nuno Cunha <ncunha@nuxeo.com>
*/

/* Using a version specifier, such as branch, tag, etc */
library identifier: "nuxeo-napps-tools@0.0.6"

def appName = 'nuxeo-retention'
def repositoryUrl = 'https://github.com/nuxeo/nuxeo-retention/'

def runBackEndUnitTests() {
  return {
    stage('BackEnd') {
      container('maven') {
        script {
          try {
            echo '''
              ----------------------------------------
              Run BackEnd Unit tests
              ----------------------------------------
            '''
            sh """
              cd ${BACKEND_FOLDER}
              mvn ${MAVEN_ARGS} -V -T0.8C test
            """
          } catch (err) {
            throw err
          } finally {
            junit testResults: "**/target/surefire-reports/*.xml"
          }
        }
      }
    }
  }
}

pipeline {
  agent {
    label 'builder-maven-nuxeo-11'
  }
  options {
    disableConcurrentBuilds()
    buildDiscarder(logRotator(daysToKeepStr: '15', numToKeepStr: '10', artifactNumToKeepStr: '5'))
  }
  environment {
    APP_NAME = "${appName}"
    BACKEND_FOLDER = "${WORKSPACE}/nuxeo-retention"
    BRANCH_LC = "${BRANCH_NAME.toLowerCase()}"
    CHANGE_BRANCH = "${env.CHANGE_BRANCH != null ? env.CHANGE_BRANCH : BRANCH_NAME}"
    CHANGE_TARGET = "${env.CHANGE_TARGET != null ? env.CHANGE_TARGET : BRANCH_NAME}"
    CHART_DIR = 'ci/helm/preview'
    CONNECT_PREPROD_URL = 'https://nos-preprod-connect.nuxeocloud.com/nuxeo'
    ENABLE_GITHUB_STATUS = 'true'
    FRONTEND_FOLDER = "${WORKSPACE}/nuxeo-retention-web"
    JENKINS_HOME = '/root'
    MAVEN_DEBUG = '-e'
    MAVEN_OPTS = "${MAVEN_OPTS} -Xms512m -Xmx3072m"
    NUXEO_BASE_IMAGE = 'docker-private.packages.nuxeo.com/nuxeo/nuxeo:11.4.42'
    ORG = 'nuxeo'
    PREVIEW_NAMESPACE = "retention-${BRANCH_LC}"
    REFERENCE_BRANCH = 'master'
    IS_REFERENCE_BRANCH = "${BRANCH_NAME == REFERENCE_BRANCH}"
    SLACK_CHANNEL = "${env.DRY_RUN == 'true' ? 'infra-napps' : 'napps-notifs'}"
  }
  stages {
    stage('Set Labels') {
      steps {
        container('maven') {
          script {
            nxNapps.setLabels()
          }
        }
      }
    }
    stage('Setup') {
      steps {
        container('maven') {
          script {
            nxNapps.setup()
            env.VERSION = nxNapps.getRCVersion()
          }
        }
      }
    }
    stage('Update Version') {
      steps {
        container('maven') {
          script {
            nxNapps.updateVersion("${VERSION}")
          }
        }
      }
    }
    stage('Compile') {
      steps {
        container('maven') {
          script {
            gitHubBuildStatus('compile')
            nxNapps.mavenCompile()
          }
        }
      }
      post {
        always {
          script {
            gitHubBuildStatus('compile')
          }
        }
      }
    }
    stage('Linting') {
      steps {
        container('maven') {
          script {
            gitHubBuildStatus('lint')
            nxNapps.lint("${FRONTEND_FOLDER}")
          }
        }
      }
      post {
        always {
          script {
            gitHubBuildStatus('lint')
          }
        }
      }
    }
    stage('Sonar') {
      environment {
        SONAR_PROJECT = 'nuxeo_nuxeo-retention'
        SONAR_BRANCH = "${BRANCH_LC}"
      }
      steps {
        container('maven') {
          script {
            try {
              if (nxNapps.isPullRequest()) {
                env.SONAR_OPTION = "-Dsonar.branch.target=${CHANGE_TARGET}"
              }
              nxSonar('sonar-retention', "${SONAR_PROJECT}", "${SONAR_BRANCH}", "${SONAR_OPTION}")
            } catch(err) {
              throw err
            } finally {
              archive '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*'
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/**/*.xml'
            }
          }
        }
      }
    }
    stage('Run Unit Tests') {
      steps {
        script {
          def stages = [:]
          stages['backend'] = runBackEndUnitTests()
          gitHubBuildStatus('utests/backend')
          parallel stages
        }
      }
      post {
        always {
          script {
            gitHubBuildStatus('utests/backend')
          }
        }
      }
    }
    stage('Package') {
      steps {
        container('maven') {
          script {
            gitHubBuildStatus('package')
            nxNapps.mavenPackage()
          }
        }
      }
      post {
        always {
          script {
            gitHubBuildStatus('package')
          }
        }
      }
    }
    stage('Build Docker Image') {
      steps {
        container('maven') {
          script {
            gitHubBuildStatus('docker/build')
            nxNapps.dockerBuild(
              "${WORKSPACE}/nuxeo-retention-package/target/nuxeo-retention-package-*.zip",
              "${WORKSPACE}/ci/docker","${WORKSPACE}/ci/docker/skaffold.yaml"
            )
          }
        }
      }
      post {
        always {
          script {
            gitHubBuildStatus('docker/build')
          }
        }
      }
    }
    stage('Buid Helm Chart') {
      steps {
        container('maven') {
          script {
            gitHubBuildStatus('helm/chart/build')
            nxKube.helmBuildChart("${CHART_DIR}", 'values.yaml')
          }
        }
      }
      post {
        always {
          script {
            gitHubBuildStatus('helm/chart/build')
          }
        }
      }
    }
    stage('Deploy Preview') {
      steps {
        container('maven') {
          script {
            nxKube.helmDeployPreview(
              "${PREVIEW_NAMESPACE}", "${CHART_DIR}", "${repositoryUrl}", "${IS_REFERENCE_BRANCH}"
            )
          }
        }
      }
    }
    stage('Run Functional Tests') {
      steps {
        container('maven') {
          script {
            gitHubBuildStatus('ftests')
            try {
              retry(3) {
                nxNapps.runFunctionalTests(
                  "${FRONTEND_FOLDER}", "--nuxeoUrl=http://preview.${PREVIEW_NAMESPACE}.svc.cluster.local/nuxeo"
                )
              }
            } catch(err) {
              throw err
            } finally {
              //retrieve preview logs
              nxKube.helmGetPreviewLogs("${PREVIEW_NAMESPACE}")
              cucumber (
                fileIncludePattern: '**/*.json',
                jsonReportDirectory: "${FRONTEND_FOLDER}/target/cucumber-reports/",
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
              try {
                if (nxNapps.needsPreviewCleanup() == 'true') {
                  nxKube.helmDeleteNamespace("${PREVIEW_NAMESPACE}")
                }
              } finally {
                gitHubBuildStatus('ftests')
              }
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
                nxNapps.gitCommit("${MESSAGE}", '-a')
                nxNapps.gitTag("${TAG}", "${MESSAGE}")
              }
            }
          }
        }
        stage('Package') {
          steps {
            container('maven') {
              script {
                gitHubBuildStatus('publish')
                echo """
                  --------------------------------------------------------------
                  Upload Retention Package ${VERSION} to ${CONNECT_PREPROD_URL}
                  --------------------------------------------------------------
                """
                String packageFile = "nuxeo-retention-package/target/nuxeo-retention-package-${VERSION}.zip"
                connectUploadPackage("${packageFile}", 'connect-preprod', "${CONNECT_PREPROD_URL}")
              }
            }
          }
          post {
            always {
              archiveArtifacts (
                allowEmptyArchive: true,
                artifacts: 'nuxeo-retention-package/target/nuxeo-retention-package-*.zip'
              )
              script {
                gitHubBuildStatus('publish')
              }
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
                nxNapps.gitPush("${TAG}")
              }
            }
          }
        }
      }
    }
  }
  post {
    always {
      script {
        if (!nxNapps.isPullRequest() && env.DRY_RUN != 'true') {
          // update JIRA issue
          step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
          currentBuild.description = "Build ${VERSION}"
        }
      }
    }
    success {
      script {
        // update Slack Channel
        String message = "${JOB_NAME} - #${BUILD_NUMBER} ${currentBuild.currentResult} (<${BUILD_URL}|Open>)"
        slackBuildStatus("${SLACK_CHANNEL}", "${message}", 'good')
      }
    }
    unsuccessful {
      script {
        // update Slack Channel
        String message = "${JOB_NAME} - #${BUILD_NUMBER} ${currentBuild.currentResult} (<${BUILD_URL}|Open>)"
        slackBuildStatus("${SLACK_CHANNEL}", "${message}", 'danger')
      }
    }
  }
}
