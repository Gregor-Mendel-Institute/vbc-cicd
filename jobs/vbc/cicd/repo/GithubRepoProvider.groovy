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
    Closure asOrganizations() {
        return {
            github {
                apiUri(this.url)
                credentialsId(this.credentials)
                repoOwner(this.owner)
            }

        }

    }

    @Override
    Closure repoTriggers() {
        return  {
            periodicFolderTrigger {
                // The maximum amount of time since the last indexing that is allowed to elapse before an indexing is triggered.
                // rescan every 15 mins
                interval("60")
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