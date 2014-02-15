$ = require('jqueryify')

R = jsRoutes.controllers

class users

  @getUser: (id) ->
    $.getJSON R.Users.getUser(id).url

  @getUsersStats: ->
    $.getJSON R.Users.getUsersStats().url

  @getUsers: (page=0) ->
    $.getJSON R.Users.getUsers(page).url

  @getUserRoles: (id) ->
    $.getJSON R.Users.getUserRoles(id).url

  @upsertUser: (spec) ->
    route = if spec.id then R.Users.addUser() else R.Users.updateUser(spec.id)
    $.ajax(
      type: route.type
      url:  route.url
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify(spec)
    )    

  @removeUsers: (users) ->
    route = R.Users.removeUsers(users)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )    

  @purgeUsers: (users) ->
    route = R.Users.purgeUsers(users)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @undeleteUsers: (users) ->
    route = R.Users.undeleteUsers(users)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @changePasswd: (id, newPasswd, oldPassword) ->
    route = R.Users.changePasswd(id)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify({password: newPasswd, old_password: oldPassword})
    )    

  @setAddress: (id, spec) ->
    route = R.Users.updateUser(id)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify(spec)
    )        

  @remHomeAddress: (id) ->
    route = R.Users.updateUser(id)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify({homeAddress: {}})
    )

  @remWorkAddress: (id) ->
    route = R.Users.updateUser(id)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify({workAddress: {}})
    )    

  @updateContacts: (id, contacts) ->
    route = R.Users.updateUser(id)
    $.ajax(
      type: route.type
      url: route.url
      contentType: 'application/json; charset=UTF-8'
      dataType: 'json'
      data: JSON.stringify({contacts})
    )       

  @grantRoles: (id, roles) ->
    route = R.Users.grantUserRoles(id, roles)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @revokeRoles: (id, roles) ->
    route = R.Users.revokeUserRoles(id, roles)
    $.ajax(
      type: route.type,
      url: route.url
      dataType: 'json'
    )    

  @remAvatar: (id, avatarId) ->
    route = R.Users.purgeAvatar(id, avatarId)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )     

  @getAvatar: (avatarId) ->
    $.getJSON R.Users.downloadAvatar(avatarId).url

  @setAvatar: (id, file) ->

    sendFile = (file) ->
      route = R.Users.uploadAvatar(id, file.name)
      $.ajax(
        type: route.type
        url: route.url
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
        dataType: 'json'
      )  

    sendFile(file)

  @getUserRoles: (id) ->
    $.getJSON R.Users.getUserRoles(id).url

  @userExists: (username) ->
    $.getJSON R.Users.primaryEmailExists(username).url

  @getTrash: ->
    $.getJSON R.Users.getPurgedUsers().url

  @labelUser: (id, labels) ->
    route = R.Users.addUserTags(id, labels)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @unLabelUser: (id, labels) ->
    route = R.Users.purgeUserTags(id, labels)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @getUserLabels: (id) ->
    $.getJSON R.Users.getUserTags(id).url


module.exports = users