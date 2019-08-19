package vbc.cicd
import java.lang.UnsupportedOperationException

import vbc.cicd.*

class CredentialsBuilder {





    CredentialsDomain domain = null
    List<Credentials> credentials = []

    CredentialsBuilder(Map credentialsData) {

        this.domain = credentialsData.get('domain', CredentialsDomain.DEFAULT_DOMAIN)

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
                    c.asDsl()
                }
            }
        }
    }
}
