package vbc.cicd.repo

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