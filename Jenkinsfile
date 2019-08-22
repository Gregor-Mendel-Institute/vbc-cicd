#!/usr/bin/env groovy

// transport scmVars through all stages, i.e. global
def scmVars

// reusable DSL https://blog.thesparktree.com/you-dont-know-jenkins-part-2

pipeline {

// this is the seed job jenkinsfile
// this job definition loads job DSL groovy script in order to generate jobs for all things Ansible

    agent any

    triggers {
        // execute once an hour, on a minute calculated from the project name
        cron('H * * * *')
    }
    //parameters {
        //fileParam("pipelines.yml", "Pipeline definition fila (yaml)")
    //}

    stages {
        // for display purposes
        stage('checkout') {
            steps {
                script {
                    // this is not for declarative pipelines, only for scripted
                    properties([
                        //[$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], 
                        pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1h']])
                    ])

                    // get the code from a git repository
                    scmVars = checkout scm
                    // checkout baseline for seed org info
                    // FIXME ref should be configurable, i.e. branch or tag
                    // TODO investigate resolveScm(source: git('https://example.com/example.git'), targets: [BRANCH_NAME,'master']
                    // tries to find the same ref as main scm, then falls back to master
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: 'refs/heads/jenkins_server']],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'baseline']],
                        submoduleCfg: [],
                        userRemoteConfigs: [[credentialsId: 'seed-git-ssh', url: 'ssh://git@bitbucket.imp.ac.at:7991/iab/linux-baseline.git']]
                    ])
                }
            }
        }

        stage('generate') {
            steps {
                script {
                    echo "my git setup: ${scmVars}"

                    // Bitbucket organizations to scan for roles
                    //def discovery_data = readYaml file: "default_discovery.yml"
                    def discovery_data = readYaml file: "baseline/host_vars/test-jenkins-1.vbc.ac.at"

                    onepass.signin('svc-1password-user', 'svc-1password-domain')

                    def theVault = 'sandbox'
                    // oneass.lookup (itemName,  vaultName=null, sectionName=null, fieldName = 'password') {
                    def examplePassword = onepass.lookup('my_test_example') // should give password
                    def exampleUsername = onepass.lookup('my_test_example', theVault, null, 'username')

                    onepass.clearCache()
                    def exampleFieldValue = onepass.lookup('my_test_example', theVault, "my_section", 'my_field')

                    // lookup all the credentials
                    def seed_orgs = discovery_data.jenkins_seed_orgs
                    // echo "processing seed orgs: ${seed_orgs}"
                    for (org in seed_orgs) {
                        echo "fetching credentials specific to ${org.owner}"
                        for (creds_in_domain in org.jenkins.credentials) {

                          def domain = creds_in_domain.get('domain')
                          if (domain != null) {
                            echo "lookup for domain: ${domain.name}"
                          }
                          else {
                            echo "lookup for DEFAULT domain:"
                          }

                          for (cc in creds_in_domain.credentials) {
                            echo "looking for ${cc.id}"

                          }
                        }
                    }

                    // call the jobdsl script for the roles
                    jobDsl removedConfigFilesAction: 'DELETE',
                           removedJobAction: 'DELETE',
                           removedViewAction: 'DELETE',
                           //lookupStrategy: 'SEED_JOB',
                           lookupStrategy: 'JENKINS_ROOT',
                           sandbox: true,
                           targets: 'jobs/discoverWithLib.groovy',
                           //additionalClasspath: 'src/main/groovy',
                           additionalParameters: [
                               discoverOrgs: seed_orgs
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
