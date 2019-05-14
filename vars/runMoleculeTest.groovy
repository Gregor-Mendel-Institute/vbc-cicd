
def call(String roleName, Map params=[debug: false, scenarios: ["default"]]) {



    echo "running molecule in this env:"
    sh "id"
    sh "ansible --version"
    sh "molecule --version"

    echo "running molecule scenarios: ${params.scenarios}"

    // write complete env for molecule to interpolate


    // set debug flag dependeing on build params
    def moleculeDebugFlag = params.debug ? "--debug" : ""

    for (scenario in params.scenarios) {
        stage("${scenario} scenario") {
            // sanitize unique id for save container usage: replace " ", "." and "_" with the "-"

            // this will work with groovy 2.5.0 and above
            //.digest("SHA-1").take(16)
            def longUniqueTag = "${scenario}-${env.BUILD_TAG}-${env.GIT_COMMIT}".replaceAll("[ ._]", "-")
            def tagFile = "unique_job_tag.${scenario}"
            sh "echo '${longUniqueTag}' > ${tagFile}"

            // shorten ID, as it will be used also as default hostname in container (maxsize hostname is 64 chars)
            def uniqueTag = sha1(file: tagFile).take(16)
            env.INSTANCE_ID = uniqueTag

            echo "==================== BEGIN scenario ${scenario} ===================="
            sh 'echo "my instance id is $INSTANCE_ID"'
            // docker older 17.12 does not like dir() {  } here, we might run on RHEL with old docker...
            // sudo is necessary, as we need a new login shell with all our group memberships
            sh "cd ${roleName} && sudo --preserve-env -u molecule molecule ${moleculeDebugFlag} test -s ${scenario}"
            // see multiline indent https://stackoverflow.com/questions/19882849/strip-indent-in-groovy-multiline-strings
            echo "==================== END scenario ${scenario} ===================="
        }
    }

    // collect the test results from all scenarios
    junit keepLongStdio: true, testResults: '**/molecule/*/junit.xml'

}
