
def call(String roleName, Map params=[debug: false, scenarios: ["default"], concurrency: true]) {

    echo "running molecule in this env:"
    sh "id"
    sh "ansible --version"
    sh "molecule --version"

    echo "running molecule scenarios: ${params.scenarios}"

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
                env.INSTANCE_ID = uniqueTag

                echo "==================== BEGIN scenario ${localScenario} ===================="
                sh 'echo "my instance id is $INSTANCE_ID"'
                // docker older 17.12 does not like dir() {  } here, we might run on RHEL with old docker...
                // sudo is necessary, as we need a new login shell with all our group memberships
                def moleculeCmd = "cd ${roleName} && sudo --preserve-env -u molecule molecule ${moleculeDebugFlag} test -s ${localScenario}"
                if (params.concurrency == false) {
                    // acquire lock per role name
                    lock("MoleculeAnsibleRole_${roleName}") {
                        sh moleculeCmd
                    }
                }
                else {
                    sh moleculeCmd
                }
                // see multiline indent https://stackoverflow.com/questions/19882849/strip-indent-in-groovy-multiline-strings
                echo "==================== END scenario ${localScenario} ===================="
            }
        }
    }

    // run collected scenario stages
    parallel parallelStages

    // collect the test results from all scenarios
    junit keepLongStdio: true, testResults: '**/molecule/*/junit.xml'

}
