// parse overly of multi build params onto "default params"
// i.e. given a set of parameters, fill multi params with defaults from 
@groovy.transform.Field
Map defaultParams = [:]

@groovy.transform.Field
Map builds = [:]

// add a default, top-level parameter
def addParam(String paramName, String paramDefault, boolean mandatory = false) {
    echo "adding parameter ${paramName}"
    defaultParams.put(paramName, paramDefault)
}

// add a multi-build instance, by name, as overlay on defaultParams
def addBuild(String buildName, Map buildOverlay) {
    Map buildConfig = [:]
    buildConfig.putAll(defaultParams)
    buildConfig.putAll(buildOverlay)

    echo "adding extra build config: ${buildConfig}"
    builds.put(buildName, [name: buildName, config: buildConfig])
}

// list of dictionaries as list of build parameters
def addMultiParams(String nameKey, List multiParams) {

    // add default build config
    addBuild(defaultParams[nameKey], defaultParams)

    for (Map params in multiParams) {
        if (!params[nameKey]) {
            error("Multi-Build config is invalid (no key ${nameKey}): ${params}")
        }
        def buildName = params[nameKey]
        addBuild(buildName, params)
    }
}

// helper to run given body closure for each multiBuild, injecting name and config
def forAllBuilds(Closure bodyPerBuild) {
    builds.each { name, build ->
        def conf = build.config
        bodyPerBuild(name, conf, build)
    }
}

return this
