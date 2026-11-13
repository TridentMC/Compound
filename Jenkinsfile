pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    stages {
        stage('Build and test') {
            steps {
                sh './gradlew :clean :Molecule:clean :build :Molecule:build --max-workers=1 --no-parallel --no-daemon'
                archiveArtifacts artifacts: 'build/libs/compound-*.jar', fingerprint: true
            }
            post {
                always {
                    junit testResults: '**/build/test-results/test/TEST-*.xml'
                    archiveArtifacts artifacts: 'Molecule/build/screenshots/**/*.png,build/screenshots/*.png', allowEmptyArchive: true
                }
            }
        }
        stage('Publish stable Compound') {
            when {
                allOf {
                    branch 'stable'
                    not { changeRequest() }
                }
            }
            steps {
                withCredentials([file(credentialsId: 'privateStandard', variable: 'PRIVATEGRADLE')]) {
                    sh '''
                        trap 'rm -f private.gradle' EXIT
                        cp "$PRIVATEGRADLE" private.gradle
                        ./gradlew :publish --max-workers=1 --no-parallel --no-daemon
                    '''
                }
            }
        }
        stage('Publish snapshot Compound') {
            when {
                allOf {
                    anyOf {
                        branch '26.3'
                        branch 'master'
                    }
                    not { changeRequest() }
                }
            }
            steps {
                withCredentials([file(credentialsId: 'privateSnapshot', variable: 'PRIVATEGRADLE')]) {
                    sh '''
                        trap 'rm -f private.gradle' EXIT
                        cp "$PRIVATEGRADLE" private.gradle
                        ./gradlew :publish --max-workers=1 --no-parallel --no-daemon
                    '''
                }
            }
        }
    }
}
