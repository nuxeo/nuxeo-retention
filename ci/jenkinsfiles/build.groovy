/*
* (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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

void retrieveSharedLibrary(String outputFile, String version = 'master') {
  withCredentials([string(credentialsId: 'github_token', variable: 'GITHUB_TOKEN')]) {
    withEnv(["OUTPUT_FILENAME=${outputFile}", "VERSION=${version}"]) {
      sh '''
      curl --fail -v -H "Authorization: token $GITHUB_TOKEN" \
        -H 'Accept: application/vnd.github.v3.raw' \
        -o $OUTPUT_FILENAME \
        -L https://api.github.com/repos/nuxeo/nuxeo-napps-tools/contents/vars/commonLibraries.groovy?ref=$VERSION
      '''
    }
  }
}

pipeline {
  agent {
    label 'builder-maven-nuxeo-11'
  }
  environment {
    APP_NAME = "${appName}"
    BACKEND_FOLDER = "${WORKSPACE}/nuxeo-retention"
    BRANCH_LC = "${BRANCH_NAME.toLowerCase()}"
    CHANGE_BRANCH = "${env.CHANGE_BRANCH != null ? env.CHANGE_BRANCH : BRANCH_NAME}"
    CHANGE_TARGET = "${env.CHANGE_TARGET != null ? env.CHANGE_TARGET : BRANCH_NAME}"
    CHART_DIR = 'ci/helm/preview'
    CONNECT_PREPROD_URL = 'https://nos-preprod-connect.nuxeocloud.com/nuxeo'
    DEFAULT_CONTAINER = 'maven'
    ENABLE_GITHUB_STATUS = 'true'
    FRONTEND_FOLDER = "${WORKSPACE}/nuxeo-retention-web"
    JENKINS_HOME = '/root'
    MAVEN_OPTS = "${MAVEN_OPTS} -Xms512m -Xmx3072m"
    NUXEO_VERSION = '11.4.42'
    NUXEO_BASE_IMAGE = 'docker-private.packages.nuxeo.com/nuxeo/nuxeo:11.4.42'
    ORG = 'nuxeo'
    PREVIEW_NAMESPACE = "retention-${BRANCH_LC}"
    REFERENCE_BRANCH = 'master'
    IS_REFERENCE_BRANCH = "${BRANCH_NAME == REFERENCE_BRANCH}"
    SHARED_LIB_FILENAME = 'ci/jenkinsfiles/common-lib.groovy'
    SLACK_CHANNEL = "${env.DRY_RUN == 'true' ? 'infra-napps' : 'pr-napps'}"
  }
  stages {
    stage('Load Common Library') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            retrieveSharedLibrary("${SHARED_LIB_FILENAME}", 'task-NXBT-3447-napps-shared-libs')
            pipelineLib = load "${SHARED_LIB_FILENAME}"
          }
        }
      }
    }
    stage('Set Labels') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            pipelineLib.setLabels()
          }
        }
      }
    }
    stage('Setup') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            pipelineLib.setup()
            env.VERSION = pipelineLib.getVersion()
            sh 'env'
          }
        }
      }
    }
    stage('Update Version') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            pipelineLib.updateVersion("${VERSION}")
          }
        }
      }
    }
    stage('Compile') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            pipelineLib.setGitHubBuildStatus('retention/compile', "${repositoryUrl}")
            pipelineLib.runMavenStep('Compile', '-V -T0.8C -DskipTests clean install')
          }
        }
      }
      post {
        always {
          script {
            pipelineLib.setGitHubBuildStatus('retention/compile', "${repositoryUrl}")
          }
        }
      }
    }
    stage('Linting') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            pipelineLib.setGitHubBuildStatus('retention/lint', "${repositoryUrl}")
            pipelineLib.lint("${FRONTEND_FOLDER}")
          }
        }
      }
      post {
        always {
          script {
            pipelineLib.setGitHubBuildStatus('retention/lint', "${repositoryUrl}")
          }
        }
      }
    }
    stage('Run Unit Tests') {
      steps {
        script {
          def stages = [:]
          stages['backend'] = pipelineLib.runBackEndUnitTests("${BACKEND_FOLDER}")
          pipelineLib.setGitHubBuildStatus('retention/utests', "${repositoryUrl}")
          parallel stages
        }
      }
      post {
        always {
          script {
            pipelineLib.setGitHubBuildStatus('retention/utests', "${repositoryUrl}")
          }
        }
      }
    }
    stage('Build Docker Image') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            pipelineLib.setGitHubBuildStatus('retention/docker/build', "${repositoryUrl}")
            pipelineLib.dockerBuild(
              "${WORKSPACE}/nuxeo-retention-package/target/nuxeo-retention-package-*.zip",
              "${WORKSPACE}/ci/docker", "${WORKSPACE}/ci/docker/skaffold.yaml"
            )
          }
        }
      }
      post {
        always {
          script {
            pipelineLib.setGitHubBuildStatus('retention/docker/build', "${repositoryUrl}")
          }
        }
      }
    }
    stage('Buid Helm Chart') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            pipelineLib.setGitHubBuildStatus('retention/helm/chart', "${repositoryUrl}")
            pipelineLib.helmBuild("${CHART_DIR}")
          }
        }
      }
      post {
        always {
          script {
            pipelineLib.setGitHubBuildStatus('retention/helm/chart', "${repositoryUrl}")
          }
        }
      }
    }
    stage('Deploy Retention Preview') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            env.CLEANUP_PREVIEW = pipelineLib.needsPreviewCleanup()
            pipelineLib.deployPreview(
              "${PREVIEW_NAMESPACE}", "${CHART_DIR}", "${CLEANUP_PREVIEW}", "${repositoryUrl}", "${IS_REFERENCE_BRANCH}"
            )
          }
        }
      }
    }
    stage('Run Functional Tests') {
      steps {
        container(DEFAULT_CONTAINER) {
          script {
            pipelineLib.setGitHubBuildStatus('retention/ftests', "${repositoryUrl}")
            try {
              def ftestOption = "--nuxeoUrl=http://preview.${PREVIEW_NAMESPACE}.svc.cluster.local/nuxeo"
              retry(3) {
                pipelineLib.runFunctionalTests("${FRONTEND_FOLDER}", "${ftestOption}")
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
          container(DEFAULT_CONTAINER) {
            script {
              //cleanup the preview
              if (env.CLEANUP_PREVIEW == 'true') {
                pipelineLib.cleanupPreview("${PREVIEW_NAMESPACE}")
              }
              pipelineLib.setGitHubBuildStatus('retention/ftests', "${repositoryUrl}")
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
            container(DEFAULT_CONTAINER) {
              script {
                pipelineLib.gitCommit("${MESSAGE}", '-a')
                pipelineLib.gitTag("${TAG}", "${MESSAGE}")
              }
            }
          }
        }
        stage('Publish Retention Package') {
          steps {
            container(DEFAULT_CONTAINER) {
              script {
                pipelineLib.setGitHubBuildStatus('retention/publish/package', "${repositoryUrl}")
                echo """
                  -------------------------------------------------
                  Upload Retention Package ${VERSION} to ${CONNECT_PREPROD_URL}
                  -------------------------------------------------
                """
                def packageFile = "nuxeo-retention-package/target/nuxeo-retention-package-${VERSION}.zip"
                pipelineLib.uploadPackage("${packageFile}", 'connect-preprod', "${CONNECT_PREPROD_URL}")
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
                pipelineLib.setGitHubBuildStatus('retention/publish/package', "${repositoryUrl}")
              }
            }
          }
        }
        stage('Git Push') {
          steps {
            container(DEFAULT_CONTAINER) {
              echo """
                --------------------------
                Git push ${TAG}
                --------------------------
              """
              script {
                pipelineLib.gitPush("${TAG}")
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
        if (!pipelineLib.isPullRequest() && env.DRY_RUN != 'true') {
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
        pipelineLib.setSlackBuildStatus("${SLACK_CHANNEL}", "${message}", 'good')
      }
    }
    unsuccessful {
      script {
        // update Slack Channel
        String message = "${JOB_NAME} - #${BUILD_NUMBER} ${currentBuild.currentResult} (<${BUILD_URL}|Open>)"
        pipelineLib.setSlackBuildStatus("${SLACK_CHANNEL}", "${message}", 'danger')
      }
    }
  }
}
