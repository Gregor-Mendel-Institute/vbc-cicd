package vbc.cicd

import vbc.cicd.credentials.Credentials
import vbc.cicd.credentials.CredentialsDomain
import vbc.cicd.credentials.SSHPrivateKeyCredentials
import vbc.cicd.credentials.UsernamePasswordCredentials

class CredentialsBuilder {

    CredentialsDomain domain = CredentialsDomain.DEFAULT_DOMAIN
    List<Credentials> credentials = []

    CredentialsBuilder(Map credentialsData) {

        Map domainData = credentialsData.get('domain')
        // update with specific domain data if we have it
        if (domainData) {
            this.domain = new CredentialsDomain(domainData)
        }

        for (Map cred in credentialsData.credentials) {
            this.credentials.add(this._generateFromMap(cred))
        }
    }

    private Credentials _generateFromMap(Map c) {

        switch (c.type) {
            case 'usernamepassword':
                return new UsernamePasswordCredentials(c.id, c.description, c.get('scope'), c.username, c.password)
            case 'sshprivatekey':
                return new SSHPrivateKeyCredentials(c.id, c.description, c.get('scope'), c.username, c.privatekey, c.password)
            default:
                throw UnsupportedOperationException('cannot handle credentials of type ${c.type}')
        }
    }

    // render complete credentials settings in DSL
    Closure asDsl() {

        return {
            domain this.domain.asDsl()

            credentials {
                for (Credentials c in this.credentials) {
                    switch (c.class) {
                        case UsernamePasswordCredentials.class:
                            usernamePasswordCredentialsImpl c.asDsl()
                            break

                        case SSHPrivateKeyCredentials.class:
                            basicSSHUserPrivateKey c.asDsl()
                            break

                        default:
                            throw new UnsupportedOperationException("cannot handle class " + c.class)
                    }

                }
            }
        }
    }
}
