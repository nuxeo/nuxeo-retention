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

void setGitHubBuildStatus(String context, String message, String state, String gitRepo) {
  if ( env.DRY_RUN != 'true' && ENABLE_GITHUB_STATUS == 'true') {
    step([
      $class: 'GitHubCommitStatusSetter',
      reposSource: [$class: 'ManuallyEnteredRepositorySource', url: gitRepo],
      contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
      statusResultSource: [
        $class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]
      ],
    ])
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
    BUCKET_PREFIX = "${appName}-${BRANCH_LC}-${BUILD_NUMBER}"
    CHANGE_BRANCH = "${env.CHANGE_BRANCH != null ? env.CHANGE_BRANCH : BRANCH_NAME}"
    CHANGE_TARGET = "${env.CHANGE_TARGET != null ? env.CHANGE_TARGET : BRANCH_NAME}"
    CHART_DIR = 'ci/helm/preview'
    CONNECT_PREPROD_URL = 'https://nos-preprod-connect.nuxeocloud.com/nuxeo'
    ENABLE_GITHUB_STATUS = 'true'
    FRONTEND_FOLDER = "${WORKSPACE}/nuxeo-retention-web"
    JENKINS_HOME = '/root'
    MAVEN_DEBUG = '-e'
    MAVEN_OPTS = "${MAVEN_OPTS} -Xms512m -Xmx3072m"
    NUXEO_VERSION = '2021.0'
    NUXEO_BASE_IMAGE = "docker-private.packages.nuxeo.com/nuxeo/nuxeo:${NUXEO_VERSION}"
    ORG = 'nuxeo'
    PREVIEW_NAMESPACE = "retention-${BRANCH_LC}"
    REFERENCE_BRANCH = 'master'
    IS_REFERENCE_BRANCH = "${BRANCH_NAME == REFERENCE_BRANCH}"
  }
  stages {
    stage('Load Common Library') {
      steps {
        container('maven') {
          script {
            pipelineLib = load 'ci/jenkinsfiles/common-lib.groovy'
            if (env.DRY_RUN == 'true') {
              env.SLACK_CHANNEL = 'infra-napps'
            } else {
              env.SLACK_CHANNEL = 'pr-napps'
            }
          }
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
            env.VERSION = pipelineLib.getVersion()
            sh 'env'
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
        setGitHubBuildStatus('retention/compile', 'Compile', 'PENDING', "${repositoryUrl}")
        container('maven') {
          script {
            pipelineLib.compile()
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('retention/compile', 'Compile', 'SUCCESS', "${repositoryUrl}")
        }
        unsuccessful {
          setGitHubBuildStatus('retention/compile', 'Compile', 'FAILURE', "${repositoryUrl}")
        }
      }
    }
    stage('Linting') {
      steps {
        setGitHubBuildStatus('retention/lint', 'Run Linting Validations', 'PENDING', "${repositoryUrl}")
        container('maven') {
          script {
            pipelineLib.lint()
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('retention/lint', 'Run Linting Validations', 'SUCCESS', "${repositoryUrl}")
        }
        unsuccessful {
          setGitHubBuildStatus('retention/lint', 'Run Linting Validations', 'FAILURE', "${repositoryUrl}")
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
        setGitHubBuildStatus('retention/docker/build', 'Build Docker Image', 'PENDING', "${repositoryUrl}")
        container('maven') {
          script {
            pipelineLib.buildDockerImage()
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('retention/docker/build', 'Build Docker Image', 'SUCCESS', "${repositoryUrl}")
        }
        unsuccessful {
          setGitHubBuildStatus('retention/docker/build', 'Build Docker Image', 'FAILURE', "${repositoryUrl}")
        }
      }
    }
    stage('Buid Helm Chart') {
      steps {
        setGitHubBuildStatus('retention/helm/chart', 'Build Helm Chart', 'PENDING', "${repositoryUrl}")
        container('maven') {
          script {
            def retentionParams = pipelineLib.getRetentionMode().split(',')
            env.RETENTION_MODE = retentionParams[0]
            env.COMPLIANCE_MODE_ENABLED = retentionParams[1]
            pipelineLib.buildHelmChart("${CHART_DIR}")
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('retention/helm/chart', 'Build Helm Chart', 'SUCCESS', "${repositoryUrl}")
        }
        unsuccessful {
          setGitHubBuildStatus('retention/helm/chart', 'Build Helm Chart', 'FAILURE', "${repositoryUrl}")
        }
      }
    }
    stage('Deploy Retention Preview') {
      steps {
        container('maven') {
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
        setGitHubBuildStatus('retention/ftests', 'Functional tests - default environment', 'PENDING', "${repositoryUrl}")
        container('maven') {
          script {
            try {
              echo 'Functional Tests disabled'
              retry(3) {
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
              if (env.CLEANUP_PREVIEW == 'true') {
                pipelineLib.cleanupPreview("${PREVIEW_NAMESPACE}")
              }
            }
          }
        }
        success {
          setGitHubBuildStatus('retention/ftests', 'Functional tests - default environment', 'SUCCESS', "${repositoryUrl}")
        }
        unsuccessful {
          setGitHubBuildStatus('retention/ftests', 'Functional tests - default environment', 'FAILURE', "${repositoryUrl}")
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
            setGitHubBuildStatus('retention/publish/package', 'Upload Retention Package', 'PENDING', "${repositoryUrl}")
            container('maven') {
              script {
                echo """
                  -------------------------------------------------
                  Upload Retention Package ${VERSION} to ${CONNECT_PREPROD_URL}
                  -------------------------------------------------
                """
                pipelineLib.uploadPackage("${VERSION}", 'connect-preprod', "${CONNECT_PREPROD_URL}")
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
            success {
              setGitHubBuildStatus('retention/publish/package', 'Upload Retention Package', 'SUCCESS', "${repositoryUrl}")
            }
            unsuccessful {
              setGitHubBuildStatus('retention/publish/package', 'Upload Retention Package', 'FAILURE', "${repositoryUrl}")
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
