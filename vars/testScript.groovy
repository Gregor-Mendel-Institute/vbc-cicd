
// return implementation of container image test script run

def call(String scriptPath, String testResultPattern=null, String imageName = null) {
    return { defaultImageName, allBuilds ->
        // use base image if not explicitly set
        if (!imageName) {
            imageName = defaultImageName
        }
        allBuilds[imageName].image.inside() {
            echo "running tests for ${imageName} as ${scriptPath}"
            sh 'env'
            sh 'pwd'

            // dont fail on error, we'll be UNSTABLE with failed tests
            def test_status = sh script: scriptPath, returnStatus:true, label: "testing"

            if (testResultPattern) {
                // collect test results
                // https://stackoverflow.com/questions/39920437/how-to-access-junit-test-counts-in-jenkins-pipeline-project
                junit keepLongStdio: true, testResults: testResultPattern
            }
            else if (tes_status) {
                // failed test is unstable, not failed build
                unstable('no test result files specified: see testScript(String scriptPath, String testResultPattern), test script returned non-zero exit code.')
            }
        }
    }
}
