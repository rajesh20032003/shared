def call(Map config) {

    pipeline {
        agent {
            docker {
                image config.image
                args '-u root -v /var/run/docker.sock:/var/run/docker.sock'
            }
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

           stage('quality checks'){
             parallel {
                stage('unit test') {
                    steps {
                        sh 'npm test'
                    }
                }
                stage('lint') {
                    steps {
                        sh 'npm install eslint || true'
                        sh 'npx eslint . || true '
                    }
                }
                stage('security scan') {
                    steps {
                        sh 'npm audit --audit-level=high || true'
                    }
                }
             }
           }

            stage('Docker Build') {
                steps {
                    script {
                        sh "docker build -t ${config.imageName}:latest ."
                    }
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
                        script {
                            sh """
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            docker push ${config.imageName}:latest
                            """
                        }
                    }
                }
            }
        }
    }
}
