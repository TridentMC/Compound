pipeline {
	agent any
	stages {
		stage('Build') {
			steps {
				sh 'rm -f Compound/private.gradle'
				sh './gradlew clean'
				sh './gradlew build'
				archive 'build/libs/*jar'
				archive 'Compound/build/libs/*jar'
			}
		}
		stage('DeployStable') {
			when {
				branch 'stable'
			}
			steps {
				withCredentials([file(credentialsId: 'privateStandard', variable: 'PRIVATEGRADLE')]) {
					sh '''
						rm -f Compound/private.gradle
						cp "$PRIVATEGRADLE" Compound/private.gradle
						./gradlew publish
					'''
				}
			}
		}
		stage('DeploySnapshot') {
			steps {
				withCredentials([file(credentialsId: 'privateSnapshot', variable: 'PRIVATEGRADLE')]) {
					sh '''
						rm -rf Compound/private.gradle
						cp "$PRIVATEGRADLE" Compound/private.gradle
						./gradlew publish
					'''
				}
			}
		}
	}
}
