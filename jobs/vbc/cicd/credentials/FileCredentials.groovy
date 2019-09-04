package vbc.cicd.credentials

class FileCredentials extends Credentials {

    String content

    FileCredentials(String id, String description, String scope ='global', String content, Map data) {
        super(id, description, scope)
        this.content = content
    }

    @Override
    Closure asDsl() {
        throw new UnsupportedOperationException("FileCredentials are not implemented yet")
        return {
            scope(this.scope)
            id(this.id)
            description(this.description)
            file(null)
            fileName('UNDEFINED.txt')
            secretBytes()
        }
    }
}
