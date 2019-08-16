package vbc.cicd
import java.lang.UnsupportedOperationException

class CredentialsBuilder {

    class CredentialsDomain {

        String name
        String description
        String includes
        String excludes

        CredentialsDomain(String name, String description, String includes, String excludes) {
            this.name = name
            this.description = description
            this.includes = includes
            this.excludes = excludes
        }

        CredentialsDomain(Map domain) {
            this.name = domain.name
            this.description = domain.description
            this.includes = domain.includes
            this.excludes = domain.excludes
        }

        Closure asDsl() {
            return {
                name(this.get('name'))
                description(this.get('description'))
                specifications {
                    hostnameSpecification {
                        // A comma separated whitelist of hostnames.
                        includes(this.get('includes', ""))
                        // A comma separated blacklist of hostnames.
                        excludes(this.get('excludes', ""))
                    }
                }
            }
        }
    }


    static CredentialsDomain DEFAULT_DOMAIN = new CredentialsDomain(null)

    CredentialsDomain domain = null
    List<Credentials> credentials = []

    CredentialsBuilder(Map credentialsData) {

        this.domain = credentialsData.get('domain', DEFAULT_DOMAIN)

        for (Map cred in credentialsData.credentials) {
            this.credentials.add(this._generateFromMap(cred))
        }
    }

    private Credentials _generateFromMap(Map c) {

        switch (c.type) {
            case 'usernamepassword':
                return new UsernamePasswordCredentials(c.id, c.description, c.get('scope'), c.username, c.password)
            default:
                throw UnsupportedOperationException('cannot handle credentials of type ${c.type}')
        }
    }

    Closure asDsl() {
        return {
            domain this.domain.asDsl()
            credentials {
                for (Credentials c in this.credentials) {

                }
            }
        }
    }
}
