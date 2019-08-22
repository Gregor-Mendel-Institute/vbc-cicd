package vbc.cicd.repo

abstract class RepoProvider {

    String url = null
    String credentials = null
    String checkoutCredentials = null

    String includes = ""
    String excludes = ""

    RepoProvider(Map org) {
        Map provider = org.jenkins.provider

        this.url = provider.url
        this.credentials = provider.credentials
        this.checkoutCredentials = provider.get('checkoutCredentials', null)

        this.includes = provider.get("includes", "")
        this.excludes = provider.get("excludes", "")

    }

    abstract Closure getScmDefinition()
    abstract Closure repoTriggers()

    // by default do nothing, i.e. empty closure
    Closure asOrganizations() {
        return {
        }
    }
    // factory method to create instance
    static RepoProvider newRepoProvider(Map org) {
        def type = org.jenkins.provider.type

        switch (type) {
            case 'bitbucket':
                return new BitbucketRepoProvider(org)
            case 'github':
                return new GithubRepoProvider(org)
            case 'single':
                return new SingleRepoProvider(org)
            default:
                throw new UnsupportedOperationException("repository provider of type ${type} is not supported.")
        }

    }
}


