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
                    /** not quite ready
                    gitHubForkDiscovery {
                        strategyId(1)
                        gitHubTrustContributors()
                    }
                    originPullRequestDiscoveryTrait {
                        strategyId(1)
                    }
                    */

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
        /*
        <org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait>
          <strategyId>1</strategyId>
        </org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait>

        <org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait>
          <strategyId>1</strategyId>
          <trust class="org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustContributors"/>
        </org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait>

        <org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait>
          <strategyId>1</strategyId>
        </org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait>

        <org.jenkinsci.plugins.github__branch__source.TagDiscoveryTrait/>

         */
        return { it ->
            def traits = it / navigators /  'org.jenkinsci.plugins.github__branch__source.GitHubSCMNavigator' / traits
            traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
                strategyId(1)
            }
            traits << 'org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait' {
                strategyId(1)
                trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustContributors')
                // see also https://github.com/jenkinsci/github-branch-source-plugin/blob/github-branch-source-2.5.6/src/main/java/org/jenkinsci/plugins/github_branch_source/ForkPullRequestDiscoveryTrait.java#L301
                //gitHubTrustNobody()
                // gitHubTrustPermissions() // users with r/w or admin
                //gitHubTrustContributors() // any collaborators (also with r/o acces
            }
        }
    }
}