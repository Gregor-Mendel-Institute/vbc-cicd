package vbc.cicd

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Folder
import javaposse.jobdsl.dsl.Item
import javaposse.jobdsl.dsl.jobs.OrganizationFolderJob
import vbc.cicd.repo.RepoProvider


class JobFactory {

/** sample ORG entry to be represented
  - owner: "DMY"
    name: Dummy project example
    jenkins:
      includePattern: "*"
      buildTags: true
      provider:
        <<: *bitbucket
        # type: single
      #project_credentials: *dmy_credentials
      credentials:
        - credentials:
            - description: my extra credentials
              id: my-extra
              type: usernamepassword
              username: qwer
              password: '1234'
            - type: sshprivatekey
              scope: GLOBAL
              id: "dmy-special-ssh"
              username: "dmy_ssh_svc_user_testing"
              # Doable, but not recommended
              passphrase:
              privatekey: "completely incorrect format"
              description: "SSH Credentials for special checkouts"
    groups:
      - name: perm.pipline.dmy
        # leave empty for no bitbucket access
        bitbucket_perm: PROJECT_WRITE
        artifactory_perms:
          - r
          - w
          - a
          - d
          # new api perm names
          # - read
          # - write
          # - annotate
          # - delete
        tower_perms: []
        jenkins_perms: *jenkins_default_perms
    # for Artifactory
    repositories:
      docker:
        includes:
          - dmy/**
      # - mvn
    tower:
      team: "Team DMY"
      templates:
        - The App Prod
        - The App Dev

*/

    private String owner = "XXX"
    private String name = "no name set"
    private String description = ""
    private Boolean buildTags = false
    private String folder = null

    boolean globalJobDisabled = false

    private Map<String,List<String>> permissionSets = [:]
    private RepoProvider repoProvider = null

    private Map raw = null
    DslFactory _dslFactory = null

    public JobFactory(dslFactory, Map org, boolean disabled = false) {
        this.globalJobDisabled = disabled
        this._dslFactory = dslFactory
        this.raw = org

        this.owner = org.owner
        this.name = org.name
        this.description = org.get('description', '')

        this.buildTags = org.jenkins.get('buildTags', false)
        // folder can be set optionally, defaults to owner
        this.folder = org.jenkins.get('folder', this.owner)

        buildPermissions((List) org.get('groups', []))
        buildPermissions((List) org.get('users', []))
        this.repoProvider = RepoProvider.newRepoProvider(org)
    }

    // setter / getters are created implicitly

    private buildPermissions(List permissionObjects) {
        for (Map pm in permissionObjects) {
            String principal = pm.name
            // also create permissions if empty, using this to explicitly shut off access inherited from above
            List<String> permissions = pm.get('jenkins_perms', [])
            permissionSets.put(principal, permissions)
        }
    }


    Folder makeFolder() {
        return _dslFactory.folder(this.folder) {

        }
    }


    Item makeMultibranchJob() {
        return _dslFactory.multibranchPipelineJob(this.folder) {
            displayName("Molecule Cookiecutter")
            description("test the cookiecutter template for creating new ansible roles")

        }
    }

    Closure configure() {
        // to job configure freeform
        return {

            // setup build strategies: avoid build storm on tags, don't build after discovery
            def buildStrategies = it / buildStrategies
            buildStrategies << 'jenkins.branch.buildstrategies.basic.SkipInitialBuildOnFirstBranchIndexing' {
            }
            /* this invalidates above skip build on first indexing
            buildStrategies << 'jenkins.branch.buildstrategies.basic.BranchBuildStrategyImpl' {
            }
            */

            if (this.buildTags) {
                // automatically build tags newer than 7 days (604800000 millis)
                buildStrategies << 'jenkins.branch.buildstrategies.basic.TagBuildStrategyImpl' {
                    atLeastMillis(1)
                    atMostMillis(604800000)
                }
            }

        }
    }

    // build up credentials set for the processed Item (Job / folder)
    Closure itemCredentials() {
        List item_credentials = this.raw.jenkins.credentials

        return {
            folderCredentialsProperty {
                // yes domainCredentials is nested 2x here
                domainCredentials {
                    for (creds_in_domain in item_credentials) {
                        CredentialsBuilder builder = new CredentialsBuilder(creds_in_domain)
                        domainCredentials builder.asDsl()
                    }
                }
            }
        }
    }

    public OrganizationFolderJob makeOrganizationFolder() {

        def orgFolder = _dslFactory.organizationFolder(this.folder) {
            displayName(this.name)
            description(this.description)

            // disable job if global disable flag is set
            // fixme cannot call this on or folder jobs
            //if (this.globalJobDisabled) {
                // disabled()
            //}

            authorization {
                for (perm in this.permissionSets) {
                    permissions(perm.key, perm.value)
                }
            }

            // dynamically setup the right organization
            //organizations repoProvider.getOrganization()

            // this is how we detect that there is something to do for us
            projectFactories {
                workflowMultiBranchProjectFactory {
                    // Relative location within the checkout of your Pipeline script.
                    scriptPath("Jenkinsfile")
                }
            }

            // here orphaned items are repositories in the organisations, not jobs
            orphanedItemStrategy {
                /*
                defaultOrphanedItemStrategy {
                    pruneDeadBranches(true)
                    daysToKeepStr("90")
                    numToKeepStr("30")
                }
                */
            }
        }

        // setup job triggers if not globally disabled
        /*
        if (!this.globalJobDisabled) {
            orgFolder.triggers this.repoProvider.repoTriggers()
        }
        else {
            // FIXME log the fact we're not enabling triggers
        }
        */
        orgFolder.triggers this.repoProvider.repoTriggers()
        orgFolder.organizations this.repoProvider.asOrganizations()
        orgFolder.properties this.itemCredentials()

        orgFolder.configure this.repoProvider.configure()
        orgFolder.configure this.configure()
        //orgFolder.configure this.repoProvider.configure()


        // orgFolder.with(this.itemProperties())
        // return complete configured job
        return orgFolder
    }
}
