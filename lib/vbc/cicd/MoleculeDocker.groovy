package vbc.cicd

class MoleculeDocker {

    String registry = "docker.artifactory.imp.ac.at"
    String registryCredentialsId = "jenkins_artifactory_creds"
    String imageTag = "${registry}/it/molecule:vbc"

    def buildImage() {
        // setup Docker image for the molecule builds
        //FIXME how to version?
        //Note: we can pre-build here, assuming that Jenkins will be running under same UID everywhere, (as connected via Jenkins svc user)
        docker.withRegistry("https://${registry}", registryCredentialsId) {
            def moleculeImage = docker.build(imageTag, "-f resources/Dockerfile.molecule .")
            moleculeImage.push()
        }
    }


    def agent() {
        docker {
            label 'docker'
            image "${imageTag}"
            registryUrl "https://${registry}"
            registryCredentialsId "${registryCredentialsId}"

            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }

    }
}
