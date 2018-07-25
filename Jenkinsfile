pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        withMaven(maven: 'M3.5', mavenSettingsConfig: 'fb57b2b9-c2e4-4e05-955e-8688bc067515', mavenLocalRepo: "$WORKSPACE/../../.m2/${env.BRANCH_NAME}") {
          sh 'mvn clean install -DskipTests'
        }
      }
    }
    stage('Test') {
      steps {
        withMaven(maven: 'M3.5', mavenSettingsConfig: 'fb57b2b9-c2e4-4e05-955e-8688bc067515', mavenLocalRepo: "$WORKSPACE/../../.m2/${env.BRANCH_NAME}") {
          sh 'mvn test'
        }
      }
      post {
        always {
            step([$class: 'Publisher', reportFilenamePattern: '**/testng-results.xml'])
        }
      }
    }
    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        withMaven(maven: 'M3.5', mavenSettingsConfig: 'fb57b2b9-c2e4-4e05-955e-8688bc067515', mavenLocalRepo: "$WORKSPACE/../../.m2/${env.BRANCH_NAME}") {
          sh 'mvn deploy'
        }
      }
    }
  }
}