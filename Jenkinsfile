#!/usr/bin/env groovy

// transport scmVars through all stages, i.e. global
def scmVars

pipeline {

// this is the seed job jenkinsfile
// this job definition loads job DSL groovy script in order to generate jobs for all things Ansible

    agent any

    triggers {
        // execute once an hour, on a minute calculated from the project name
        cron('H * * * *')
    }
    parameters {
        //fileParam("pipelines.yml", "Pipeline definition fila (yaml)")
    }

    stages {
        // for display purposes
        stage('checkout') {
            steps {
                script {
                    // this is not for declarative pipelines, only for scripted
                    properties([[$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1h']])])

                    // get the code from a git repository
                    scmVars = checkout scm
                }
            }
        }
        stage('generate') {
            steps {
                script {
                    echo "my git setup: ${scmVars}"
                    echo "will configure library as: ${cicdLibSettings.name} in version: ${cicdLibSettings.version} (commit or branch or tag)"

                    // Bitbucket organizations to scan for roles
                    def discovery_data = readYaml file: "default_discovery.yml"

                    // shared CICD library config, could also do version: scmVars.GIT_COMMIT
                    if not cicdLibSettings.version {
                        cicdLibSettings.version = scmVars.GIT_BRANCH
                    }


                    // load myself as library for the discovery scripts, same repo ref as the seed job itself
                    //library identifier: cicdLibSettings.name, retriever: modernSCM(scm)
                    // call the jobdsl script for the roles
                    jobDsl removedConfigFilesAction: 'DELETE',
                           removedJobAction: 'DELETE',
                           removedViewAction: 'DELETE',
                           lookupStrategy: 'SEED_JOB',
                           sandbox: true,
                           targets: 'jobs/*.groovy',
                           additionalParameters: [
                               cicdLibConfig: cicdLibSettings,
                               discoverOrgs: discovery_data.repo_orgs
                           ]
                 }
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
