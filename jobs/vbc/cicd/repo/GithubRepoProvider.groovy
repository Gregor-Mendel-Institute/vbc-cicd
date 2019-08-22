package vbc.cicd.repo

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

    @Override
    Closure configure() {
        return null
    }
}