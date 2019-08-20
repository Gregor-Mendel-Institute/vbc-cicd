
/*
{
  "uuid": "luxb5uaasrgorhokhrbgvioyi4",
  "templateUuid": "001",
  "trashed": "N",
  "createdAt": "2019-08-20T09:46:39Z",
  "updatedAt": "2019-08-20T09:48:44Z",
  "changerUuid": "2DHGUNNA7ZFQFKSDEQQIRONADU",
  "itemVersion": 1,
  "vaultUuid": "2cjwqg2wvbdsnaf6etdpmomora",
  "details": {
    "fields": [
      {
        "designation": "username",
        "name": "username",
        "type": "T",
        "value": "my_name"
      },
      {
        "designation": "password",
        "name": "password",
        "type": "P",
        "value": "my_password"
      }
    ],
    "notesPlain": "entry for testing 1password api lookups in arbitrary sections",
    "sections": [
      {
        "name": "linked items",
        "title": "Related Items"
      },
      {
        "fields": [
          {
            "k": "string",
            "n": "AA861505D1F347CB8EAAABA44DDB12EE",
            "t": "my_field",
            "v": "my_value"
          }
        ],
        "name": "Section_408E7181BC004F92A43F75550D52BB9D",
        "title": "my_section"
      }
    ]
  },
  "overview": {
    "ainfo": "my_name",
    "ps": 51,
    "title": "my_test_example"
  }
}
 */


def signin(String credentialsUsernamePassword, String credentialsDomainMasterKey) {
    echo "signing in to 1Password using credentials: ${credentialsUsernamePassword} and ${credentialsDomainMasterKey}"

    def onePassCredentials = [
        usernamePassword(credentialsId: credentialsUsernamePassword, usernameVariable: "OP_USERNAME", passwordVariable: "OP_PASSWORD"),
        usernamePassword(credentialsId: credentialsDomainMasterKey, usernameVariable: "OP_DOMAIN", passwordVariable: "OP_DOMAIN_MASTER_KEY")
    ]

    def onepass_token = null
    withCredentials(onePassCredentials) {

        withEnv(["OP_DOMAIN=${op_domain}", "OP_MASTER_KEY=${op_master_key}"]) {
            echo "onepass env:"
            sh "env"
            echo "will sign in to domain: ${env.OP_DOMAIN}"
            // usage: op signin <signinaddress> <emailaddress> <secretkey> [--output=raw]
            onepass_token = sh label: "onepass", script: 'echo $OP_PASSWORD | op signin $OP_DOMAIN $OP_USERNAME $OP_MASTER_KEY --output=raw', returnStdout: true
        }
        // FIXME this is leaking
        echo "got the token: ${onepass_token}"

        if (onepass_token == null) {
            error('failed to retrieve 1Password session token')
        }
    }

    return onepass_token
}


def lookup(String itemName, String vault=null, String section='default', String field = 'password') {

}

return this