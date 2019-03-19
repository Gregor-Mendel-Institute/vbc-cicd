// config values for pushing/pulling the molecule test environment container image




def call(Map params = [:]) {

    def dockerSettings = moleculeDockerSettings()

    def dockerAgent = null

    agent {
        docker {
            image "${dockerSettings.imageTag}"
            args '-v /var/run/docker.sock:/var/run/docker.sock'
            reuseNode true
        }
    }

    //return dockerAgent
}

return this