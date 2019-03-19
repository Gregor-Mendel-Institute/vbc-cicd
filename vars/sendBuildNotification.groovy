
// this is our externalized way of sending build notifications

def call(Map currentBuild, Map params = [:]) {


    echo "NOTIFICATION TIME"
    echo "got build: ${currentBuild}"
}

return this
