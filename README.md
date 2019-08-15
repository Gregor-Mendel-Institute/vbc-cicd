
goal: automatically create all CI/CD jobs for ansible repositories
* roles
* playbook repos
* all others

see also
* https://github.com/jenkinsci/job-dsl-plugin/wiki/IDE-Support
* Examples https://github.com/jenkinsci/job-dsl-plugin/wiki/Real-World-Examples
* Tutorial https://github.com/jenkinsci/job-dsl-plugin/wiki/Tutorial---Using-the-Jenkins-Job-DSL
* dsl, lib closures: https://blog.thesparktree.com/you-dont-know-jenkins-part-2

test locally:
wget https://repo.jenkins-ci.org/public/org/jenkins-ci/plugins/job-dsl-core/1.75/job-dsl-core-1.75-standalone.jar
java -classpath . -jar job-dsl-core-1.75-standalone.jar jobs/discoverWithLib.groovy

