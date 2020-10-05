podTemplate(
    containers: [
        containerTemplate(name: 'maven', alwaysPullImage: true, image: 'nexus.sling-dev.com:8023/sling/jenkins/maven:3.6.3-jdk-11-openj9', args: 'cat', ttyEnabled: true),
        containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true)
    ],
    volumes: [
        hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
    ],
    imagePullSecrets: ['registry-credentials']) {
  node(POD_LABEL) {
    def gitBranch
    def gitCommit
    def targetBranch
    def dockerRegistry = 'nexus.sling-dev.com:8123'
    def webhookURL = "https://outlook.office.com/webhook/14bd0fd8-1bd7-43dd-bdf5-2f0e89c4ec50@46ecb5f0-1227-4755-a101-64d39a05e1c7/JenkinsCI/752745c8a19a45caae6f2bc8a2a9df07/ad82656b-8630-4724-81d2-b76ce5380247"
    def message = "Starting Build (${env.BUILD_ID}) for '${env.JOB_NAME} (${env.BRANCH_NAME})'"
    def status = "Starting"
    def color = "a0a0a0"
    try {
      office365ConnectorSend message: message, status: status, webhookUrl: webhookURL, color: color
      container('maven') {
        stage('Git Checkout') {
          def gitRepo = checkout scm
          gitCommit = gitRepo.GIT_COMMIT
          gitBranch = gitRepo.GIT_BRANCH
          targetBranch = env.CHANGE_TARGET
          sh "echo $gitCommit"
          sh "echo $gitBranch"
          sh "echo $env.CHANGE_TARGET"
        }
        stage('Build') {
          sh script: "mvn clean compile -DskipTests", label: "mvn compile"
        }
        stage('Test') {
          sh 'mvn test'
        }
        stage('Integration Test') {
          sh 'mvn clean verify -P integration-test'
        }
        stage('Package Jar') {
          sh 'mvn clean package -Dskip.unit.tests=true'
        }
      }
      container('docker') {
        def imageName = "${dockerRegistry}/sling/sd-workflow"
        def flywayImageName = "${dockerRegistry}/sling/sd-workflow/db-migration"
        def gitTag = "${imageName}:${gitCommit}"
        def flywayGitTag = "${flywayImageName}:${gitCommit}"
        def pullRequestTag = "${imageName}:${gitBranch}"
        def flywayPullRequestTag = "${flywayImageName}:${gitBranch}"
        def latestTag = "${imageName}:latest"
        def flywayLatestTag = "${flywayImageName}:latest"

        if (gitBranch.startsWith('PR-') && targetBranch == 'master') {
          pullRequestTag = "${imageName}:RC-${env.BUILD_ID}"
          flywayPullRequestTag = "${flywayImageName}:RC-${env.BUILD_ID}"
        }
        withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                          credentialsId   : 'docker-sling-registry',
                          usernameVariable: 'DOCKER_USER',
                          passwordVariable: 'DOCKER_PASSWORD']]) {
          stage('Prepare flyway migrations') {
            sh script: "docker login ${dockerRegistry} -u ${DOCKER_USER} -p ${DOCKER_PASSWORD}", label: "Docker Login"
            if (gitBranch.startsWith('PR-')) {
              sh script: "docker build -f ./flyway-Dockerfile -t ${flywayPullRequestTag} .", label: "Build flyway migrations image with Pull Request tag"
              sh script: "docker push ${flywayPullRequestTag}", label: "Push to registry"
            }
            if (gitBranch == 'dev') {
              sh script: "docker build -f ./flyway-Dockerfile -t ${flywayGitTag} .", label: "Build flyway migrations image with git commit hash"

              sh script: "docker push ${flywayGitTag}", label: "Push to registry"
            }
            if (gitBranch == 'master') {
              def flywayReleaseCandidateTag = "${flywayImageName}:Release-${env.BUILD_ID}"
              sh script: "docker build -f ./flyway-Dockerfile -t ${flywayGitTag} .", label: "Build flyway migrations image with git commit hash"
              sh script: "docker tag ${flywayGitTag} ${flywayReleaseCandidateTag}", label: "Tag image as release candidate"
              sh script: "docker tag ${flywayGitTag} ${flywayLatestTag}", label: "Tag image as latest candidate"

              sh script: "docker push ${flywayGitTag}", label: "Push to registry"
              sh script: "docker push ${flywayReleaseCandidateTag}", label: "Push release candidate image to registry"
              sh script: "docker push ${flywayLatestTag}", label: "Push latest image to registry"
            }
          }
          stage('Publish Docker Image') {
            if (gitBranch.startsWith('PR-')) {
              sh script: "docker build -t ${pullRequestTag} .", label: "Build image with Pull Request tag"
              sh script: "docker push ${pullRequestTag}", label: "Push to registry"
            }
            if (gitBranch == 'dev') {
              sh script: "docker build -t ${gitTag} .", label: "Build image with git commit hash"

              sh script: "docker push ${gitTag}", label: "Push to registry"
            }
            if (gitBranch == 'master') {
              def releaseCandidateTag = "${imageName}:Release-${env.BUILD_ID}"
              sh script: "docker build -t ${gitTag} .", label: "Build image with git commit hash"
              sh script: "docker tag ${gitTag} ${releaseCandidateTag}", label: "Tag image as release candidate"
              sh script: "docker tag ${gitTag} ${latestTag}", label: "Tag image as latest candidate"

              sh script: "docker push ${gitTag}", label: "Push to registry"
              sh script: "docker push ${releaseCandidateTag}", label: "Push release candidate image to registry"
              sh script: "docker push ${latestTag}", label: "Push latest image to registry"
            }
          }
        }
      }
      if (gitBranch.startsWith('PR-')) {
        try {
          if (targetBranch == 'master') {
            stage('Approval for Deployment') {
              userInput = input(id: 'confirm', message: 'Do you wish to deploy the PR to STAGE environment?',
                  parameters: [[$class: 'BooleanParameterDefinition', defaultValue: false, description: 'This will deploy the current PR in STAGE environment', name: 'confirm']])
            }
            stage('Start Deployments') {
              build job: '../deploy-to-stage',
                  parameters: [[$class: 'StringParameterValue', name: 'dockerImageTag', value: "RC-${env.BUILD_ID}"],
                               [$class: 'StringParameterValue', name: 'branchName', value: gitBranch],
                               [$class: 'StringParameterValue', name: 'targetBranch', value: targetBranch]],
                  wait: false
            }
          }else {
            stage('Approval for Deployment') {
              userInput = input(id: 'confirm', message: 'Do you wish to deploy the PR to QA environment?',
                  parameters: [[$class: 'BooleanParameterDefinition', defaultValue: false, description: 'This will deploy the current PR in QA environment', name: 'confirm']])
            }
            stage('Start Deployments') {
              build job: '../deploy-to-qa',
                  parameters: [[$class: 'StringParameterValue', name: 'dockerImageTag', value: gitBranch],
                               [$class: 'StringParameterValue', name: 'branchName', value: gitBranch],
                               [$class: 'StringParameterValue', name: 'targetBranch', value: targetBranch]],
                  wait: false
            }
          }
        } catch (err) {
          def user = err.getCauses()[0].getUser()
          userInput = false
          echo "Aborted by: [${user}]"
        }
      }
      if (gitBranch == 'dev') {
        stage('Start Deployments') {
          build job: '../deploy-to-qa',
              parameters: [[$class: 'StringParameterValue', name: 'dockerImageTag', value: gitCommit],
                           [$class: 'StringParameterValue', name: 'branchName', value: gitBranch]],
              wait: false
        }
      }
      if (gitBranch == 'master') {
        stage('Start Stage Deployment') {
          build job: '../deploy-to-stage',
              parameters: [[$class: 'StringParameterValue', name: 'dockerImageTag', value: "Release-${env.BUILD_ID}"],
                           [$class: 'StringParameterValue', name: 'triggeredByJob', value: "build-and-package-master : #${BUILD_NUMBER}"],
                           [$class: 'StringParameterValue', name: 'branchName', value: "master"]],
              wait: false
        }
      }
      if (currentBuild.currentResult == "SUCCESS") {
        status = "SUCCESS"
        color = "0bcc2c"
        message = "Build (${env.BUILD_ID}) for '${env.JOB_NAME} (${env.BRANCH_NAME})' is Successful. Great job!"
      } else if (currentBuild.currentResult == "UNSTABLE") {
        status = "UNSTABLE"
        color = "d00000"
        message = "Build (${env.BUILD_ID}) for '${env.JOB_NAME} (${env.BRANCH_NAME})' is UNSTABLE!"
      } else if (currentBuild.currentResult == "FAILURE") {
        status = "FAILURE"
        color = "d00000"
        message = "Build (${env.BUILD_ID}) for '${env.JOB_NAME} ({$BRANCH_NAME})' Failed!"
      }
      office365ConnectorSend message: message, status: status, webhookUrl: webhookURL, color: color
    }
    catch(exc) {
      office365ConnectorSend message: "Exception while Building", status: "FAILURE", webhookUrl: webhookURL, color: "d00000"
      throw exc
    }
  }
}
