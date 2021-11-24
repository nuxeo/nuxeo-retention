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
library identifier: 'nuxeo-napps-tools@0.0.14', retriever: modernSCM(
        [$class       : 'GitSCMSource',
         credentialsId: 'jx-pipeline-git-github',
         remote       : 'https://github.com/nuxeo/nuxeo-napps-tools.git'])

def appName = 'nuxeo-retention'
def containerLabel = 'maven'
def mcontainers = [
  'dev': 'maven',
  'mongodb': 'maven-mongodb',
  'pgsql': 'maven-pgsql',
]
def targetTestEnvs = ['dev', 'mongodb', 'pgsql',]

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
    NUXEO_VERSION = '2021.10'
    NUXEO_BASE_IMAGE = "docker-private.packages.nuxeo.com/nuxeo/nuxeo:${NUXEO_VERSION}"
    ORG = 'nuxeo'
    PREVIEW_NAMESPACE = "retention-${BRANCH_LC}"
    REFERENCE_BRANCH = 'lts-2021'
    IS_REFERENCE_BRANCH = "${BRANCH_NAME == REFERENCE_BRANCH}"
    SLACK_CHANNEL = "${env.DRY_RUN == 'true' ? 'infra-napps' : 'napps-notifs'}"
    SKAFFOLD_VERSION = 'v1.26.1'
    UNIT_TEST_NAMESPACE_SUFFIX = "${APP_NAME}-${BRANCH_LC}".toLowerCase()
  }
  stages {
    stage('Set Labels') {
      steps {
        container(containerLabel) {
          script {
            nxNapps.setLabels()
          }
        }
      }
    }
    stage('Setup') {
      steps {
        container(containerLabel) {
          script {
            nxNapps.setup()
            env.VERSION = nxNapps.getRCVersion()
          }
        }
      }
    }
    stage('Summary') {
      steps {
        script {
          println("Test environments: ${targetTestEnvs.join(' ')}")
          println("Maven Args: ${MAVEN_ARGS}")
        }
      }
    }
    stage('Update Version') {
      steps {
        container(containerLabel) {
          script {
            nxNapps.updateVersion("${VERSION}")
          }
        }
      }
    }
    stage('Compile') {
      steps {
        container(containerLabel) {
          script {
            gitHubBuildStatus('compile')
            nxNapps.mavenCompile()
          }
        }
      }
      post {
        always {
          gitHubBuildStatus('compile')
        }
      }
    }
    stage('Linting') {
      steps {
        container(containerLabel) {
          script {
            gitHubBuildStatus('lint')
            nxNapps.lint("${FRONTEND_FOLDER}")
          }
        }
      }
      post {
        always {
          gitHubBuildStatus('lint')
        }
      }
    }
    stage('Run runtime unit tests') {
      steps {
        container(containerLabel) {
          script {
            def stages = [:]
            def envVars = ["TEST=TEST"]
            stages['backend'] = nxNapps.runBackendUnitTests(envVars)
            gitHubBuildStatus('utests/backend')
            parallel stages
          }
        }
      }
      post {
        always {
          gitHubBuildStatus('utests/backend')
        }
      }
    }
    stage('Deploy Env') {
      steps {
        script {
          def stages = [:]
          targetTestEnvs.each { env ->
            String containerName = mcontainers["${env}"]
            stages["Deploy - ${env}"] =
              nxKube.helmDeployUnitTestEnvStage(
                "${containerName}", "${env}", "${UNIT_TEST_NAMESPACE_SUFFIX}-${env}", 'ci/helm/utests'
              )
          }
          parallel stages
        }
      }
    }
    stage('Run unit tests') {
      steps {
        script {
          gitHubBuildStatus('utests')
          def stages = [:]
          def parameters = [:]
          targetTestEnvs.each { env ->
            String containerName = mcontainers["${env}"]
            String namespace  = "${UNIT_TEST_NAMESPACE_SUFFIX}-${env}"
            stages["Run ${env} unit tests"] = nxNapps.runUnitTestStage("${containerName}", "${env}", "${WORKSPACE}", "${namespace}", ["TEST=TEST"])
          }
          parallel stages
        }
      }
      post {
        always {
          script {
            try {
              targetTestEnvs.each { env ->
                container('maven') {
                  nxKube.helmDestroyUnitTestsEnv(
                    "${UNIT_TEST_NAMESPACE_SUFFIX}-${env}"
                  )
                }
              }
            } finally {
              gitHubBuildStatus('utests')
            }
          }
        }
      }
    }
    stage('Package') {
      steps {
        container(containerLabel) {
          script {
            gitHubBuildStatus('package')
            nxNapps.mavenPackage()
          }
        }
      }
      post {
        always {
          gitHubBuildStatus('package')
        }
      }
    }
    stage('Build Docker Image') {
      steps {
        container(containerLabel) {
          script {
            gitHubBuildStatus('docker/build')
            nxNapps.setupKaniko("${SKAFFOLD_VERSION}")
            nxNapps.dockerBuild(
              "${WORKSPACE}/nuxeo-retention-package/target/nuxeo-retention-package-*.zip",
              "${WORKSPACE}/ci/docker","${WORKSPACE}/ci/docker/skaffold.yaml"
            )
          }
        }
      }
      post {
        always {
          gitHubBuildStatus('docker/build')
        }
      }
    }
    stage('Buid Helm Chart') {
      steps {
        container(containerLabel) {
          script {
            gitHubBuildStatus('helm/chart/build')
            nxKube.helmBuildChart("${CHART_DIR}", 'values.yaml')
            nxNapps.gitCheckout("${CHART_DIR}/requirements.yaml")
          }
        }
      }
      post {
        always {
          gitHubBuildStatus('helm/chart/build')
        }
      }
    }
    stage('Deploy Preview') {
      steps {
        container(containerLabel) {
          script {
            gitHubBuildStatus('helm/chart/deploy')
            nxKube.helmDeployPreview(
              "${PREVIEW_NAMESPACE}", "${CHART_DIR}", "${GIT_URL}", "${IS_REFERENCE_BRANCH}"
            )
          }
        }
      }
      post {
        always {
          gitHubBuildStatus('helm/chart/deploy')
        }
      }
    }
    stage('Run Functional Tests') {
      steps {
        container(containerLabel) {
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
          container(containerLabel) {
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
            container(containerLabel) {
              script {
                nxNapps.gitCommit("${MESSAGE}", '-a')
                nxNapps.gitTag("${TAG}", "${MESSAGE}")
              }
            }
          }
        }
        stage('Package') {
          steps {
            container(containerLabel) {
              script {
                gitHubBuildStatus('publish/package')
                                echo """
                  -------------------------------------------------------------
                  Upload Retention Package ${VERSION} to ${CONNECT_PREPROD_URL}
                  -------------------------------------------------------------
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
              gitHubBuildStatus('publish/package')
            }
          }
        }
        stage('Git Push') {
          steps {
            container(containerLabel) {
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
