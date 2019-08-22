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
                credentialsId(this.credentials)

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
            bitbucketPush()
        }
    }

    @Override
    Closure configure() {
        return { it ->
            def scm_traits = it / navigators / 'com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMNavigator' / traits

            // discover all branches
            scm_traits << 'com.cloudbees.jenkins.plugins.bitbucket.BranchDiscoveryTrait' {
                strategyId('3')
            }

            // discover PRs in the same repo
            scm_traits << 'com.cloudbees.jenkins.plugins.bitbucket.OriginPullRequestDiscoveryTrait' {

                // try both: the just the cloned branch AND the cloned branch merged on top of the destination branch of PR
                strategyId('3')
            }

            // discover PRs in cloned repos
            scm_traits << 'com.cloudbees.jenkins.plugins.bitbucket.ForkPullRequestDiscoveryTrait' {

                // try both: the just the cloned branch AND the cloned branch merged on top of the destination branch of PR
                strategyId('3')

                // trust changed Jenkinsfiles from Everyone (from clones in other Accounts), if not trusted will require+use Jenkinsfile in origin
                trust(class: 'com.cloudbees.jenkins.plugins.bitbucket.ForkPullRequestDiscoveryTrait$TrustEveryone')
            }

            // discover tags, if we release versions
            scm_traits << 'com.cloudbees.jenkins.plugins.bitbucket.TagDiscoveryTrait' {
            }

            if (this.checkoutCredentials) {
                scm_traits << 'com.cloudbees.jenkins.plugins.bitbucket.SSHCheckoutTrait' {
                    // use ssh with these credentials for the actual checkout
                    credentialsId(this.checkoutCredentials)
                }
            }

        }
    }

}