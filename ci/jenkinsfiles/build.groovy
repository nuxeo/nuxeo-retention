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
library identifier: 'nuxeo-napps-tools@improv-NXBT-3531-update-maven-workflow', retriever: modernSCM(
        [$class       : 'GitSCMSource',
         credentialsId: 'jx-pipeline-git-github',
         remote       : 'https://github.com/nuxeo/nuxeo-napps-tools.git'])

def appName = 'nuxeo-retention'
def configFile = 'ci/workflow.yaml'
def defaultContainer = 'maven'
def nxVersion = '2021.0'
def referenceBranch = 'lts-2021'
def podLabel = 'builder-maven-nuxeo-lts-2021'

buildMaven(appName, podLabel, defaultContainer, nxVersion, referenceBranch, configFile)
