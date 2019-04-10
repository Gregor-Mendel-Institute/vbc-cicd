

// just return a dict with the registry settings
def call(Map params = [:]) {

    def registry = "docker.artifactory.imp.ac.at"

    def settings = [
        registry: "${registry}",
        registryUrl: "https://${registry}",
        registryCredentialsId: "jenkins_artifactory_creds",
        imageTag: "local_molecule:vbc"
        //imageTag: "${registry}/it/molecule:vbc"
    ]


    return settings
}


return this