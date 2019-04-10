
def cicdLib = cicdLibConfig

def cookiecutterRepo = cookiecutterRepoConfig

multibranchPipelineJob("CookiecutterMolecule") {

    displayName("Molecule Cookiecutter")
    description("test the cookiecutter template for creating new ansible roles")

    triggers {
        bitbucketPush()
        periodicFolderTrigger {
            // The maximum amount of time since the last indexing that is allowed to elapse before an indexing is triggered.
            // rescan every 15 mins
            interval("15")
        }
    }

    // use cookiecutter repo for pipeline setup, shallow checkout
    // set shared libraries to use i.e. notifications
    // we must also ensure build container
    branchSources {

        branchSource {
            source {
// Discovers branches and/or pull requests from a specific repository in either Bitbucket Cloud or a Bitbucket Server instance.
                bitbucket {
                    serverUrl(cookiecutterRepo.url)
                    // Specify the name of the Bitbucket Team or Bitbucket User Account.
                    repoOwner(cookiecutterRepo.repoOwner)
                    // The repository to scan.
                    repository(cookiecutterRepo.repoName)

                    // credentials for API access and checkouts
                    credentialsId(cookiecutterRepo.credentials)

                    traits {
                        wipeWorkspaceTrait()
                        //localBranchTrait()
                        //pruneStaleBranchTrait()
                    }
                }
            }

            buildStrategies {

                buildChangeRequests {
                    // If the change request / pull request is a merge, there are two reasons for a revision change: The origin of the change request may have changed The target of the change request may have changed Selecting this option will ignore revision changes if the only difference between the last built revision is the target of the change request.
                    ignoreTargetOnlyChanges(false)

                    // Some sources can permit change request / pull request from external entities.
                    ignoreUntrustedChanges(false)
                }
                // Builds regular branches whenever a change is detected.
                buildRegularBranches()
            }
        }
    }

    factory {
        workflowBranchProjectFactory {
            // Relative location within the checkout of your Pipeline script.
            scriptPath("Jenkinsfile")
        }
    }

    orphanedItemStrategy {
        // Trims dead items by the number of days or the number of items.
        discardOldItems {
            // Sets the number of days to keep old items.
            daysToKeep(30)
            // Sets the number of old items to keep.
            numToKeep(10)
        }

        defaultOrphanedItemStrategy {
            pruneDeadBranches(true)
            daysToKeepStr("10")
            numToKeepStr("10")
        }
    }


    // extra configs
    configure {
        def scm_traits = it / sources / data / 'jenkins.branch.BranchSource' / 'source'/  traits

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

        scm_traits << 'com.cloudbees.jenkins.plugins.bitbucket.SSHCheckoutTrait' {
            // use ssh with these credentials for the actual checkout
            credentialsId(cookiecutterRepo.sshCredentials)
        }
    }

    properties {
        folderLibraries {
            libraries {
                libraryConfiguration {
                    // An identifier you pick for this library, to be used in the @Library annotation.
                    name(cicdLib.name)

                    // A default version of the library to load if a script does not select another.
                    defaultVersion(cicdLib.version) // this is the git tag, make sure to have branch/tag discovery

                    // If checked, scripts may select a custom version of the library by appending @someversion in the @Library annotation.
                    //allowVersionOverride(boolean value)
                    // If checked, scripts will automatically have access to this library without needing to request it via @Library.
                    implicit(true)

                    // If checked, any changes in the library will be included in the changesets of a build.
                    includeInChangesets(true)

                    retriever {
                        modernSCM {
                            scm {
                                git {
                                    remote(cicdLib.gitRepo)
                                    credentialsId(cicdLib.gitCredentialsId)

                                    traits {
                                        gitBranchDiscovery()
                                        gitTagDiscovery()
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}
