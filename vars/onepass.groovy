
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


def signin(String credentialsId) {
    echo "signing in to 1Password using credentials: ${credentialsId}"

    def onePassCredentials = [
        usernamePassword(credentialsId: credentialsId, usernameVariable: "OP_USERNAME", passwordVariable: "OP_PASSWORD")
    ]

    withCredentials(onePassCredentials) {
        def onepass_token = sh label: "onepass", script: 'echo $OP_PASSWORD | op signin --raw $OP_USERNAME', returnStdout: true

        // FIXME this is leaking
        echo "got the token: ${onepass_token}"
    }




}


def lookup(String itemName, String vault=null, String section='default', String field = 'password') {

}

return this