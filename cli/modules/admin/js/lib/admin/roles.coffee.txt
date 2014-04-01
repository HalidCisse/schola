$ = require('jqueryify')

R = jsRoutes.controllers.Roles

class roles

  @getRoles: ->
    $.getJSON R.getRoles().url

  @getRole: (name) ->
    $.getJSON R.getRole(name).url

  @getPermissions: ->
    $.getJSON R.getPermissions().url

  @getRolePermissions: (name) ->
    $.getJSON R.getRolePermissions(name).url

  @addRole: (spec) ->
    route = R.addRole(spec)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @updateRole: (name, newName, parent) ->
    route = R.updateRole(name, newName, parent)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @purgeRoles: (roles) ->
    route = R.purgeRoles(roles)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )        

  @grantPermissions: (roleName, permissions) ->
    route = R.grantPermissions(roleName, permissions)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @revokePermissions: (roleName, permissions) ->
    route = R.revokePermissions(roleName, permissions)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @roleExists: (roleName) ->
    $.getJSON R.roleExists(roleName).url

module.exports = roles