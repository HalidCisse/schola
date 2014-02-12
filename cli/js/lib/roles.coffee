$ = require('jqueryify')

class roles

  @getRoles: ->
    $.getJSON "/api/v1/roles"

  @addRole: (spec) ->
    $.ajax(
      type: 'POST',
      url: "/api/v1/roles",
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify(spec)
    )

  @updateRole: (name, newName, parent) ->
    $.ajax(
      type: 'PUT',
      url: "/api/v1/roles/#{name}",
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify({name: newName, parent: parent})
    )

  @purgeRole: (roleName) ->
    $.ajax(
      type: 'DELETE',
      url: "/api/v1/role/#{roleName}",
      dataType: 'json'
    )        

  @grantPermissions: (roleName, permissions) ->
    $.ajax(
      type: 'PUT',
      url: "/api/v1/role/_/permissions",
      dataType: 'json',
      traditional: true,
      data: {name: roleName, permissions: permissions}
    )

  @revokePermissions: (roleName, permissions) ->
    $.ajax(
      type: 'DELETE',
      url: "/api/v1/role/_/permissions?" + $.param({name: roleName, permissions: permissions}, true),
      traditional: true,
      dataType: 'json'
    )

  @roleExists: (roleName) ->
    $.getJSON "/api/v1/roleexists", {name: roleName}

module.exports = roles