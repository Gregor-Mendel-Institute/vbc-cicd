package vbc.cicd.credentials

class StringCredentials extends Credentials {

    String secret = null

    StringCredentials(String id, String description, String scope ='global', String secret) {
        super(id, description, scope)
        this.secret = secret
    }

    @Override
    Closure asDsl() {
        return {
            // Determines where this credential can be used.
            scope(this.scope)
            // An internal unique ID by which these credentials are identified from jobs and other configuration.
            id(this.id)
            // An optional description to help tell similar credentials apart.
            description(this.description)
            // The username.
            // The password. FIXME this should be a placeholders, as needs updating from 1Pass??
            secret(this.secret.toString())
        }
    }
}
