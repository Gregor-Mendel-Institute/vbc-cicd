def bitbucketUrl = "https://bitbucket.imp.ac.at"
def bitbucketCredentials = "svc-bitbucket-access-user-passwd"
def jobFolder = "SD"
def libraryVersion = "job_dsl_setup"
def bitbucketOrgs = [
        [owner: "IAB", name:"IT Ansible Baseline", descrition: "Baseline components for System Deployment" ],
        [owner: "IAA", name:"IT Ansible Apps", descrition: "Application components for System Deployment" ],
        [owner: "IAO", name:"IT Ansible Ops", descrition: "Operations Tasks and Tooling" ],
        [owner: "CLIP", name:"CLIP Ansible Roles", descrition: "CLIP related Ansible roles" ]
]

def libBranchName = "job_dsl_setup"
def libGitRepo = "ssh://git@bitbucket.imp.ac.at:7991/iab/ansible-cicd.git"
def libGitCredentialsId = "dd2eddb1-0e79-4acb-9dca-5fe6b4ba25b3"

def orgJobs = []

for (org in bitbucketOrgs) {
    def orgJob = organizationFolder("${jobFolder}/${org.owner}") {
        displayName("${org.name}")
        description("${org.descrition}")
        triggers {
            bitbucketPush()
            periodicFolderTrigger {
                // The maximum amount of time since the last indexing that is allowed to elapse before an indexing is triggered.
                interval("60")
            }
        }
        authorization {
            // set permissions for generated jobs
            permissionAll('adm_ebirn')

        }
        organizations {
            bitbucket {
                repoOwner("${org.owner}")
                serverUrl("$bitbucketUrl")

                // credentials for API access and checkouts
                credentialsId("$bitbucketCredentials")

                // this one is deprecated
                //autoRegisterHooks(true)
                traits {
                    sourceWildcardFilter {
                        // Space-separated list of project name patterns to consider.
                        includes("role-*")
                        excludes("")
                    }
                }
            }
        }

        projectFactories {
            workflowMultiBranchProjectFactory {
                // Relative location within the checkout of your Pipeline script.
                scriptPath("Jenkinsfile")
            }
        }

        properties {
            folderLibraries {
                libraries {
                    libraryConfiguration {
                        // An identifier you pick for this library, to be used in the @Library annotation.
                        name("ansible-cicd")
                        defaultVersion(libBranchName) // this is the git tag, make sure to have branch/tag discovery

                        retriever {
                            modernSCM {
                                scm {
                                    git {
                                        remote(libGitRepo)
                                        credentialsId(libGitCredentialsId)

                                        traits {
                                            gitBranchDiscovery()
                                            gitTagDiscovery()
                                        }
                                    }
                                }
                            }
                        }
                        /**
                            modernSCM {
                                // can I use the value in project.scm ??
                                scm {
                                    bitbucket {
                                        repoOwner("${org.owner}")
                                        serverUrl("${bitbucketUrl}")
                                        repository("ansible-vbc.ansible.cicd")
                                        // credentials for API access and checkouts
                                        credentialsId("${bitbucketCredentials}")
                                    }
                                }

                            }
                            */

                        // If checked, scripts may select a custom version of the library by appending @someversion in the @Library annotation.
                        //allowVersionOverride(boolean value)

                        // A default version of the library to load if a script does not select another.
                        //defaultVersion(String value)

                        // If checked, scripts will automatically have access to this library without needing to request it via @Library.
                        implicit(true)

                        // If checked, any changes in the library will be included in the changesets of a build.
                        includeInChangesets(true)
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

        }
    }
    orgJobs.add(orgJob)
}

//FIXME consider this https://plugins.jenkins.io/pipeline-multibranch-defaults
// for defaulting Jenkinsfile(s) in those jobs
// put Jenkinsfile in config files https://jenkins-impimba-1.imp.ac.at/plugin/job-dsl/api-viewer/index.html#method/javaposse.jobdsl.dsl.DslFactory.configFiles
// use defaults job factors: https://jenkins-impimba-1.imp.ac.at/plugin/job-dsl/api-viewer/index.html#method/javaposse.jobdsl.dsl.helpers.workflow.BranchProjectFactoryContext.pipelineBranchDefaultsProjectFactory

// alternative approach would be:

// minimalistic Jenkinsfile, importing all logic via shared Library, like here
// https://github.com/jenkinsci/job-dsl-plugin/blob/master/Jenkinsfile and ... the library implementation below
// https://github.com/jenkins-infra/pipeline-library/tree/master/vars



// why does print not work?
// https://github.com/jenkinsci/job-dsl-plugin/wiki/Job-DSL-Commands#logging
//println('Hello from a Job DSL script!')
for (job in orgJobs) {
    // sandbox does not like us to do that
    //println("oh my job: $job")
}