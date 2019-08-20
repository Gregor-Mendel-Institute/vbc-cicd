import vbc.cicd.*

// this should be ABC
Map org = discoverOrgs[1]

JobFactory factory = new JobFactory(this, org)
// out.println "creating organization folder for: ${factory.owner}"
def orgFolder = factory.makeOrganizationFolder()

//out << "done creating."

