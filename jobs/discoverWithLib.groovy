import javaposse.jobdsl.dsl.jobs.OrganizationFolderJob
import vbc.cicd.*

// this should be ABC

for (Map org in discoverOrgs) {
    JobFactory factory = new JobFactory(this, org)
    // out.println "creating organization folder for: ${factory.owner}"
    OrganizationFolderJob orgFolder = factory.makeOrganizationFolder()

    // queue org folder job right away to discover child projects
    queue(orgFolder)
}
//out << "done creating."

