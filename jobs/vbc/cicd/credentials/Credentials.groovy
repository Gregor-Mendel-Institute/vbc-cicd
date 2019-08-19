package vbc.cicd.credentials

abstract class Credentials {

    String id
    String description
    String scope = 'GLOBAL'

    Credentials(String id, String description, String scope ='global') {
        this.id = id
        this.description = description
        this.scope = scope
    }

    abstract Closure asDsl()
}


