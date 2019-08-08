
def orgJobs = []
// groovy 2.4
// for (org in discoverOrgs.findAll({ it.provider.type == 'bitbucket' }) ) {
for (org in discoverOrgs) {
    if (org.provider.type != 'bitbucket')
        continue

    def buildTags = org.buildTags
    def provider = org.provider
    def folder = org.get('folder', org.owner)
    def orgJob = organizationFolder("${folder}") {
        displayName("${org.name}")
        description("${org.description}")
        triggers {
            bitbucketPush()
            periodicFolderTrigger {
                // The maximum amount of time since the last indexing that is allowed to elapse before an indexing is triggered.
                // rescan every 15 mins
                interval("60")
            }
        }
        // authorization {
        //    // jobUtils.buildPermissions(org.permissions)
        // }
        organizations {
            bitbucket {
                repoOwner("${org.owner}")
                serverUrl("${org.provider.url}")

                // credentials for API access
                credentialsId("${org.provider.credentials}")

                // this one is deprecated
                //autoRegisterHooks(true)
                traits {
                    sourceWildcardFilter {
                        // Space-separated list of project name patterns to consider.
                        includes("${org.includePattern}")
                        excludes("${org.excludePattern}")
                    }
                }
            }
        }

        projectFactories {
            workflowMultiBranchProjectFactory {
                // Relative location within the checkout of your Pipeline script.
                scriptPath("Jenkinsfile.vbc")
                scriptPath("Jenkinsfile")
            }
        }

        properties {
            folderLibraries {
                libraries {
                    libraryConfiguration {
                        // An identifier you pick for this library, to be used in the @Library annotation.
                        name("${cicdLib.name}")

                        // A default version of the library to load if a script does not select another.
                        defaultVersion("${cicdLib.version}") // this is the git tag, make sure to have branch/tag discovery

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
                                        remote(cicdLib.provider.url)
                                        credentialsId(cicdLib.provider.checkoutCredentials)

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

        // see https://issues.jenkins-ci.org/browse/JENKINS-48360
        configure {
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

            scm_traits << 'com.cloudbees.jenkins.plugins.bitbucket.SSHCheckoutTrait' {
                // use ssh with these credentials for the actual checkout
                credentialsId("${provider.checkoutCredentials}")
            }

           def buildStrategies = it / buildStrategies
           buildStrategies << 'jenkins.branch.buildstrategies.basic.SkipInitialBuildOnFirstBranchIndexing' {
           }
           buildStrategies << 'jenkins.branch.buildstrategies.basic.BranchBuildStrategyImpl' {
           }

           if (buildTags) {
               // automatically build tags newer than 7 days (604800000 millis)
               buildStrategies << 'jenkins.branch.buildstrategies.basic.TagBuildStrategyImpl' {
                   atLeastMillis(1)
                   atMostMillis(604800000)
               }
           }

        }
    }
    orgJobs.add(orgJob)
}

