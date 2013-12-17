Spine = require('spine')

class Role extends Spine.Model
  @configure 'Role', 'name', 'parent', 'public', 'createdAt', 'createdBy'

  grantPermissions: (permissions) ->

  revokePermissions: (permissions) ->
  
module.exports = Role