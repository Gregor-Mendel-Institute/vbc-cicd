
// this is our externalized way of sending build notifications

def call(Map currentBuild, Map params = [:]) {

    echo "sending build notifications"
    echo "NOTIFICATION TIME"
    echo "got build: ${currentBuild}"


    // all the build causes
    //def causes = currentBuild.rawBuild.getCauses()

    // Get a specific Cause type (in this case the user who kicked off the build),
    // if present.
    //def specificCause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)

    // https://support.cloudbees.com/hc/en-us/articles/217630098-How-to-access-Changelogs-in-a-Pipeline-Job-

    echo "current build changeset(s): ${currentBuild.changeSets}"

    // send the actual message, see https://jenkins.io/doc/pipeline/steps/Office-365-Connector/
    //office365ConnectorSend
}

return this
