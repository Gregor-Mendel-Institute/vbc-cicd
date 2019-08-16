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
org.jenkins.credentials = [
    [
        domain: [
            name: "myCreds",
            description: "wherever, whatever",
            includes: "example.com",
            excludes: ""
        ],
        credentials: [
            [
                id: 'asdf-qwer-1234',
                type: 'usernamepassword',
                username: "the_user_name",
                password: "the secret password"
            ]
        ]
    ]
]
org.groups = [
    [
        name: "mygroup",
        jenkins_perms: [
            "hudson.model.Item.Read",
            "hudson.model.Item.Build",
            "hudson.model.Item.Cancel"
        ]
    ],
    [
        name: "group_without_Access",
        jenkins_perms: []
    ]
]


JobFactory factory = new JobFactory(this, org)
// out.println "creating organization folder for: ${factory.owner}"
def orgFolder = factory.makeOrganizationFolder()

//out << "done creating."

