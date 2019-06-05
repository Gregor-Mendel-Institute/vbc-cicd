// signature of this function call
// example:
//     buildDockerImage([imageName: "myImage"])
//
// will be pushed to docker.artifactory.imp.ac.at/it/myImage:latest
// other params:
//   testCmd: a script to run tests (default: ./test.sh, MUST produce one or more files matching testResultPattern output files)
//      to disable testing: set to null
//   testResultPattern: search pattern for junit style xml test results: default: **/junit.xml,**/TEST*.xml
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
    def pushRegistry = params.get("pushRegistry", "docker.artifactory.imp.ac.at")
    def pushRegistryCredentials = params.get("pushRegistryCredentials", "jenkins_artifactory_creds")
    def pushRegistryNamespace = params.get("pushRegistryNamespace", "it")
    def testCmd = params.get("testCmd", "./test.sh")
    def testResultPattern = params.get("testResultPattern", "**/junit.xml,**/TEST*.xml")
    def productionBranch = params.get("productionBranch", "production")

    // default agent labels for the build job: docker, centos
    def defaultAgentLabels = ["dockerce"]
    def agentLabels = params.get("agentLabels", defaultAgentLabels)

    echo "checking agentLabels, so far: ${agentLabels}"
    for (defaultLbl in defaultAgentLabels) {
        if (agentLabels.contains(defaultLbl) == false) {
            echo "adding label ${defaultLbl}, as it was missing but is required"
            agentLabels.add(defaultLbl)
        }
    }

    agentLabels = agentLabels.join(' && ')

    def imgRepo = "${pushRegistry}/${pushRegistryNamespace}/${imageName}"
    def scmVars = null
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
                    }
                }
            }
            // 
            stage('build') {
                steps {
                    script {
                        productImage = docker.build(imgRepo)
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
                        }
                    }
                    // collect test results
                   junit keepLongStdio: true, testResults: testResultPattern
                }
            }
            stage('push') {
                when {
                    buildingTag()
                }
                steps {
                    script {
                        productImage.push('latest')
                        productImage.push("${TAG_NAME}")
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

