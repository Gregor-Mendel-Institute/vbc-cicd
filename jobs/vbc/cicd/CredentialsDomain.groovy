package vbc.cicd



class CredentialsDomain {
    static CredentialsDomain DEFAULT_DOMAIN = new CredentialsDomain(null)
    
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
