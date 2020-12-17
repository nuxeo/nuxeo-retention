/* groovylint-disable DuplicateStringLiteral */
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

def buildDockerImage () {
  echo """
    ----------------------------------------
    Build Docker image
    ----------------------------------------
    Image tag: ${VERSION}
  """
  sh """
      echo "Build and push Docker image to internal Docker registry ${DOCKER_REGISTRY}"
    cp -r ${WORKSPACE}/nuxeo-retention-package/target/nuxeo-retention-package-*.zip ${WORKSPACE}/ci/docker
  """
  skaffoldBuild("${WORKSPACE}/ci/docker/skaffold.yaml")
}

def cleanupChart(String charDir) {
  sh """
    cd ${charDir}
    git checkout requirements.yaml
  """
}

def cleanupPreview(String namespace) {
  try {
    this.scaleResource("${namespace}", 'deployment', "${PREVIEW_HELM_RELEASE}", '0')
    this.scaleResource("${namespace}", 'statefulset', 'postgresql-postgresql', '0')
  } catch (err) {
    echo hudson.Functions.printThrowable(err)
  } finally {
    this.helmUninstall('postgresql', "${namespace}")
    this.helmUninstall('nuxeo', "${namespace}")
    // clean up the preview namespace
    sh "kubectl delete namespace ${namespace} --ignore-not-found=true"
  }
}

def cleanupUnitTestsEnv(String env, String namespace) {
  boolean isDefaultEnv = env == 'default'
  String chartName = "${env}"
  try {
    if (this.namespaceExists("${namespace}")) {
      this.helmUninstall('redis', "${namespace}")
      if (!isDefaultEnv) {
        this.helmUninstall("${chartName}", "${namespace}")
      }
    }
  } catch (err) {
    echo hudson.Functions.printThrowable(err)
    echo "NUXEO::RETENTION Cannot cleanup ${env} ${namespace}"
  } finally {
    // clean up test namespace
    sh "kubectl delete namespace ${namespace} --ignore-not-found=true"
  }
}

def compile() {
  echo '''
    ----------------------------------------
    Compile
    ----------------------------------------
  '''
  sh "mvn ${MAVEN_ARGS} -V -T0.8C -DskipTests clean install"
}

def deployPreview(String namespace, String charDir, String isCleanupPreview, String gitRepo, String isReferenceBranch ) {
  echo """
    ----------------------------------------
    Deploy Preview environment in ${namespace}
    ----------------------------------------
  """
  this.helmSetup()
  dir(charDir) {
    try {
      boolean nsExists = this.namespaceExists(namespace)
      if (nsExists) {
        echo 'Scale down nuxeo preview deployment before release upgrade'
        // Previous preview deployment needs to be scaled to 0 to be replaced correctly
        this.scaleResource("${namespace}", 'deployment', "${PREVIEW_HELM_RELEASE}", '0')
        this.scaleResource("${namespace}", 'statefulset', 'postgresql-postgresql', '0')
      } else {
        sh "kubectl create namespace ${namespace}"
      }

      this.helmUpgradePostgreSQL("${namespace}", "${HELM_VALUES_DIR}/values-postgresql.yaml~gen")
      this.rolloutStatus('statefulset', 'postgresql-postgresql', '5m', "${namespace}")

      echo '''
        Upgrade nuxeo preview release
      '''

      sh """
        helm3 template nuxeo/nuxeo --version=~2.0.0 \
          --values=${HELM_VALUES_DIR}/values-nuxeo.yaml~gen --output-dir=target
      """
      this.helmUpgrade(
        "${PREVIEW_HELM_RELEASE}", 'nuxeo', 'nuxeo', '~2.0.0',
        "${namespace}", "${HELM_VALUES_DIR}/values-nuxeo.yaml~gen"
      )
      this.rolloutStatus('deployment', "${PREVIEW_HELM_RELEASE}", '11m', "${namespace}")

      previewHost = sh(returnStdout: true, script: """
        kubectl get ingress ${PREVIEW_HELM_RELEASE} \
          --namespace=${namespace} \
          -ojsonpath='{.spec.rules[*].host}'
      """)
      echo """
        -----------------------------------------------
        Preview available at: https://${previewHost}
        -----------------------------------------------
      """
      //The notification will only be printed if the PR has "preview" tag on GitHub
      if (isReferenceBranch == 'false' && isCleanupPreview == 'false') {
        String message = """
          :star: PR built and available in a preview environment **${namespace}** [here](https://${previewHost})
        """
        githubPRComment(comment: githubPRMessage("${message}"))
      }
    } catch (err) {
      if (isCleanupPreview == 'true') {
        this.cleanupPreview("${namespace}")
      }
      throw err
    }
  }
}

String getVersion() {
  return this.isPullRequest() ? this.getPullRequestVersion() : this.getReleaseVersion()
}

String getReleaseVersion() {
  String preid = 'rc'
  String nextPromotion = readMavenPom().getVersion().replace('-SNAPSHOT', '')
  String version = "${nextPromotion}-${preid}.0" // first version ever

  // find the latest tag if any
  sh "git fetch origin 'refs/tags/v${nextPromotion}*:refs/tags/v${nextPromotion}*'"
  String tag = sh(returnStdout: true, script: "git tag --sort=taggerdate --list 'v${nextPromotion}*' | tail -1 | tr -d '\n'")
  if (tag) {
    container('maven') {
      version = sh(returnStdout: true, script: "npx --ignore-existing semver -i prerelease --preid ${preid} ${tag} | tr -d '\n'")
    }
  }
  return version
}

String getPullRequestVersion() {
  return "${readMavenPom().getVersion()}-${BRANCH_NAME}"
}

def gitCheckout(String reference) {
  sh "git checkout ${reference}"
}

def gitCommit(String message, String option) {
  echo '''
    ----------------------------------------
    Git Commit
    ----------------------------------------
  '''
  sh """
    git commit ${option} -m "${message}"
  """
}

def gitPush(String reference) {
  sh "git push origin ${reference}"
}

def gitTag(String tagname, String message) {
  sh """
    git tag -a ${tagname} -m "${message}"
  """
}

def helmAddRepository(String name, String url) {
  sh "helm3 repo add ${name} ${url}"
}

def helmGenerateValues(String srcFolder,String usage) {
  sh """
    for valuesFile in ${srcFolder}/*.yaml; do
      USAGE=${usage} envsubst < \$valuesFile > \$valuesFile~gen
    done
  """
}

def helmUpgrade(String release, String repo, String chart, String version, String namespace, String values) {
  sh """
    helm3 upgrade ${release} ${repo}/${chart} \
      --install \
      --version=${version} \
      --namespace=${namespace} \
      --values=${values}
  """
}

def helmUpgradeMongoDB(String namespace, String values) {
  this.helmUpgrade('mongodb', 'bitnami', 'mongodb', '7.14.2', "${namespace}", "${values}")
}

def helmUpgradePostgreSQL(String namespace, String values) {
  this.helmUpgrade('postgresql', 'bitnami', 'postgresql', '9.8.4', "${namespace}", "${values}")
}

def helmUpgradeRedis(String namespace, String values) {
  helmUpgrade('redis', 'bitnami', 'redis', '11.2.1', "${namespace}", "${values}")
}

def helmSetup() {
  this.helmAddRepository('bitnami', 'https://charts.bitnami.com/bitnami')
  this.helmAddRepository('nuxeo', 'https://chartmuseum.platform.dev.nuxeo.com/')
}

def helmUninstall(String release, String namespace) {
  try {
    sh "helm3 uninstall ${release} --namespace=${namespace}"
  } catch (err) {
    echo hudson.Functions.printThrowable(err)
    echo "NUXEO::RETENTION Cannot uninstall ${release}"
  }
}

def setupUnitTestsEnv(String env, String namespace) {
  boolean isDefaultEnv = env == 'default'
  if (!isDefaultEnv) {
    String values = "${HELM_VALUES_DIR}/values-${env}.yaml~gen"
    if ("${env}" == 'mongodb') {
      this.helmUpgradeMongoDB("${namespace}", "${values}")
      this.rolloutStatus('deployment', "${env}", '5m', "${namespace}")
    } else if ( "${env}" == 'postgresql') {
      this.helmUpgradePostgreSQL("${namespace}", "${values}")
      this.rolloutStatus('statefulset', "${env}-${env}", '5m', "${namespace}")
    }
  }
  this.helmUpgradeRedis("${namespace}", "${HELM_VALUES_DIR}/values-redis.yaml~gen")
  this.rolloutStatus('statefulset', 'redis-master', '5m', "${namespace}")
}

def isPullRequest() {
  return "${BRANCH_NAME}" =~ /PR-.*/
}

def lint() {
  echo '''
    ----------------------------------------
    Run Linting Validations
    ----------------------------------------
  '''
  sh """
    cd ${FRONTEND_FOLDER}
    npm run lint
  """
}

String mavenArgs() {
  String args = '-DskipITs=true -fae -B -nsu'
  return args
}

boolean namespaceExists(String namespace) {
  return sh(returnStatus: true, script: "kubectl get namespace ${namespace}") == 0
}

boolean needsPreviewCleanup() {
  return this.isPullRequest() && !pullRequest.labels.contains('preview')
}

boolean needsSaucelabs() {
  return this.isPullRequest() && pullRequest.labels.contains('saucelabs')
}

def rolloutStatus(String kind, String name, String timeout, String namespace) {
  sh "kubectl rollout status ${kind} ${name} --timeout=${timeout} --namespace=${namespace}"
}

def runBackEndUnitTests(String env, String containerName, String gitRepo, String context, String message) {
  String namespace = "retention-utests-${BRANCH_NAME}-${env}-${BUILD_NUMBER}".toLowerCase()
  String reportPath = "target-${env}"
  boolean isDefaultEnv = env == 'default'
  String mavenOpt = '-Pqa'
  String redisHost = "redis-master.${namespace}.${TEST_SERVICE_DOMAIN_SUFFIX}"
  String testCore = env == 'mongodb' ? 'mongodb' : 'vcs'

  return {
    stage("Run ${env} unit tests") {
      container("${containerName}") {
        script {
          setGitHubBuildStatus("${context}", "${message}", 'PENDING', "${gitRepo}")
          try {
            sh "kubectl create namespace ${namespace}"
            if (!isDefaultEnv) {
              mavenOpt += ",customdb,${env} -Dalt.build.dir=${env}"
              // prepare test framework system properties
              sh """
                cat ci/mvn/nuxeo-test-${env}.properties \
                  > ci/mvn/nuxeo-test-${env}.properties~gen
                NAMESPACE=${namespace} DOMAIN=${TEST_SERVICE_DOMAIN_SUFFIX} \
                  envsubst < ci/mvn/nuxeo-test-${env}.properties~gen > ${JENKINS_HOME}/nuxeo-test-${env}.properties
              """
            } else {
              sh "touch ${JENKINS_HOME}/nuxeo-test-${env}.properties"
            }
            this.helmSetup()
            this.setupUnitTestsEnv("${env}", "${namespace}")
            retry(2) {
              sh """
                cd ${BACKEND_FOLDER}
                mvn ${MAVEN_ARGS} \
                  -Dcustom.environment=${env} \
                  -Dcustom.environment.log.dir=${reportPath} \
                  -Dnuxeo.test.core=${testCore} \
                  -Dnuxeo.test.redis.host=${redisHost} \
                  test
              """
            }
            setGitHubBuildStatus("${context}", "${message}", 'SUCCESS', "${gitRepo}")
          } catch (err) {
            setGitHubBuildStatus("${context}", "${message}", 'FAILURE', "${gitRepo}")
            throw err
          } finally {
            try {
              junit testResults: "**/${reportPath}/surefire-reports/*.xml"
            } finally {
              this.cleanupUnitTestsEnv("${env}", "${namespace}")
            }
          }
        }
      }
    }
  }
}

def runFunctionalTests(String ftestFolder, String namespace) {
  echo '''
    ----------------------------------------
    Run "default" functional tests
    ----------------------------------------
  '''
  sh """
    cd ${ftestFolder}
    npm run ftest -- --nuxeoUrl=http://preview.${namespace}.${TEST_SERVICE_DOMAIN_SUFFIX}/nuxeo
  """
}

def scaleResource(String namespace, String resource, String podName, String replicas) {
  sh "kubectl -n ${namespace} scale ${resource} ${podName} --replicas=${replicas}"
}

def setup() {
  sh '''
    # create the Git credentials
    jx step git credentials
    git config credential.helper store
  '''
  env.MAVEN_ARGS = this.mavenArgs()
}

def setSlackBuildStatus(String channel, String message, String color) {
  if ( env.DRY_RUN != 'true' ) {
    slackSend(channel: channel, color: color, message: message)
  }
}

def setGitHubBuildStatus(String context, String message, String state, String gitRepo) {
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

def setLabels() {
  echo '''
    ----------------------------------------
    Set Kubernetes resource labels
    ----------------------------------------
  '''
  echo "Set label 'branch: ${BRANCH_NAME}' on pod ${NODE_NAME}"
  sh "kubectl label pods ${NODE_NAME} branch=${BRANCH_NAME}"
  // output pod description
  echo "Describe pod ${NODE_NAME}"
  sh "kubectl describe pod ${NODE_NAME}"
}

def uploadPackage(String version, String credential,  String connectUrl) {
  withCredentials(
    [usernameColonPassword(credentialsId: "${credential}", variable: 'CONNECT_PASS')]
  ) {
    sh """
      PACKAGE="nuxeo-retention-package/target/nuxeo-retention-package-${version}.zip"
      curl -i -u "$CONNECT_PASS" -F package=@\$PACKAGE "$connectUrl"/site/marketplace/upload?batch=true
    """
  }
}

def updateVersion(String version) {
  echo """
    ----------------------------------------
    Update version
    ----------------------------------------
    New version: ${version}
  """
  sh """
    mvn ${MAVEN_ARGS} versions:set -DnewVersion=${version} -DgenerateBackupPoms=false
    cd ${FRONTEND_FOLDER}
    npm version ${version} --no-git-tag-version
  """
}

/**
 * Replaces environment variables present in the given yaml file and then runs skaffold build on it.
 * Needed environment variables are generally:
 * - DOCKER_REGISTRY
 * - VERSION
 */
def skaffoldBuild(String skaffoldFile) {
  sh """
    envsubst < ${skaffoldFile} > ${skaffoldFile}~gen
    skaffold build -f ${skaffoldFile}~gen
  """
}

return this;
