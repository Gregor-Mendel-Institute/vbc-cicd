// jobutils library support for job discovery

def buildPermissions(permissions) {
  echo "calling here"
  // set permissions for generated jobs
  // total admin access
  //permissionAll('admin_gods')
  // add specific permissions if they were configured
  for (perm in permissions) {
    //permissions(perm.subject, perm.privileges)
  }

  return permissions
}

// handin org.groups
def setupOrgPermissions(org) {
    return authorization {
        def groups = org.get('groups', [])
        for (group in groups) {
            permissions(group.name, group.jenkins_perms)
        }
    }
}
