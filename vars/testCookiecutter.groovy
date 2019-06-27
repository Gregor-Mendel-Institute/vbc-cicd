
// test the cookiecutter template job

def call(Map settings = [:]) {

    def dockerSettings = moleculeDockerSettings()

    // Jenkinsfile for testing the cookiecutter
    pipeline {
        agent {
            node {
                label 'dockerce'
            }
        }
        triggers {
            bitbucketPush()
            // every fifteen minutes (perhaps at :07, :22, :37, :52)
            pollSCM 'H/15 * * * *'
        }
        environment {
            GIT_AUTHOR_NAME = 'Jenkins'
            GIT_AUTHOR_EMAIL = 'cookiecutter@jenkins.example.com'
            GIT_COMMITTER_NAME = 'Jenkins'
            GIT_COMMITTER_EMAIL = 'cookiecutter@jenkins.example.com'
        }
        parameters {
            booleanParam(name: 'MOLECULE_DEBUG', defaultValue: false, description: 'enable Molecule debug log')
        }
        stages {
            stage('prepare environment') {
                steps {
                    // wipeout workspace before anything
                    cleanWs()
                    // do the checkout just so we know all the GIT settings from upstream
                    //TODO this could be a very shallow clone

                    dir("cookiecutter-molecule-upstream") {
                        checkout scm
                    }
                    script {
                        buildMoleculeDocker(dockerSettings)
                    }
                }
            }
            stage('cookiecutter') {
                agent {
                    docker {
                        image "${dockerSettings.imageTag}"
                        args '-v /var/run/docker.sock:/var/run/docker.sock'
                        reuseNode true
                    }
                }
                steps {
                    script {
                        sh "cookiecutter --version"
                        echo "running the cookiecutter template from ${env.GIT_URL} on branch ${env.BRANCH_NAME} "

                        // credentials are for 	svc_bitbucket_access to ssh into bitbucket
                        sshagent(['dd2eddb1-0e79-4acb-9dca-5fe6b4ba25b3']) {
                            sh "export GIT_SSH_COMMAND=\"ssh -o StrictHostKeyChecking=no\"; cookiecutter git+${env.GIT_URL} --checkout ${env.BRANCH_NAME} --no-input"
                        }
                    }
                }
            }
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
                        // shared implementation from vbc-cicd
                        runMoleculeTest("role-dummy", [debug: params.MOLECULE_DEBUG, scenarios: ["default"]])
                    }
                }
            }
        }

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
}
