
R = adminRoutes.controllers.admin.Users

class users

  @getUser: (id) ->
    $.getJSON R.getUser(id).url

  @getUsersStats: ->
    $.getJSON R.getUsersStats().url

  @getUsers: (page=0) ->
    $.getJSON R.getUsers(page).url

  @getUserRoles: (id) ->
    $.getJSON R.getUserRoles(id).url

  @saveUser: (spec) ->
    route = R.addUser()
    $.ajax(
      type: route.type
      url:  route.url
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify(spec)
    )    

  @updateUser: (id, spec) ->
    route = R.updateUser(id)
    $.ajax(
      type: route.type
      url:  route.url
      dataType: 'json',
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify(spec)
    )    

  @removeUsers: (users) ->
    route = R.deleteUsers(users)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )    

  @purgeUsers: (users) ->
    route = R.purgeUsers(users)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @undeleteUsers: (users) ->
    route = R.undeleteUsers(users)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @changePasswd: (id, newPasswd, oldPassword) ->
    route = R.changePasswd(id)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify({password: newPasswd, old_password: oldPassword})
    )    

  @setAddress: (id, spec) ->
    route = R.updateUser(id)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify(spec)
    )        

  @remHomeAddress: (id) ->
    route = R.updateUser(id)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify({homeAddress: {}})
    )

  @remWorkAddress: (id) ->
    route = R.updateUser(id)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
      contentType: 'application/json; charset=UTF-8'
      data: JSON.stringify({workAddress: {}})
    )    

  @updateContacts: (id, contacts) ->
    route = R.updateUser(id)
    $.ajax(
      type: route.type
      url: route.url
      contentType: 'application/json; charset=UTF-8'
      dataType: 'json'
      data: JSON.stringify({contacts})
    )       

  @grantRoles: (id, roles) ->
    route = R.grantUserRoles(id, roles)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @revokeRoles: (id, roles) ->
    route = R.revokeUserRoles(id, roles)
    $.ajax(
      type: route.type,
      url: route.url
      dataType: 'json'
    )    

  @purgeAvatar: (id, avatarId) ->
    route = R.purgeAvatar(id, avatarId)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )     

  @downloadAvatar: (avatarId) ->
    $.getJSON R.downloadAvatar(avatarId).url

  @uploadAvatar: (id, file) ->

    sendFile = ({type, url, file}) ->      
      $.ajax(
        type: type
        url: url
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
        dataType: 'json'
      )  

    {type, url} = R.uploadAvatar(id, file.name)
    sendFile({type, url, file})

  @getUserRoles: (id) ->
    $.getJSON R.getUserRoles(id).url

  @userExists: (username) ->
    $.getJSON R.userExists(username).url

  @getUsersInTrash: ->
    $.getJSON R.getPurgedUsers().url

  @labelUser: (id, labels) ->
    route = R.addUserTags(id, labels)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @unLabelUser: (id, labels) ->
    route = R.purgeUserTags(id, labels)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @getUserLabels: (id) ->
    $.getJSON R.getUserTags(id).url

module.exports = users