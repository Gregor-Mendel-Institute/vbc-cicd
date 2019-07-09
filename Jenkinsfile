

// transport scmVars through all stages, i.e. global
def scmVars

pipeline {

// this is the seed job jenkinsfile
// this job definition loads job DSL groovy script in order to generate jobs for all things Ansible

    agent any

    triggers {
        // execute once an hour, on a minute calculated from the project name
        cron('H * * * *')
    }

    stages {
        // for display purposes
        stage('checkout') {
            steps {
                script {
                    // this is not for declarative pipelines, only for scripted
                    properties([[$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1h']])])

                    // get the code from a git repository
                    scmVars = checkout scm
                }
            }
        }
        stage('generate') {
            steps {
                script {
                    def bitbucketUrl = 'https://bitbucket.imp.ac.at'
                    def bitbucketCredentials = 'svc-bitbucket-access-user-passwd'
                    def bitbucketSshCredentials = 'dd2eddb1-0e79-4acb-9dca-5fe6b4ba25b3'

                    // shared CICD library config, could also do version: scmVars.GIT_COMMIT
                    def cicdLibSettings = [
                            name: 'vbc-cicd',
                            version: scmVars.GIT_BRANCH,
                            gitRepo: "ssh://git@bitbucket.imp.ac.at:7991/iab/vbc-cicd.git",
                            gitCredentialsId: bitbucketSshCredentials
                    ]

                    echo "my git setup: ${scmVars}"
                    echo "will configure library as: ${cicdLibSettings.name} in version: ${cicdLibSettings.version} (commit or branch or tag)"

                    // Bitbucket organizations to scan for roles
                    def vbcBitbucketOrgs = [
                        [owner: "IAB",
                          name:"IT Ansible Baseline",
                          description: "Baseline components for System Deployment",
                          excludePattern: "vbc-cicd",
                          includePattern: "*",
                          buildTags: false ],
                        [owner: "IAO",
                          name:"IT Ansible Ops",
                          description: "Operations Tasks and Tooling",
                          excludePattern: "",
                          includePattern: "*",
                          buildTags: false ],
                        [owner: "VBC",
                          name:"VBC repos",
                          description: "mostly for building docker images",
                          excludePattern: "",
                          includePattern: "*",
                          buildTags: true ],
                        [owner: "CLIP",
                          name:"CLIP Ansible Roles",
                          description: "CLIP related Ansible roles",
                          excludePattern: "",
                          includePattern: "role-*",
                          buildTags: false ],
                        [owner: "CLIP",
                          folder: "CLIP-platinum",
                          name:"Platinum",
                          description: "CLIP Platinum accounting",
                          excludePattern: "",
                          includePattern: "platinum*",
                          buildTags: true ]
                    ]

                    // molecule cookiecutter testing is extra
                    def cookiecutterRepoConfig = [
                        url: bitbucketUrl,
                        credentials: bitbucketCredentials,
                        repoOwner: "IAB",
                        repoName: "cookiecutter-molecule",
                        sshCredentials: bitbucketSshCredentials
                    ]



                    // call the jobdsl script for the roles
                    jobDsl removedConfigFilesAction: 'DELETE',
                           removedJobAction: 'DELETE',
                           removedViewAction: 'DELETE',
                           lookupStrategy: 'SEED_JOB',
                           sandbox: true,
                           targets: 'jobs/*.groovy',
                           additionalParameters: [
                               bitbucketUrl: bitbucketUrl,
                               bitbucketCredentials: bitbucketCredentials,
                               bitbucketSshCredentials: bitbucketSshCredentials,
                               cicdLibConfig: cicdLibSettings,
                               vbcBitbucketOrgs: vbcBitbucketOrgs,
                               cookiecutterRepoConfig: cookiecutterRepoConfig
                           ]
                 }
            }
        }
    }
    // post block is executed after all the stages/steps in the pipeline
    post {
        always {
            // notify build results, see https://jenkins.io/blog/2016/07/18/pipline-notifications/
            // notifyBuild(currentBuild.result)
            echo "this will always show up"
        }
        changed {
            echo "build changed"
        }
        aborted {
            echo "build aborted"
        }
        failure {
            echo "build failed"
        }
        success {
            echo "build is success"
        }
        unstable {
            echo "build is unstable"
        }
    }

}
