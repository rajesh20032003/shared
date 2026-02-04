def call(Map config) {

    pipeline {
        agent {
            docker {
                image config.image
                args '-u root -v /var/run/docker.sock:/var/run/docker.sock'
            }
        }

        environment {
            IMAGE_NAME = config.imageName
        }

        stages {

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Install') {
                steps {
                    sh 'npm install'
                }
            }

            stage('Test') {
                steps {
                    sh 'npm test'
                }
            }

            stage('Docker Build') {
                steps {
                    sh "docker build -t $IMAGE_NAME:latest ."
                }
            }

            stage('Docker Push') {
                when {
                    branch 'main'
                }
                steps {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh '''
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        docker push $IMAGE_NAME:latest
                        '''
                    }
                }
            }
        }
    }
}

