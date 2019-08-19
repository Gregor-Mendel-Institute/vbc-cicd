package vbc.cicd.credentials

class UsernamePasswordCredentials extends Credentials {
    String username
    String password

    UsernamePasswordCredentials(String id, String description, String scope, String username, String password) {
        super(id, description, scope)
        this.username = username
        this.password = password
    }

    @Override
    Closure asDsl() {
        return {
            usernamePasswordCredentialsImpl {
                // Determines where this credential can be used.
                scope(this.scope)
                // An internal unique ID by which these credentials are identified from jobs and other configuration.
                id(this.id)
                // An optional description to help tell similar credentials apart.
                description(this.description)
                // The username.
                username(this.username.toString())
                // The password. FIXME this should be a placeholders, as needs updating from 1Pass??
                password(this.password.toString())
            }
        }
    }
}