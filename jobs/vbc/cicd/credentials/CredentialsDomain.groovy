package vbc.cicd.credentials

class CredentialsDomain {
    static final CredentialsDomain DEFAULT_DOMAIN = new CredentialsDomain(name: null, description: null, includes: null, excludes: null)

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
        this.description = domain.get('description', '')
        this.includes = domain.get('includes', '')
        this.excludes = domain.get('excludes', '')
    }

    Closure asDsl() {
        return {
            name(this.name)
            description(this.description)

            if (this != CredentialsDomain.DEFAULT_DOMAIN) {
                specifications {
                    hostnameSpecification {
                        // A comma separated whitelist of hostnames.
                        includes(this.includes)
                        // A comma separated blacklist of hostnames.
                        excludes(this.excludes)
                    }
                }
            }
        }
    }
}
