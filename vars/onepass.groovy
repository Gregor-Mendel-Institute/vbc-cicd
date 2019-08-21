
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

import groovy.transform.Memoized

@groovy.transform.Field
Map itemCache = [:]

@groovy.transform.Field
def onePassToken = null

def signin(String credentialsUsernamePassword, String credentialsDomainMasterKey) {
    echo "signing in to 1Password using credentials: ${credentialsUsernamePassword} and ${credentialsDomainMasterKey}"

    def onePassCredentials = [
        usernamePassword(credentialsId: credentialsUsernamePassword, usernameVariable: "OP_USERNAME", passwordVariable: "OP_PASSWORD"),
        usernamePassword(credentialsId: credentialsDomainMasterKey, usernameVariable: "OP_DOMAIN", passwordVariable: "OP_MASTER_KEY")
    ]


    withCredentials(onePassCredentials) {
        echo "will sign in to domain: ${env.OP_DOMAIN}"
        // usage: op signin <signinaddress> <emailaddress> <secretkey> [--output=raw]
        def signin_script = 'echo $OP_PASSWORD | op signin $OP_DOMAIN $OP_USERNAME $OP_MASTER_KEY --output=raw'

        onePassToken = sh label: "onepass", script: signin_script, returnStdout: true

        // FIXME remove it, this is leaking, was: for debugging
        // echo "got the token: ${onepass_token}"

        if (onePassToken == null) {
            error('failed to retrieve 1Password session token')
        }
    }

    return onePassToken
}

def lookup(String itemName, String vaultName=null, String sectionName=null, String fieldName = 'password') {


    Map raw = raw(itemName, vaultName)

    String lookupValue = null

    if (sectionName) {
        for (section in raw.sections) {
            if (section.title == sectionName) {
                for (field in section.fields) {
                    if (field.t == fieldName) {
                        lookupValue = field.v
                    }
                }
            }
        }
    }
    // we look in default fields
    else {
        for (Map field in raw.details.fields) {
            if (field.name == fieldName) {
                lookupValue = field.value
                break
            }
        }
    }

    echo "lookup value: ${lookupValue}"
    return lookupValue
}

// groovy method caching
//@Memoized(maxCacheSize=100)
def raw(String itemName, String vault = null) {

//    def cached_item = itemCache.get(itemName)
//    if (cached_item)
//        return cached_item

    def vault_param = vault ? "--vault=${vault}" : ""
    def item_raw = sh label: "onepass", script: "op get item --session=${onePassToken} ${itemName} ${vault_param}", returnStdout: true

    Map item_data = readJson text: item_raw

//    itemCache[itemName] = item_data
    return item_data
}

return this