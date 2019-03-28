import groovy.transform.Field

// config values for pushing/pulling the molecule test environment container image


def call(Map settings = [imageTag: "local_molecule:vbc"]) {


    //def registry = "docker.artifactory.imp.ac.at"
    //def registryCredentialsId = "jenkins_artifactory_creds"
    //def imageTag = "${registry}/it/molecule:vbc"

    println("Molecule image settings:")
    println("  imageTag: ${settings.imageTag}")

    // copy Dockerfile to here
    def moleculeDockerfile = libraryResource 'Dockerfile.molecule'
    writeFile file: "Dockerfile", text: moleculeDockerfile

        // now we only build locally, no need for credentials
        sh "docker build --build-arg MOLECULE_UID=\$(id -u svc_jenkins_docker) --build-arg JENKINS_GID=\$(id -g svc_jenkins_docker) --build-arg DOCKER_GID=\$(getent group docker | cut -d: -f3) -t ${settings.imageTag} ."
}

return this
