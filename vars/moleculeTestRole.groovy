#!/usr/bin/env groovy

/**
 *  this will be called as function, returning the actual (scripted pipeline) implementation of the build job
 * @param params
 */
def call(Map params = [:]) {

    // global var settings
    def projectParts = JOB_NAME.tokenize('/') as String[]
    def multibranchProjectName = projectParts[projectParts.length - 1]

    def dockerSettings = moleculeDockerSettings()

    ///////////////// params go here ///////////////////////////
    // hand in molecule scenarios to run: use "default" if not provided
    def moleculeScenarios = params.get("moleculeScenarios", ["default"])

    // assuming we must be running in a multibranch project, extract actual repo name from project name
    def roleName = params.get("roleName", projectParts[projectParts.length - 2])

    // default for setting MOLECULE_DEBUG env
    def moleculeDebug = params.get("moleculeDebug", false)

    // additional credentials were handed in as list: https://jenkins.io/doc/pipeline/steps/credentials-binding/
    def credentials = params.get("credentials", [])
    for (cred in credentials) {
        echo "using credential: ${cred}"
    }

    // allow job concurrency
    def concurrency = params.get("concurrency", true)

    // default agent labels for the build job: docker, centos
    def defaultAgentLabels = ["docker", "centos"]
    def agentLabels = params.get("agentLabels", defaultAgentLabels)

    echo "checking agentLabels, so far: ${agentLabels}"
    for (defaultLbl in defaultAgentLabels) {
        if (agentLabels.contains(defaultLbl) == false) {
            echo "adding label ${defaultLbl}, as it was missing but is required"
            agentLabels.add(defaultLbl)
        }
    }

    agentLabels = agentLabels.join(' && ')


    //def roleDirs = []
    //def roleScenarios = []
    //def roleBaseDir = null
    pipeline {

        agent {
            node {
                label agentLabels
            }
        }
        options {
            timestamps()
        }
        parameters {
            booleanParam(name: 'MOLECULE_DEBUG', defaultValue: moleculeDebug, description: 'enable Molecule debug log')
        }
        stages {
            stage("Molecule Docker") {
                steps {
                    script {
                        echo "ensure default docker environment exists here"
                        buildMoleculeDocker(dockerSettings)
                    }
                }
            }
            stage('checkout') { // for display purposes
                steps {
                    echo "checking out for Project '${roleName}' in '${multibranchProjectName}'"
                    // get the code from a git repository
                    //checkout scm
                    checkout([
                            $class: 'GitSCM',
                            branches: scm.branches,
                            doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: roleName]], // [$class: 'WipeWorkspace'],  breaks things?
                            userRemoteConfigs: scm.userRemoteConfigs
                    ])
                }
            }
            // FIXME here the previously built container can be used
            stage('molecule test') {
                agent {
                    docker {
                        image "${dockerSettings.imageTag}"
                        args '-v /var/run/docker.sock:/var/run/docker.sock'
                        reuseNode true
                    }
                }

                steps {
                    script {
                        withCredentials(credentials) {
                            runMoleculeTest(roleName, [debug: params.MOLECULE_DEBUG, scenarios: moleculeScenarios, concurrency: concurrency])
                        }
                    }
                }
            }
        }
        // post block is executed after all the stages/steps in the pipeline
        post {
            always {
                // notify build results, see https://jenkins.io/blog/2016/07/18/pipline-notifications/
                sendBuildNotification currentBuild: currentBuild
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

    /**
    def labels = params.containsKey('labels') ? params.labels : "docker"

    // shamelessly stolen from here https://github.com/jenkins-infra/pipeline-library/blob/master/vars/buildPlugin.groovy#L10
    properties([
            durabilityHint('PERFORMANCE_OPTIMIZED'),
            buildDiscarder(logRotator(numToKeepStr: '5')),
    ])



    node(labels) {
        stage("Checkout") {
            scm checkout
        }

        // Requires the Docker Pipeline plugin to be installed
        docker.image('molecule:vbc').inside('-v /var/run/docker.sock:/var/run/docker.sock') {
            stage('Uptime') {
                sh 'uptime'
            }
        }
    }
*/
}
