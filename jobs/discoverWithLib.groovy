import vbc.cicd.JobFactory

for (org in discoverOrgs) {
    if (org.jenkins.provider.type != 'bitbucket')
        continue
    
    JobFactory factory = new JobFactory(this, org)
    println("creating organization folder for: ${factory.owner}")
    factory.makeOrganizationFolder()
}