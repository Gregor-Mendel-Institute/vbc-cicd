
def call(Map params = [:]) {

    multiBuild.addParam('imageName', params['imageName'], true)
    multiBuild.addParam('recipeFile',  params.get('recipeFile', 'Singularityfile'))

    // buildPath is different than context in docker builds
    multiBuild.addParam('buildPath',  params.get('buildPath', ""))
    multiBuild.addParam('signKeyCredentials', params.get("signKeyCredentials", null))

    multiBuild.addParam('pushRegistry', params.get("pushRegistry", "singularity.vbc.ac.at"))
    multiBuild.addParam('pushRegistryNamespace', params.get("pushRegistryNamespace", "default"))
    multiBuild.addParam('pushRegistryCredentials', params.get("pushRegistryCredentials", null))

    // global only, as this switches the test stage on/off
    def runTest =  params.get('runTest', true)
    // branches that will be pushed
    def pushBranches = params.get("pushBranches", ["develop"])

    def debugFlag = '' // '--debug'

    multiBuild.addMultiParams('imageName', params.get('containerImages', []))

    def isPushBranch = false
    def isTagBuild = false

    def agentLabels = 'dockerce'
    def agentImageTag = 'local_singularity:vbc'
    def agentDockerfile = 'Dockerfile.singularity'
    def agentDockerArgs = '--privileged'

    def pushRegistryCredentials = multiBuild.defaultParams.get('pushRegistryCredentials', false)
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
                        sh "env; singularity --version"
                        ansiColor('xterm') {
                            multiBuild.forAllBuilds({name, conf, build ->
                                echo("building image '${name}'.")
                                dir(conf.buildPath) {
                                    def buildStatus = sh returnStatus: true, script: "singularity --verbose ${debugFlag} build --force --fakeroot --notest ${name}.sif ${conf.recipeFile}"

                                    // use warnError in the future
                                    if (buildStatus) {
                                        error("failed to build Singularity image ${name}")
                                    }
                                }
                            })
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
                            multiBuild.forAllBuilds({name, conf, build -> 
                                dir(conf.buildPath) {
                                    def testResult = sh returnStatus: true, script: "singularity test --fakeroot ${name}.sif"
                                    if (testResult) {
                                        unstable("test for image ${name} failed.")
                                    }
                                }
                            })
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
                        multiBuild.forAllBuilds({name, conf, build -> 
                            def pushUnsignedParam  = ' --allow-unsigned '
                            if(conf.signKeyCredentials) {
                                // TODO read credentials, import key, sign the image
                                pushUnsignedParam = ''
                                echo 'image signing not implemented (yet).'
                            }
                            else {
                                echo "no signing key, will push unsigned"
                            }
                            withCredentials([usernamePassword(credentialsId: conf.pushRegistryCredentials, 
                                                              usernameVariable: 'SINGULARITY_LIBRARY_USER', 
                                                              passwordVariable: 'SINGULARITY_LIBRARY_TOKEN')]) {
                                def registryAlias = name
                                dir(conf.buildPath) {
                                    sh 'echo $SINGULARITY_LIBRARY_TOKEN |' + " singularity remote add ${registryAlias} ${conf.pushRegistry}"
                                    sh "singularity remote use ${registryAlias}"
                                    sh "singularity remote list"
                                    sh "singularity remote status ${registryAlias}"
                                    sh "singularity push ${pushUnsignedParam} ${conf.imageName}.sif library://" + '$SINGULARITY_LIBRARY_USER' +  \
                                        "/${conf.pushRegistryNamespace}/${conf.imageName}:${BRANCH_NAME}"
                                }
                            }
                        })
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

