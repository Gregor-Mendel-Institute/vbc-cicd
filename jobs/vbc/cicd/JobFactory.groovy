package vbc.cicd

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
    def _dslFactory

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
        for (pm in permissionObjects) {
            String principal = pm.name
            List<String> permissions = pm.jenins_perms
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

    Closure itemProperties() {
        return { item ->
            item.properties {

            }
        }
    }

    Closure itemCredentials() {
        return { item ->
            item.properties {
                folderCredentialsProperty {

                    for (org_creds in this.raw.jenkins.credentials) {
                        def cred_list = org_creds.credentials
                        // NULL is the name of the global domain: https://github.com/jenkinsci/credentials-plugin/blob/master/src/main/java/com/cloudbees/plugins/credentials/domains/Domain.java#L52
                        def cred_domain = org_creds.get('domain', [:])

                        domainCredentials {
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
                            credentials {
                                //basicSSHUserPrivateKey {}
                                //certificateCredentialsImpl {}
                                for (cc in cred_list) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Closure folderAuthorization() {
        return { folder ->
            folder.authorization {
                // jobUtils.buildPermissions(org.permissions)
                for (perm in this.permissionSets) {
                    permissions(perm.key, perm.value)
                }
            }
        }
    }

    public OrganizationFolderJob makeOrganizationFolder() {

        def orgFolder = _dslFactory.organizationFolder(this.folder) {
            displayName(this.name)
            description(this.description)


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


        orgFolder.with(this.folderAuthorization())
        orgFolder.with(repoProvider.repoTriggers())
        orgFolder.with(repoProvider.asOrganizations())
        orgFolder.with(this.itemCredentials())
        orgFolder.with(this.itemProperties())

        orgFolder.with {this.itemCredentials()}
        // return complete configured job
        return orgFolder



    }
}
