import vbc.cicd.JobFactory

def org = [:]

org.owner = 'ABC'
org.name = 'ABC project'
org.jenkins = [:]
org.jenkins.credentials = []
org.jenkins.provider= [:]
org.jenkins.provider.type = 'bitbucket'
org.jenkins.provider.url = 'ssh://git@bitbucket.imp.ac.at/abc/asdf.git'
org.jenkins.provider.credentialsId = 'asdf-qwer-1234'

org.groups = []


JobFactory factory = new JobFactory(this, org)
// out.println "creating organization folder for: ${factory.owner}"
def orgFolder = factory.makeOrganizationFolder()

//out << "done creating."

