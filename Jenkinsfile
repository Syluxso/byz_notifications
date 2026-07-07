pipeline {
    agent any

    tools {
        jdk 'jdk-23'
        maven 'Maven'
    }

    environment {
        DEPLOY_DIR = '/opt/services/notifications'
        JAR_NAME = 'app.jar'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Deploy') {
            steps {
                script {
                    def jarFile = sh(
                        script: "find target -maxdepth 1 -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -1",
                        returnStdout: true
                    ).trim()

                    if (!jarFile) {
                        error 'No runnable JAR found in target/'
                    }

                    sh """
                        sudo mkdir -p ${DEPLOY_DIR}
                        sudo cp '${jarFile}' ${DEPLOY_DIR}/${JAR_NAME}
                        sudo chown root:root ${DEPLOY_DIR}/${JAR_NAME}
                        sudo supervisorctl reread
                        sudo supervisorctl update notifications || true
                        sudo supervisorctl start notifications || sudo supervisorctl restart notifications
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Notification service deployed to notifications.byzantineapp.dev'
        }
        failure {
            echo 'Build or deploy failed.'
        }
    }
}