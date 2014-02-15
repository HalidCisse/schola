$ = require('jqueryify')

R = jsRoutes.controllers

class roles

  @getRoles: ->
    $.getJSON R.Roles.getRoles().url

  @getRole: (name) ->
    $.getJSON R.Roles.getRole(name).url

  @getPermissions: ->
    $.getJSON R.Roles.getPermissions().url

  @getRolePermissions: (name) ->
    $.getJSON R.Roles.getRolePermissions(name).url

  @addRole: (spec) ->
    route = R.Roles.addRole(spec)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @updateRole: (name, newName, parent) ->
    route = R.Roles.updateRole(name, newName, parent)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @purgeRoles: (roles) ->
    route = R.Roles.purgeRoles(roles)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )        

  @grantPermissions: (roleName, permissions) ->
    route = R.Roles.grantRolePermissions(roleName, permissions)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @revokePermissions: (roleName, permissions) ->
    route = R.Roles.revokeRolePermissions(roleName, permissions)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @roleExists: (roleName) ->
    $.getJSON R.Roles.roleExists(roleName).url

module.exports = roles