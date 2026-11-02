pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    // Fires on GitHub webhook (http://<jenkins-ip>:8080/github-webhook/); pollSCM is a fallback.
    triggers {
        githubPush()
        pollSCM('H/2 * * * *')
    }

    environment {
        APP_NAME   = 'devops-demo'
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
        // Jenkins credentials (created automatically by the Ansible setup):
        K8S_HOST   = credentials('k8s-host-ip')        // Secret text: private IP of the Kubernetes VM
        ACR_SERVER = credentials('acr-login-server')   // Secret text: <name>.azurecr.io
        ACR_CRED   = credentials('acr-creds')          // Username/password of Azure Container Registry
        // Pinned so `mvn sonar:sonar` works without a pluginGroups entry. Server URL + token come from withSonarQubeEnv.
        SONAR_MVN  = 'org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --pretty="Commit: %h - %s (%an)"'
            }
        }

        stage('Build & Package (Maven)') {
            steps {
                sh 'mvn -B -q clean package -DskipTests'
            }
        }

        stage('Unit Tests (Maven)') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        // Static analysis: bugs, vulnerabilities, code smells, duplication and JaCoCo coverage.
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {      // name of the server configured in Jenkins (created by Ansible)
                    sh 'mvn -B ${SONAR_MVN}:sonar'
                }
            }
        }

        // Blocks here until SonarQube calls the Jenkins webhook; a failed Quality Gate stops the pipeline
        // BEFORE anything is built into an image or deployed.
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build & Tag') {
            steps {
                script {
                    env.IMAGE = "${env.ACR_SERVER}/${env.APP_NAME}:${env.IMAGE_TAG}"
                }
                sh '''
                    docker build --build-arg BUILD_NUMBER=${IMAGE_TAG} -t ${IMAGE} -t ${ACR_SERVER}/${APP_NAME}:latest .
                '''
            }
        }

        stage('Push to Azure Container Registry') {
            steps {
                sh '''
                    echo "${ACR_CRED_PSW}" | docker login ${ACR_SERVER} --username ${ACR_CRED_USR} --password-stdin
                    docker push ${IMAGE}
                    docker push ${ACR_SERVER}/${APP_NAME}:latest
                '''
            }
        }

        stage('Deploy to Kubernetes (Ansible)') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'k8s-ssh-key', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
                    sh '''
                        cd ansible
                        ansible-playbook -i "${K8S_HOST}," deploy.yml \
                          -u ${SSH_USER} --private-key ${SSH_KEY} \
                          -e image=${IMAGE}
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    for i in $(seq 1 12); do
                      RESP=$(curl -sf http://${K8S_HOST}:30080/health || true)
                      echo "Attempt $i: $RESP"
                      echo "$RESP" | grep -q "\\"version\\":\\"${IMAGE_TAG}\\"" && echo "Deployed build ${IMAGE_TAG} is live" && exit 0
                      sleep 5
                    done
                    echo "Verification failed"; exit 1
                '''
            }
        }
    }

    post {
        success { echo "SUCCESS: build ${env.BUILD_NUMBER} is live" }
        failure { echo 'FAILED: check the stage logs above' }
        always  { sh 'docker image prune -f || true' }
    }
}
