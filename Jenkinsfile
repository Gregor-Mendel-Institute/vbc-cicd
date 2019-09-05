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
    // not setting parameters here will avoid overwriting them from initial job DSL seed setup
    parameters {
        // see https://plugins.jenkins.io/git-parameter
        // these parameters only effect the repo/location/version of the discovery config data
        // credentials name: "SEED_JOB_CREDENTIALS_ID", defaultValue: "${env.SEED_JOB_CREDENTIALS_ID}", description: 'credentials to access seed job config repo', credentialType: 'com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey'
        // branch,version of actual seed job is in its job definition
        // gitParameter name: 'SEED_JOB_VERSION', branchFilter: "origin/(.*)", defaultValue: "${env.SEED_JOB_VERSION}", type: 'PT_BRANCH_TAG', description: 'seed job revision', useRepository: "${env.SEED_JOB_REPO_URL}", selectedValue: 'DEFAULT'

        string name: "SEED_JOB_CONFIG_REPO_URL", defaultValue: "${env.SEED_JOB_CONFIG_REPO_URL}", description: 'repo to retrieve discovery info from', trim: true
        gitParameter name: 'SEED_JOB_CONFIG_VERSION', branchFilter: "origin/(.*)", defaultValue: "${env.SEED_JOB_CONFIG_VERSION}", type: 'PT_BRANCH_TAG', description: 'which config (in host_vars) from linux-baseline repo', useRepository: "${env.SEED_JOB_CONFIG_REPO_URL}", selectedValue: "DEFAULT"
        string name: 'SEED_JOB_CONFIG_FILE', defaultValue: "${env.SEED_JOB_CONFIG_FILE}", description: 'file to read job discovery from', trim: true
    }

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
                    // TODO investigate resolveScm(source: git('https://example.com/example.git'), targets: [BRANCH_NAME, 'master']
                    // tries to find the same ref as main scm, then falls back to master
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "${params.SEED_JOB_CONFIG_VERSION}"]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'config']],
                        submoduleCfg: [],
                        userRemoteConfigs: [[credentialsId: "${env.SEED_JOB_CREDENTIALS_ID}", url: "${env.SEED_JOB_CONFIG_REPO_URL}"]]
                    ])
                }
            }
        }

        stage('generate') {
            steps {
                script {
                    echo "my git setup: ${scmVars}"

                    // organizations to scan
                    def discovery_data = readYaml file: "${env.SEED_JOB_CONFIG_FILE}"
                    onepass.signin('svc-1password-user', 'svc-1password-domain')

                    // lookup all the credentials
                    def seed_orgs = discovery_data.jenkins_seed_orgs
                    // echo "processing seed orgs: ${seed_orgs}"
                    for (org in seed_orgs) {
                        echo "fetching credentials specific to ${org.owner}"
                        for (creds_in_domain in org.jenkins.get('credentials', [])) {

                          def domain = creds_in_domain.get('domain')
                          if (domain != null) {
                            echo "lookup for domain: ${domain.name}"
                          }
                          else {
                            echo "lookup for DEFAULT domain:"
                          }

                          for (cc in creds_in_domain.credentials) {
                            echo "looking for ${cc.id}"
                                if ('onepass' in cc) {
                                    for (op_lookup in cc.onepass) {
                                        // dynamically lookup and assign
                                        cc."${op_lookup.target}" = onepass.lookup(op_lookup.item, op_lookup.vault, op_lookup.section, op_lookup.field)
                                    }
                                }
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
                               discoverOrgs: seed_orgs,
                               globalJobDisabled: (env.GLOBAL_JOB_DISABLE == 'true')
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

            echo "publish build result to Artifactory"
            rtBuildInfo maxBuilds: 30, captureEnv: true
            rtPublishBuildInfo serverId: "artifactoryVBC"

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
