// execute tower jobs: https://wiki.jenkins.io/display/JENKINS/Ansible+Tower+Plugin

def call(String jobTemplate, Map params=[]) {

    //def jobTemplate = 'My Job Template'
    def towerInventory = params.get('towerInventory', null)
    def jobTags = params.get('jobTags', '')
    def skipJobTags = params.get('skipJobTags', '')
    def limit = params.get('limit', '')
    def extraVars = params.get('extraVars', '')
 
    ansiColor('xterm') {
        ansibleTower(
            towerServer: 'VBC Tower',
            credential: null, // this is set globally?
            templateType: 'job',
            jobTemplate: jobTemplate,
            inventory: towerInventory,
            jobTags: jobTags,
            skipJobTags: skipJobTags,
            limit: limit,
            importTowerLogs: true,
            removeColor: false,
            verbose: true,
            extraVars: extraVars
        )
    }
}
