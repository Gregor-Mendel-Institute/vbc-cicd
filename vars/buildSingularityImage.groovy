
def call(Map params = [:]) {

    def imageName = params['imageName']
    def recipeFile = params.get('recipeFile', 'Singularityfile')
    // def extraBuildArgs = params.get("extraBuildArgs", "")

    // buildPath is different than context in docker builds
    def buildPath = params.get('buildPath', "")

    def pushBranches = params.get("pushBranches", ["develop"])
    def pushRegistry = params.get("pushRegistry", "singularity.vbc.ac.at")
    def pushRegistryNamespace = params.get("pushRegistryNamespace", "default")
    def pushRegistryCredentials = params.get("pushRegistryCredentials", null)

    def signKeyCredentials = params.get("signKeyCredentials", null)
    def debugFlag = '' // '--debug'

    def runTest = params.get('runTest', true)
    def isPushBranch = false
    def isTagBuild = false

    def agentLabels = 'dockerce'
    def agentImageTag = 'local_singularity:vbc'
    def agentDockerfile = 'Dockerfile.singularity'
    def agentDockerArgs = '--privileged'

    if(pushRegistryCredentials) {
        echo "using push credentials id ${pushRegistryCredentials}."
    }
    else {
        echo "got no push credentials, will not push."
    }


    pipeline {

        agent {
          node {
            label agentLabels
          }
        }
        options {
            timestamps()
            skipStagesAfterUnstable()
        }

        environment {
           SINGULARITY_CACHEDIR = "${env.WORKSPACE}"
        }
        stages {
            stage('checkout') { // for display purposes
                steps {
                    // cleanWs(deleteDirs: true)
                    echo "checking out for Project '${JOB_NAME}'"
                    // get the code from a git repository
                    script {
                        scmVars = checkout scm
                        echo "scmVars: ${scmVars}"
                        isPushBranch = pushBranches.contains(scmVars.GIT_BRANCH)
                        if (env.TAG_NAME) {
                            isTagBuild = env.TAG_NAME
                            echo "TAG_NAME=${env.TAG_NAME}"
                        }
                        // get dockerfile from library resources
                        writeFile file: agentDockerfile, text: libraryResource(agentDockerfile)
                    }
                }
            }

            // singularity build --section setup
            // singularity build --section post
            // singularity build --section files
            // singularity build --section environment
            // singularity build --section test
            // singularity build --section labels
            stage('build') {
                agent {
                    dockerfile {
                        filename agentDockerfile
                        label agentLabels
                        // dir 'build'
                        additionalBuildArgs "--tag $agentImageTag" + ' --build-arg BUILDER_UID=\$(id -u) --build-arg BUILDER_GID=\$(id -g)'
                        // must be previleged for the build: namespaces etc
                        args agentDockerArgs
                        reuseNode true
                    }
                }
                steps {
                    script {
                        echo "Singularity version"
                        ansiColor('xterm') {
                            sh "env; singularity --version"
                            dir(buildPath) {
                                def buildStatus = sh returnStatus: true, script: "singularity --verbose ${debugFlag} build --force --fakeroot --notest ${imageName}.sif ${recipeFile}"

                                // use warnError in the future
                                if (buildStatus) {
                                    error("failed to build Singularity image")
                                }
                            }
                        }
                    }
                }
            }
            stage('test') {
                agent  {
                    docker {
                        label agentLabels
                        image agentImageTag
                        args agentDockerArgs
                        reuseNode true
                    }
                }
                when {
                    // skip test if necessary
                    expression { runTest }
                }
                steps {
                    script {
                        ansiColor('xterm') {
                            dir(buildPath) {
                                def testResult = sh returnStatus: true, script: "singularity test --fakeroot ${imageName}.sif"
                                if (testResult) {
                                    unstable("test for image ${imageName} failed.")
                                }
                            }
                        }
                    }
                }
            }
            stage('push') {
                agent {
                    docker {
                        label agentLabels
                        image agentImageTag
                        args agentDockerArgs
                        reuseNode true
                    }
                }
                when {
                    anyOf {
                        // always push tags
                        buildingTag()
                        // should we push the current branch?m skipped if no credentials are provided
                        expression { isPushBranch && pushRegistryCredentials }
                    }
                }
                steps {
                    script {
                        //forAllBuilds({ name, conf, image ->
                        // sh "singularity remote add vbc singularity.vbc.ac.at"
                        // sh "singularity remote use vbc"
                        // FIXE inject token as secret file sh "singularity remote login vbc  --tokenfile /dev/null"
                        // sh "singularity sign ${imageName}.sif"
                        //sh "singularity push ${imageName}.sif library://${pushRegistry}/${pushNamespace}/${imageName}:${BRANCH_NAME}"
                        def pushUnsignedParam  = ' --allow-unsigned '
                        if(signKeyCredentials) {
                            // TODO read credentials, import key, sign the image
                            pushUnsignedParam = ''
                            echo 'image signing not implemented (yet).'
                        }
                        else {
                            echo "no signing key, will push unsigned"
                        }
                        withCredentials([usernamePassword(credentialsId: pushRegistryCredentials, usernameVariable: 'SINGULARITY_LIBRARY_USER', passwordVariable: 'SINGULARITY_LIBRARY_TOKEN')]) {
                            def registryAlias = 'jenkins'
                            dir(buildPath) {
                                sh 'echo $SINGULARITY_LIBRARY_TOKEN |' + " singularity remote add ${registryAlias} ${pushRegistry}"
                                sh "singularity remote list"
                                sh "singularity remote status ${registryAlias}"
                                sh "singularity remote use ${registryAlias}"
                                sh "singularity push ${pushUnsignedParam} ${imageName}.sif library://" + '$SINGULARITY_LIBRARY_USER' + "/${pushRegistryNamespace}/${imageName}:${BRANCH_NAME}"
                            }
                        }
                        //})
                    }
                }
            }
        }
        // post block is executed after all the stages/steps in the pipeline
        post {
            always {
                // notify build results, see https://jenkins.io/blog/2016/07/18/pipline-notifications/
                sendBuildNotification currentBuild: currentBuild
                echo "this is done whatever the state"
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

