
pipeline {

// this is the seed job jenkinsfile
// this job definition loads job DSL groovy script in order to generate jobs for all things Ansible

    agent any

    triggers {
        // execute once an hour, on a minute calculated from the project name
        cron('H * * * *')
    }

    stages {
        stage('checkout') { // for display purposes
            steps {
                script {
                    // this is not for declarative pipelines, only for scripted
                    properties([[$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1h']])])
                }
                // get the code from a git repository
                checkout scm
            }
        }
        stage('generate') {
            steps {
                // call the jobdsl script for the roles
                // don't use sandbox -> we can run as SYSTEM and do the docker build
                jobDsl removedConfigFilesAction: 'DELETE', removedJobAction: 'DELETE', removedViewAction: 'DELETE', sandbox: false, targets: 'jobs/ansibleRoles.groovy'
            }
        }
    }
    // post block is executed after all the stages/steps in the pipeline
    post {
        always {
            // notify build results, see https://jenkins.io/blog/2016/07/18/pipline-notifications/
            // notifyBuild(currentBuild.result)
            echo "this will always show up"
        }
        changed {
            echo "build changed"
        }
        aborted {
            echo "build aborted"
        }
        failure {
            echo "build failed"
        }
        success {
            echo "build is success"
        }
        unstable {
            echo "build is unstable"
        }
    }

}
