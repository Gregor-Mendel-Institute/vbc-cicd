// signature of this function call
// example:
//     buildDockerImage([imageName: "myImage"])
//
// will be pushed to docker.artifactory.imp.ac.at/it/myImage:latest
// other params:
//   test: Closure test implementation { dockerBuilds -> to Tests; } helpers: testScript(String scriptPath, String testPattern) 
//   testCmd: DEPRECATED. a script to run tests (default: ./test.sh)
//      to disable testing: set to null
//   testResultPattern: DEPRECATED. search pattern for junit style xml test results: default: **/junit.xml,**/TEST*.xml This pattern will be searched inside container
//   pushBranches: list of branches to push (tagged by branch name), default: develop
//   pushRegistry: default: "docker.artifactory.imp.ac.at"
//   pushRegistryCredentials: "defaults to credentials for artifactory"
//   pushRegistryNamespace: default: "it"
//   tower: tower configurations to trigger Tower job runs, dict (other config params see runTowerJob.groovy):
//      [
//         tags: [jobName: 'my Job Template name],
//         develop: [jobName: 'my Thing TDE'],
//         branch2: [jobName: 'my special job']
//       ]
//   containerImages: additional images to build, (same namespace, tag, registry as above, tests default to global params) list of dicts:
//     [
//       [imageName: "my-extra-imp", dockerFile: "relative/to/Dockerfile", dockerContext: "relative/to/context"] 
//     ]
//   agentLabels: additional labels to run build job on. (will be appended to "dockerce")

def call(Map params = [:]) {

    // global var settings
    def projectParts = JOB_NAME.tokenize('/') as String[]
    def multibranchProjectName = projectParts[projectParts.length - 1]

    // params go here
    def imageName = params['imageName']
    def pullRegistry = params.get("pullRegistry", "registry.redhat.io")
    def pullRegistryCredentials = params.get("pullRegistryCredentials", "redhat-registry-service-account")

    def pushBranches = params.get("pushBranches", ["develop"])
    def pushRegistry = params.get("pushRegistry", "docker.artifactory.imp.ac.at")
    def pushRegistryCredentials = params.get("pushRegistryCredentials", "jenkins_artifactory_creds")
    def pushRegistryNamespace = params.get("pushRegistryNamespace", "it")

    def containerImageConfigs = params.get("containerImages", [])

    def testCmd = params.get("testCmd", null)
    def testResultPattern = params.get("testResultPattern", "**/junit.xml,**/TEST*.xml")
    def testClos = params.get("test", null)
    if (testCmd && testClos) {
        error("cannot handle both arguments 'test' and testCmd(DEPRECATED). Recommended is test: testScript('./path/to/Script', '**/TEST*.xml') ")
    }
    else if (testCmd) {
        unstable("testCmd is DEPRECATED. Please use test: testScript('./path/to/Script', '**/TEST*.xml')")
        testClos = testScript(testCmd, testResultPattern, imageName)
    }

    def towerConfigs = params.get("tower", [:])

    // default agent labels for the build job: docker, rhel8
    def defaultAgentLabels = ["dockerce", "rhel8"]
    def agentLabels = params.get("agentLabels", defaultAgentLabels)
    echo "checking agentLabels: ${agentLabels}"
    agentLabels = agentLabels.join(' && ')
    ///////////////////////////////////////////////////////////////////////////////////////

    def scmVars = null
    def isPushBranch = false
    def isTagBuild = false
    def tagName = null
    // track all built / pushed docker images
    def dockerBuilds = [:]
    // build default Dockerfile config (in repo root)
    def defaultImageConfig = [
        imageName: imageName,
        dockerFile: './Dockerfile',
        dockerContext: '.',
        pullRegistry: pullRegistry,
        pullRegistryCredentials: pullRegistryCredentials,
        pushRegistry: pushRegistry,
        pushRegistryCredentials: pushRegistryCredentials,
        pushRegistryNamespace: pushRegistryNamespace,
    ]
    // add default image, if a name is defined
    if (imageName) {
        echo "adding default image config: ${defaultImageConfig}}"
        dockerBuilds.put(imageName, [name: imageName, config: defaultImageConfig, image: null])
    }
    else {
        echo "no default image defined"
    }
    for (imageConfig in containerImageConfigs) {
       if (!imageConfig['imageName']) {
           error("image config is invalid (no imageName): ${imageConfig}")
       }
       def fullImageConfig = [:]
       fullImageConfig.putAll(defaultImageConfig)
       fullImageConfig.putAll(imageConfig)
       echo "adding extra image config: ${fullImageConfig}"
       dockerBuilds.put(fullImageConfig['imageName'], [name: fullImageConfig['imageName'], config: fullImageConfig, image: null])
    }

    // iterator helper over all build configs / image builds
    def forAllBuilds = { Closure bodyPerBuild ->
        dockerBuilds.each { name, build ->
            def conf = build.config
            def image = build.image

            bodyPerBuild(name, conf, image, build)
        }
    }


    pipeline {

        agent {
            node {
                label agentLabels
            }
        }
        options {
            timestamps()
        }
        // parameters {
        // }
        stages {
            stage('checkout') { // for display purposes
                steps {
                    cleanWs(deleteDirs: true)
                    echo "checking out for Project '${JOB_NAME}' in '${multibranchProjectName}'"
                    // get the code from a git repository
                    script {
                        scmVars = checkout scm
                        echo "scmVars: ${scmVars}"
                        isPushBranch = pushBranches.contains(scmVars.GIT_BRANCH)
                        if (env.TAG_NAME) {
                            isTagBuild = env.TAG_NAME
                            echo "TAG_NAME=${env.TAG_NAME}"
                        }

                    }
                }
            }
            // 
            stage('build') {
                steps {
                    script {
                        forAllBuilds({ name, conf, image, build ->
                            echo "building image ${name}"
                            docker.withRegistry("https://${conf.pullRegistry}", conf.pullRegistryCredentials) {
                                def imgRepo = "${conf.pushRegistry}/${conf.pushRegistryNamespace}/${name}"
                                def jenkinsLabels = "--label vbc.git.commit='${GIT_COMMIT}' \
                                                     --label vbc.git.branch='${GIT_BRANCH}' \
                                                     --label vbc.git.tag='${isTagBuild}' \
                                                     --label vbc.git.url='${GIT_URL}' \
                                                     --label vbc.jenkins.buildtag='${BUILD_TAG}' \
                                                     --label vbc.jenkins.node='${NODE_NAME}' \
                                                     --label vbc.jenkins.buildnumber='${BUILD_NUMBER}' \
                                                     --label vbc.jenkins.buildurl='${BUILD_URL}'"
                                def jenkinsArgs = "--build-arg GIT_COMMIT='${GIT_COMMIT}' \
                                                  --build-arg GIT_BRANCH='${GIT_BRANCH}' \
                                                  --build-arg GIT_TAG='${isTagBuild}' \
                                                  --build-arg GIT_URL='${GIT_URL}' \
                                                  --build-arg BUILD_TAG='${BUILD_TAG}' \
                                                  --build-arg BUILD_URL='${BUILD_URL}' \
                                                  --build-arg BUILD_NUMBER='${BUILD_NUMBER}' \
                                                  --build-arg NODE_NAME='${NODE_NAME}'"

                                def productImage = docker.build("${imgRepo}:${GIT_COMMIT}", "-f ${conf.dockerFile} ${jenkinsLabels} ${jenkinsArgs} ${conf.dockerContext}")
                                build.putAll([image: productImage, repo: imgRepo])
                            }
                        })
                    }
                }
            }
            stage('test') {
                when {
                    // Only say hello if a "greeting" is requested
                    expression { testClos != null }
                }
                steps {
                    script {
                        // test closure
                        //    return { imageName, defaultImageName, allBuilds -> ... }
                        //
                        testClos(imageName, dockerBuilds)
                    }
                }
            }
            stage('push') {
                when {
                    anyOf {
                        // always push tags
                        buildingTag()
                        // should we push the current branch?
                        expression { isPushBranch }
                    }
                }
                steps {
                    script {
                        forAllBuilds({ name, conf, image ->
                            docker.withRegistry("https://${conf.pushRegistry}", conf.pushRegistryCredentials) {
                                // push image, if we're building a tag
                                if (isTagBuild) {
                                    image.push('latest')
                                    image.push(TAG_NAME)
                                }
                                // push image with branch name
                                else if (isPushBranch) {
                                    image.push(scmVars.GIT_BRANCH)
                                }
                            }
                        })
                    }
                }
            }
            stage('tower') {
                // triggering tower can happen anywhere
                agent any

                // deploy when tagged and tower is configured
                when {
                    anyOf {
                        allOf {
                            expression {
                                return isPushBranch && towerConfigs[scmVars.GIT_BRANCH]
                            }
                        }
                        allOf {
                            buildingTag()
                            expression {
                                return towerConfigs['tags'] != null
                            }
                        }
                    }
                }
                steps {
                    script {
                        def configKey = null
                        if (isPushBranch) {
                            configKey = scmVars.GIT_BRANCH
                        }
                        else if(isTagBuild) {
                            configKey = 'tags'
                        }
                        echo "tower config key is: ${configKey}"
                        def config = towerConfigs[configKey]
                        def jobName = config['jobName']
                        echo "trigger tower job '${jobName}' with settings(${configKey}): ${config}"
                        runTowerJob(jobName, config)
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
}

