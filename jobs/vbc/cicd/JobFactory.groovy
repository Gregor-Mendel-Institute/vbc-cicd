package vbc.cicd

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Folder
import javaposse.jobdsl.dsl.Item
import javaposse.jobdsl.dsl.jobs.OrganizationFolderJob


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

    private Map<String,List<String>> permissionSets = [:]
    private RepoProvider repoProvider = null

    private Map raw = null
    DslFactory _dslFactory = null

    public JobFactory(dslFactory, Map org) {

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


    Closure generateDomainCredentials(Map org_creds) {
        // NULL is the name of the global domain: https://github.com/jenkinsci/credentials-plugin/blob/master/src/main/java/com/cloudbees/plugins/credentials/domains/Domain.java#L52
        Map cred_domain = org_creds.get('domain', [:])

        return {
            domain {
                name(cred_domain.get('name'))
                description(cred_domain.get('description'))
                specifications {
                    hostnameSpecification {
                        // A comma separated whitelist of hostnames.
                        includes(cred_domain.get('includes', ""))
                        // A comma separated blacklist of hostnames.
                        excludes(cred_domain.get('excludes', ""))
                    }
                }
            }

            for (Map cc in org_creds.credentials) {
                String cc_scope = cc.get('scope', 'GLOBAL')
                usernamePasswordCredentialsImpl {
                    // Determines where this credential can be used.
                    scope(cc_scope)
                    // An internal unique ID by which these credentials are identified from jobs and other configuration.
                    id(cc.id)
                    // An optional description to help tell similar credentials apart.
                    description(cc.get('description', ''))
                    // The username.
                    username(cc.username)
                    // The password. FIXME this should be a placeholders, as needs updating from 1Pass??
                    password(cc.get('password', 'undefined-testing-value').toString())
                }
            }

            /*
            credentials {
                //basicSSHUserPrivateKey {}
                //certificateCredentialsImpl {}
                for (cc in cred_list) {
                }
            }
            */
        }
    }


    // build up credentials set for the processed Item (Job / folder)
    Closure itemCredentials() {
        def item_credentials = this.raw.jenkins.credentials

        return {
            folderCredentialsProperty {
                for (org_creds in item_credentials) {
                    domainCredentials { this.generateDomainCredentials(org_creds) }
                }
            }
        }
    }

    public OrganizationFolderJob makeOrganizationFolder() {

        def orgFolder = _dslFactory.organizationFolder(this.folder) {
            displayName(this.name)
            description(this.description)

            authorization {
                assert this.permissionSets.size() > 0
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
        }

        orgFolder.triggers this.repoProvider.repoTriggers()
        orgFolder.organizations this.repoProvider.asOrganizations()
        orgFolder.properties this.itemCredentials()


        // orgFolder.with(this.itemProperties())
        // return complete configured job
        return orgFolder
    }
}
