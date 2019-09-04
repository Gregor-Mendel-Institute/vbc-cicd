package vbc.cicd.credentials

class SSHPrivateKeyCredentials extends Credentials {
    String username
    String privatekey
    String password

    SSHPrivateKeyCredentials(String id, String description, String scope, String username, String privatekey, String password = null) {
        super(id, description, scope)
        this.username = username
        //FIXME private key from 1pass needs sanitation
        this.privatekey = privatekey.replace(' PRIVATE KEY ', 'PRIVATEKEY').replace(' ', '\n').replace('PRIVATEKEY', ' PRIVATE KEY ')
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
            //privateKeySource {
            //}
            // see https://issues.jenkins-ci.org/browse/JENKINS-57435
            privateKeySource {
                directEntryPrivateKeySource {
                    privateKey(this.privatekey)
                }
            }

            // An optional description to help tell similar credentials apart.
            description(this.description)
        }
    }
}