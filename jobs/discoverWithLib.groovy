import vbc.cicd.*

// this should be ABC

for (Map org in discoverOrgs) {
    JobFactory factory = new JobFactory(this, org)
    // out.println "creating organization folder for: ${factory.owner}"
    def orgFolder = factory.makeOrganizationFolder()
}
//out << "done creating."

