
def call(String roleName, Map params=[debug: false, scenarios: ["default"], concurrency: true]) {

    echo "running molecule in this env:"
    sh "id"
    sh "ansible --version"
    sh "molecule --version"

    echo "running molecule scenarios: ${params.scenarios}"

    def moleculeBaseConfigFlag = ""
    if(params.moleculeBaseConfig) {
        // write molecule base config from library resources to job's workspace
        writeFile file: "${roleName}/${params.moleculeBaseConfig}", text: libraryResource(params.moleculeBaseConfig)
        moleculeBaseConfigFlag = "--base-config ${params.moleculeBaseConfig}"
    }

    // set debug flag dependeing on build params
    def moleculeDebugFlag = params.debug ? "--debug" : ""
    def parallelStages = [:]

    if(params.concurrency) {
        echo "scenarios in parallel"
    }
    else {
        echo "scenarios serialized"
    }

    for (scenario in params.scenarios) {
        def localScenario = "${scenario}"
        parallelStages[scenario] = {
            stage(localScenario) {
                // sanitize unique id for save container usage: replace " ", "." and "_" with the "-"
               
                // demonstrate closure caputres
                // echo "scenario: " + scenario
                // echo "scenario_sub: ${scenario}"
                // echo "localScenario: " + localScenario
                // echo "localScenario_sub: ${localScenario}"
                
                // this will work with groovy 2.5.0 and above
                //.digest("SHA-1").take(16) 
                def longUniqueTag = (localScenario + "-${env.BUILD_TAG}-${env.GIT_COMMIT}").replaceAll("[ ._]", "-")
                def tagFile = "unique_job_tag." + localScenario
                sh "echo '${longUniqueTag}' > ${tagFile}"

                // shorten ID, as it will be used also as default hostname in container (maxsize hostname is 64 chars)
                def uniqueTag = sha1(file: tagFile).take(16)

                echo "==================== BEGIN scenario ${localScenario} ===================="
                sh "echo 'my instance id is ${uniqueTag}'"
                // docker older 17.12 does not like dir() {  } here, we might run on RHEL with old docker...
                // sudo is necessary, as we need a new login shell with all our group memberships
                def moleculeCmd = "export INSTANCE_ID=${uniqueTag} && cd ${roleName} && sudo --preserve-env -u default bash -c 'source /opt/app-root/bin/activate && molecule ${moleculeBaseConfigFlag} ${moleculeDebugFlag} test -s ${localScenario}'"
		def test_status = null
                if (params.concurrency == false) {
                    // acquire lock per role name
                    lock("MoleculeAnsibleRole_${roleName}") {
                        // dont fail on error, we'll be UNSTABLE with failed tests
                        ansiColor('xterm') {
                            test_status = sh returnStatus:true, script: moleculeCmd
                        }
                    }
                }
                else {
                    // dont fail on error, we'll be UNSTABLE with failed tests
                    ansiColor('xterm') {
                        test_status = sh returnStatus:true, script: moleculeCmd
                    }
                }
                // see multiline indent https://stackoverflow.com/questions/19882849/strip-indent-in-groovy-multiline-strings
                echo "==================== END scenario ${localScenario} ===================="
                // initial check for stage results, we MUST produce a junit output

                // exit code is always 1, wether test fail, or ansible run exploded
                if(test_status != 0) {
                  warning("Scenario ${localScenario} returned non-zero exit code")
                }

                // collect the test results from all scenarios, fail if no result
                junit keepLongStdio: true, testResults: "**/molecule/${localScenario}/junit.xml"
            }
        }
    }

    // run collected scenario stages
    parallel parallelStages
}
