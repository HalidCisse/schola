$ = require('jqueryify')

R = jsRoutes.controllers

class users

  @getUser: (id) ->
    $.getJSON R.Users.getUser(id).url # "/api/v1/user/#{id}"

  @getUsersStats: ->
    $.getJSON R.Users.getUsersStats().url # "/api/v1/users/stats"

  @getUsers: (page=0) ->
    $.getJSON R.Users.getUsers(page).url # '/api/v1/users', page: page

  @upsertUser: (spec) ->
    route = if spec.id then R.Users.addUser() else R.Users.updateUser(spec.id)
    $.ajax(
      type: route.type, # if(spec.id) then 'PUT' else 'POST',
      url:  route.url, # "/api/v1/users#{if spec.id then '/'+spec.id else ''}",
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8',
      data: JSON.stringify(spec)
    )    

  @removeUsers: (users) ->
    route = R.Users.removeUsers(users)
    $.ajax(
      type: route.type, # "DELETE",
      url: route.url, # "/api/v1/user/#{id}",
      dataType: 'json'
    )    

  @purgeUsers: (users) ->
    route = R.Users.purgeUsers(users)
    $.ajax(
      type: route.type, # "DELETE",
      url: route.url, # "/api/v1/user/#{id}/_purge",
      dataType: 'json'
    )        

  @changePasswd: (id, newPasswd, oldPassword) ->
    route = R.Users.changePasswd(id)
    $.ajax(
      type: route.type,
      url: route.url, # "/api/v1/user/#{id}",
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
      url: "/api/v1/user/#{id}/roles?" + $.param({roles}, true),
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

    sendFile = (file) -> # TODO
      $.ajax(
        type: 'POST'
        url: '/Avatar?name=' + file.name
        data: file,
        success: ->
          # do something
          console.log("success")
        xhrFields:
          # add listener to XMLHTTPRequest object directly for progress (jquery doesn't have this yet)
          onprogress: (progress) -> 
            # calculate upload progress
            percentage = Math.floor((progress.total / progress.totalSize) * 100)
            # log upload progress to console
            console.log('progress', percentage)
            if percentage is 100 
              console.log('DONE!')
        processData: false
        contentType: file.type
        mimeType: file.type
      )  

    $.upload("/api/v1/user/#{id}/avatars", data, 'json')

  @getUserRoles: (id) ->
    $.getJSON "/api/v1/user/#{id}/roles"

  @userExists: (username) ->
    $.getJSON "/api/v1/userexists", {username}

  @getTrashed: ->
    $.getJSON "/api/v1/users/_trash"

module.exports = users