
def call(Map params = [:]) {

    def imageName = params.get("imageName", "python")
    def imageTag = params.get("imageTag", "3.7")
    def pushBranches = params.get("pushBranches", ["develop"])
    def testRun = params.get("testRun", false)
    def pushPath = params.get("pushPath", "localPython/it/")

    pipeline {
        agent {
            docker {
                image "${imageName}:${imageTag}"
            }
        }
        options {
            timestamps()
        }
        stages {
            stage('checkout') {
                steps {
                    script {
                        scmVars = checkout scm
                        echo "scmVars: ${scmVars}"
                    }
                }
            }
            stage('test') {
                when {
                    anyOf {
                        expression { testRun }
                    }
                }
                steps {
                    sh  """
                        python3 -m venv django
                        . django/bin/activate
                        pip install -r requirements.txt
                        python runtests.py
                        """
                    junit "tests*.xml"
                }            
            }
            stage('build') {

                steps {
                    script {
                        def package_version = "unknown"

                        if (env.GIT_BRANCH) {
                            package_version = "${env.GIT_BRANCH}"
                        }
                        if (env.TAG_NAME) {
                            package_version = "${env.TAG_NAME}"
                        }
                        sh  """
                        export PACKAGE_VERSION=${package_version}
                        python setup.py sdist
                        """ 
                    }
                    
                }
            }
            stage('push') { 
                when {
                    anyOf {
                        buildingTag()
                        expression {
                            pushBranches.contains(env.GIT_BRANCH)
                        }
                    }
                } 
                steps {
                    script {
                        uploadSpec = """{
                          "files": [
                            {
                              "pattern": "dist/*",
                              "target": "${pushPath}"
                            }
                         ]
                        }"""
                        rtUpload (
                            serverId: "artifactoryVBC",
                            spec: uploadSpec
                        )
                        
                    }
                }
            }
        }
        // post block is executed after all the stages/steps in the pipeline
        post {
            always {
                // notify build results, see https://jenkins.io/blog/2016/07/18/pipline-notifications/
                sendBuildNotification currentBuild: currentBuild
                cleanWs(deleteDirs: true)
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