Spine = require('spine')

RolePermission = require('models/RolePermission')

class Role extends Spine.Model

  @configure 'Role', 
    'name', 
    'parent', 
    'publiq', 
    'createdAt', 
    'createdBy'

  @Reload: ->

    $.Deferred (deferred) =>

      deferred.done (roles) => 
        @refresh(roles, clear: true)

      app.roles.getRoles()
         .done deferred.resolve

  @getTopLevel: ->

    @select (role) => 
      not role.parent

  @getChildren: (role) ->

    @select (other) => 
      other.parent is role.name
    
  @getPermissions: (role) ->
    RolePermission.select (rolePermission) => rolePermission.role is role
  
module.exports = Role