package vbc.cicd

import groovy.transform.InheritConstructors

abstract class RepoProvider {

    String url = null
    String credentialsId = null
    String checkoutCredentialsId = null

    String includes = ""
    String excludes = ""

    RepoProvider(Map org) {
        Map provider = org.jenkins.provider

        this.url = provider.url
        this.credentialsId = provider.credentialsId
        this.checkoutCredentialsId = provider.get('checkoutCredentialsId', null)

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

class BitbucketRepoProvider extends RepoProvider {

    def owner = null

    BitbucketRepoProvider(Map org) {
        super(org)
        this.owner = org.owner
    }

    @Override
    Closure getScmDefinition() {
        return null
    }

    @Override
    Closure asOrganizations() {
        return { job ->
            bitbucket {
                repoOwner(this.owner)
                serverUrl(this.url)

                // credentials for API access
                credentialsId(this.credentialsId)

                // this one is deprecated
                //autoRegisterHooks(true)
                traits {

                    sourceWildcardFilter {
                        // Space-separated list of project name patterns to consider.
                        includes(this.includes)
                        excludes(this.excludes)
                    }
                }
            }
        }
    }

    @Override
    Closure repoTriggers() {
        return {
            periodicFolderTrigger {
                // The maximum amount of time since the last indexing that is allowed to elapse before an indexing is triggered.
                // rescan every 15 mins
                interval("60")
            }
            //periodic(60) // DEPRECATED
            bitbucketPush()
        }
    }
}

class GithubRepoProvider extends RepoProvider {

    def owner = null

    def GithubRepoProvider(Map org) {
        super(org)
        this.owner = org.owner
    }

    @Override
    def Closure getScmDefinition() {
        return null
    }

    @Override
    Closure repoTriggers() {
        return  {
            triggers {
                periodicFolderTrigger {
                    // The maximum amount of time since the last indexing that is allowed to elapse before an indexing is triggered.
                    // rescan every 15 mins
                    interval("60")
                }
                githubPush()
            }
        }
    }
}

@InheritConstructors
class SingleRepoProvider extends RepoProvider {

    @Override
    Closure getScmDefinition() {
        return null
    }

    @Override
    Closure repoTriggers() {
        return
    }
}
