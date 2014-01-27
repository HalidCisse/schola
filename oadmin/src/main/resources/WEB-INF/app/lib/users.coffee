$ = require('jqueryify')

class users

  @getUser: (id) ->
    $.getJSON "/api/v1/user/#{id}"

  @getUsersStats: ->
    $.getJSON '/api/v1/users/stats'

  @getUsers: (page=0) ->
    $.getJSON '/api/v1/users', page: page

  @upsertUser: (spec) ->
    $.ajax(
      type: if(spec.id) then 'PUT' else 'POST',
      url: "/api/v1/users#{if spec.id then '/'+spec.id else ''}",
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify(spec)
    )    

  @removeUser: (id) ->
    $.ajax(
      type:"DELETE",
      url: "/api/v1/user/#{id}",
      dataType: 'json'
    )    

  @purgeUser: (id) ->
    $.ajax(
      type:"DELETE",
      url: "/api/v1/user/#{id}/_purge",
      dataType: 'json'
    )        

  @changePasswd: (id, newPasswd, oldPassword) ->
    $.ajax(
      type:"PUT",
      url: "/api/v1/user/#{id}",
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify({password: newPasswd, old_password: oldPassword})
    )    

  @setAddress: (id, spec) ->
    $.ajax(
      type:"PUT",
      url: "/api/v1/user/#{id}",
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify(spec)
    )        

  @remHomeAddress: (id) ->
    $.ajax(
      type: "PUT",
      url: "/api/v1/user/#{id}",
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify({homeAddress: {}})
    )

  @remWorkAddress: (id) ->
    $.ajax(
      type: "PUT",
      url: "/api/v1/user/#{id}",
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify({workAddress: {}})
    )    

  @updateContacts: (id, contacts) ->
    $.ajax(
      type:"PUT",
      url: "/api/v1/user/#{id}",
      contentType: 'application/json; charset=UTF-8',
      dataType: 'json',
      data: JSON.stringify({contacts})
    )       

  @grantRoles: (id, roles) ->
    $.ajax(
      type:"PUT",
      url: "/api/v1/user/#{id}/roles",
      dataType: 'json',
      traditional: true,
      data: {roles}
    )    

  @revokeRoles: (id, roles) ->
    $.ajax(
      type:"DELETE",
      url: "/api/v1/user/#{id}/roles?" + $.param({roles}),
      traditional: true,
      dataType: 'json'
    )    

  @remAvatar: (id, avatarId) ->
    $.ajax(
      type:"DELETE",
      url: "/api/v1/user/#{id}/avatars/#{avatarId}",
      dataType: 'json'
    )     

  @getAvatar: (userId) ->
    $.getJSON "/api/v1/avatar/#{userId}"

  @setAvatar: (id, file) -> 
    data = new FormData
    data.append("f", file);  

    $.upload("/api/v1/user/#{id}/avatars", data, 'json')

  @getUserRoles: (id) ->
    $.getJSON "/api/v1/user/#{id}/roles"

  @userExists: (username) ->
    $.getJSON '/api/v1/userexists', {username}

  @getTrashed: ->
    $.getJSON '/api/v1/users/_trash'

module.exports = users