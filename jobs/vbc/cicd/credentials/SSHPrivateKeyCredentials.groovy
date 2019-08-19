package vbc.cicd.credentials

class SSHPrivateKeyCredentials extends Credentials {
    String username
    String privatekey
    String password

    SSHPrivateKeyCredentials(String id, String description, String scope, String username, String secretkey, String password = null) {
        super(id, description, scope)
        this.username = username
        this.privatekey = secretkey
        this.password = password
    }

    @Override
    Closure asDsl() {
        // as param to basicSSHUserPrivateKey
        return {
            // Determines where this credential can be used.
            scope(this.scope)
            // An internal unique ID by which these credentials are identified from jobs and other configuration.
            id(this.id)
            username(this.username)
            privateKeySource {
            }
            passphrase(this.password)
            // An optional description to help tell similar credentials apart.
            description(this.description)
        }
    }
}