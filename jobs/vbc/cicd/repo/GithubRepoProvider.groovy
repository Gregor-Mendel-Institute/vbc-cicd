package vbc.cicd.repo

class GithubRepoProvider extends RepoProvider {

    def owner = null

    def GithubRepoProvider(Map org) {
        super(org)
        this.owner = org.owner
        // github quota should have some more time
        this.scanIntervalMinutes = 120
    }

    @Override
    def Closure getScmDefinition() {
        return null
    }

    @Override
    Closure asOrganizations() {
        return {
            github {
                apiUri(this.url)
                credentialsId(this.credentials)
                repoOwner(this.owner)

                traits {
                    if (this.checkoutCredentials) {
                        gitHubSshCheckout {
                            // Credentials used to check out sources.
                            credentialsId(this.checkoutCredentials)
                        }
                    }
                    gitHubBranchDiscovery {
                        // Determines which branches are discovered.
                        strategyId(1)
                    }
                    gitHubTagDiscovery()

                    sourceWildcardFilter {
                        // Space-separated list of project name patterns to consider.
                        includes(this.includes)
                        // Space-separated list of project name patterns to ignore even if matched by the includes list.
                        excludes(this.excludes)
                    }
                }
            }

        }

    }

    @Override
    Closure repoTriggers() {
        return  {
            periodicFolderTrigger {
                // The maximum amount of time since the last indexing that is allowed to elapse before an indexing is triggered.
                // rescan every 15 mins
                interval("${this.scanIntervalMinutes}")
            }
            // for the jobs underneath?
            // githubPush()

        }
    }

    @Override
    Closure configure() {
        return {

        }
    }
}