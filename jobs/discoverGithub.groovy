
def orgJobs = []
def cicdLib = cicdLibConfig
// groovy 2.4
// for (org in discoverOrgs.findAll({ it.provider.type == 'bitbucket' }) ) {
for (org in discoverOrgs) {
    if (org.provider.type != 'github')
        continue

    def buildTags = org.buildTags
    def provider = org.provider
    def folder = org.get('folder', org.owner)
    def orgJob = organizationFolder("${folder}") {
        displayName("${org.name}")
        description("${org.description}")
        triggers {
            periodicFolderTrigger {
                // The maximum amount of time since the last indexing that is allowed to elapse before an indexing is triggered.
                // rescan every 15 mins
                interval("60")
            }
        }
        authorization {
            // set permissions for generated jobs
            // total admin access
            permissionAll('admin_gods')
            // add specific permissions if they were configured
            for (perm in org.permissions) {
              permissions(perm.subject, perm.privileges)
            }

        }
        organizations {
            github {
                repoOwner("${org.owner}")
                apiUri("${org.provider.url}")

                // credentials for API access and checkouts
                credentialsId("${org.provider.credentials}")
                // not part of github?
                // checkoutCredentialsId("${org.provider.checkoutCredentials}")

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
           // github specific from https://stackoverflow.com/questions/51747187/jenkins-configure-branch-discovery-for-github-organization-with-job-dsl
           def traits = it / navigators / 'org.jenkinsci.plugins.github__branch__source.GitHubSCMNavigator' / traits
           traits << 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait' {
               strategyId 1
           }
           traits << 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait' {
               strategyId 2
               trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustContributors')
           }
           traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
               strategyId 2
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

