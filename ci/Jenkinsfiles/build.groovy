/*
* (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
*     Kevin Leturc <kevin.leturc@hyland.com>
*/
library identifier: "platform-ci-shared-library@v0.0.25"

Closure buildUnitTestStage(env) {
  return {
    container('maven') {
      nxWithGitHubStatus(context: "utests/backend/${env}") {
        script {
          def testNamespace = "${CURRENT_NAMESPACE}-retention-${BRANCH_NAME}-${BUILD_NUMBER}-${env}".replaceAll('\\.', '-').toLowerCase()
          nxWithHelmfileDeployment(namespace: testNamespace, environment: "${env}UnitTests") {
            try {
              sh """
                cat ci/mvn/nuxeo-test-${env}.properties \
                | envsubst > /root/nuxeo-test-${env}.properties
              """
              retry(3) {
                sh """
                  mvn -B -nsu -pl :nuxeo-retention \
                    -Dcustom.environment=${env} \
                    -Dcustom.environment.log.dir=target-${env} \
                    -Dnuxeo.test.core=${env == 'mongodb' ? 'mongodb' : 'vcs'} \
                    test
                """
              }
            } finally {
              archiveArtifacts artifacts: "**/target-${env}/**/*.log"
              junit allowEmptyResults: true, testResults: "**/target-${env}/surefire-reports/*.xml"
            }
          }
        }
      }
    }
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-package-lts-2023-node-18'
  }
  options {
    buildDiscarder(logRotator(daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5'))
    disableConcurrentBuilds(abortPrevious: true)
    githubProjectProperty(projectUrlStr: 'https://github.com/nuxeo/nuxeo-retention')
  }
  environment {
    CURRENT_NAMESPACE = nxK8s.getCurrentNamespace()
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    VERSION = nxUtils.getVersion()
    NUXEO_RETENTION_PACKAGE_PATH = "nuxeo-retention-package/target/nuxeo-retention-package-${VERSION}.zip"
  }
  stages {
    stage('Set labels') {
      steps {
        container('maven') {
          script {
            nxK8s.setPodLabels()
          }
        }
      }
    }
    stage('Update version') {
      steps {
        container('maven') {
          script {
            nxMvn.updateVersion()
          }
        }
      }
    }
    stage('Compile') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'compile') {
            echo """
            ----------------------------------------
            Compile
            ----------------------------------------"""
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh 'mvn -B -nsu -T4C install -DskipTests'
          }
        }
      }
      post {
        always {
          archiveArtifacts artifacts: '**/target/*.jar, **/target/nuxeo-*-package-*.zip'
          junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
        }
      }
    }
    stage('Run functional tests') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'docker/build') {
            script {
              sh "mkdir -p ci/docker/target && cp ${NUXEO_RETENTION_PACKAGE_PATH} ci/docker/target"
              def nuxeoVersion = sh(returnStdout: true,
                  script: 'mvn org.apache.maven.plugins:maven-help-plugin:3.3.0:evaluate -Dexpression=nuxeo.platform.version -q -DforceStdout')
              nxDocker.build(skaffoldFile: 'ci/docker/skaffold.yaml', envVars: ["NUXEO_VERSION=${nuxeoVersion}"])
            }
          }
          nxWithGitHubStatus(context: 'ftests') {
            script {
              def testNamespace = "${CURRENT_NAMESPACE}-retention-${BRANCH_NAME}-${BUILD_NUMBER}-ftests".replaceAll('\\.', '-').toLowerCase()
              def nuxeoParentVersion = readMavenPom().getParent().getVersion()
              // target connect preprod if nuxeo-parent is a snapshot version or a build version
              def clidSecret = nuxeoParentVersion.matches("^\\d+\\.\\d+(-SNAPSHOT|\\.\\d+)\$") ? 'instance-clid-preprod' : 'instance-clid'
              nxWithHelmfileDeployment(namespace: testNamespace, environment: "functionalTests", envVars: ["CONNECT_CLID_SECRET=${clidSecret}"],
                  secrets: [[name: clidSecret, namespace: 'platform']]) {
                dir('nuxeo-retention-web') {
                  // do retry ftests as a test assert a number of documents and those are not cleaned at teardown
                  sh "npm run ftest -- --nuxeoUrl=http://nuxeo.${NAMESPACE}.svc.cluster.local/nuxeo"
                }
              }
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts artifacts: 'nuxeo-retention-web/ftest/target/screenshots/**', allowEmptyArchive: true
          cucumber(fileIncludePattern: '**/*.json', jsonReportDirectory: 'nuxeo-retention-web/ftest/target/cucumber-reports/',
              sortingMethod: 'NATURAL')
        }
      }
    }
    stage('Git commit, tag and push') {
      when {
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          script {
            echo """
            ----------------------------------------
            Git commit, tag and push
            ----------------------------------------
            """
            nxGit.commitTagPush()
          }
        }
      }
    }
    stage('Deploy Maven artifacts') {
      when {
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'maven/deploy', message: 'Deploy Maven artifacts') {
            script {
              echo """
              ----------------------------------------
              Deploy Maven artifacts
              ----------------------------------------"""
              nxMvn.deploy()
            }
          }
        }
      }
    }
    stage('Deploy Nuxeo package') {
      when {
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'package/deploy', message: 'Deploy Nuxeo packages') {
            script {
              echo """
              ----------------------------------------
              Upload Nuxeo Package to ${CONNECT_PREPROD_SITE_URL}
              ----------------------------------------"""
              nxUtils.postForm(credentialsId: 'connect-preprod', url: "${CONNECT_PREPROD_SITE_URL}marketplace/upload?batch=true",
                  form: ["package=@${NUXEO_RETENTION_PACKAGE_PATH}"])
            }
          }
        }
      }
    }
  }
  post {
    always {
      script {
        currentBuild.description = "Build ${VERSION}"
        nxJira.updateIssues()
      }
    }
  }
}

