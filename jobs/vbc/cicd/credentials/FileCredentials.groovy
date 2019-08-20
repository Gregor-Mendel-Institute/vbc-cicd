package vbc.cicd.credentials

class FileCredentials extends Credentials {

    String content

    FileCredentials(String id, String description, String scope ='global', String content) {
        super(id, description, scope)
        this.content = content
    }

    @Override
    Closure asDsl() {
        throw new UnsupportedOperationException("FileCredentials are not implemented yet")
        return {

        }
    }
}
