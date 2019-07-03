// signature of this function call
// example:
//     buildDockerImage([imageName: "myImage"])
//
// will be pushed to docker.artifactory.imp.ac.at/it/myImage:latest
// other params:
//   testCmd: a script to run tests (default: ./test.sh)
//      to disable testing: set to null
//   testResultPattern: search pattern for junit style xml test results: default: **/junit.xml,**/TEST*.xml This pattern will be searched inside container
//   pushBranches: list of branches to push (tagged by branch name), default: develop
//   pushRegistry: default: "docker.artifactory.imp.ac.at"
//   pushRegistryCredentials: "defaults to credentials for artifactory"
//   pushRegistryNamespace: default: "it"
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

    def testCmd = params.get("testCmd", "./test.sh")
    def testResultPattern = params.get("testResultPattern", "**/junit.xml,**/TEST*.xml")
    def productionBranch = params.get("productionBranch", "production")

    def towerConfigs = params.get("tower", [:])

    // default agent labels for the build job: docker, rhel8
    def defaultAgentLabels = ["dockerce", "rhel8"]
    def agentLabels = params.get("agentLabels", defaultAgentLabels)
    echo "checking agentLabels: ${agentLabels}"
    agentLabels = agentLabels.join(' && ')

    def imgRepo = "${pushRegistry}/${pushRegistryNamespace}/${imageName}"
    def scmVars = null
    def isPushBranch = false
    def isTagBuild = false
    def productImage = null
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
                        docker.withRegistry("https://${pullRegistry}", pullRegistryCredentials) {
                            productImage = docker.build(imgRepo)
                        }
                    }
                }
            }
            stage('test') {
                when {
                    // Only say hello if a "greeting" is requested
                    expression { testCmd != null }
                }
                steps {
                    script {
                        productImage.inside() {  
                            sh 'env'
                            echo 'my pwd:'
                            sh 'pwd'
                            echo "running tests as ${testCmd}"
                            // dont fail on error, we'll be UNSTABLE with failed tests
                            def test_status = sh returnStatus: true, script: testCmd
                            
                             // collect test results
                             // https://stackoverflow.com/questions/39920437/how-to-access-junit-test-counts-in-jenkins-pipeline-project
                            junit keepLongStdio: true, allowEmptyResults: true, testResults: testResultPattern
                        }
                    }
                }
            }
            stage('push') {
                when {
                    anyOf {
                        // always push tags
                        buildingTag()
                        expression {
                            // should we push the current branch?
                            return isPushBranch
                        }
                    }
                }
                steps {
                    script {
                        docker.withRegistry("https://${pushRegistry}", pushRegistryCredentials) {
                            // push image, if we're building a tag
                            if (isTagBuild) {
                                productImage.push('latest')
                                productImage.push(TAG_NAME)
                            }
                            // push image with branch name
                            else if (isPushBranch) {
                                productImage.push(scmVars.GIT_BRANCH)
                            }
                        }
                    }
                }
            }
            stage('tower') {
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
                                return towerConfigs['tags']
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

