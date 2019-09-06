package vbc.cicd.credentials

class SSHPrivateKeyCredentials extends Credentials {
    String username
    String privatekey
    String password
    //Secret keySecret

    SSHPrivateKeyCredentials(String id, String description, String scope, String username, String privatekey, String password = null) {
        super(id, description, scope)
        this.username = username
        //FIXME private key from 1pass needs sanitation
        this.privatekey = privatekey.replace(' RSA PRIVATE ', 'RSAPRIVATE').replace(' ', '\n').replace('RSAPRIVATE', ' RSA PRIVATE ')
        this.password = password
        //throw new UnsupportedOperationException("Seed SSH creds currently not possible because of  https://issues.jenkins-ci.org/browse/JENKINS-57435")
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
            // also https://issues.jenkins-ci.org/browse/JENKINS-34173
            // FIXME this is currently broken, in ssh-credentials > 1.15
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