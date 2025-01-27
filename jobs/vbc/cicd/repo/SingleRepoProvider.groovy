package vbc.cicd.repo

import groovy.transform.InheritConstructors

@InheritConstructors
class SingleRepoProvider extends RepoProvider {

    @Override
    Closure getScmDefinition() {
        return null
    }

    @Override
    Closure repoTriggers() {
        return {
            // if that even makes sense
            bitbucketPush()
        }
    }

    @Override
    Closure configure() {
        return {
            sources {

            }
        }
    }
}