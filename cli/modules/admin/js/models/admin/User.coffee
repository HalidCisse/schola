Spine = require('spine')
$     = require('jqueryify')

admin = require('admin')

class User extends Spine.Model

  @MaxResults: 50

  @configure 'User', 
    'id', 
    'primaryEmail', 
    'givenName', 
    'familyName', 
    'gender', 
    'homeAddress', 
    'workAddress', 
    'contacts', 
    'avatar', 
    'lastLoginTime', 
    'createdAt', 
    'createdBy', 
    'lastModifiedAt', 
    'lastModifiedBy' 
    'suspended'

  @comparator: (a, b) ->

    if a.lastLoginTime

      if b.lastLoginTime
        
        if (res = a.lastLoginTime - b.lastLoginTime) is 0

          if a.lastModifiedAt

            if b.lastModifiedAt
              return a.lastModifiedAt - b.lastModifiedAt

            return 1

          return 1 if b.lastModifiedAt         

          return a.createdAt - b.createdAt

        else return res

      return 1

    return 1 if b.lastLoginTime

    if a.lastModifiedAt

      if b.lastModifiedAt
        if (res = a.lastModifiedAt - b.lastModifiedAt) is 0
          return a.createdAt - b.createdAt
        else return res

      return 1

    return 1 if b.lastModifiedAt         

    return a.createdAt - b.createdAt

  fullName: -> "#{@givenName} #{@familyName}"

  @Reload: ->

    $.Deferred (d) =>
  
      users = []
      completed = 0

      app.modules['admin'].users.getUsersStats()
         .done ({'count': count}) =>

            PAGES = Math.ceil(count / @MaxResults)

            if PAGES is 0
              @refresh([], clear: yes)
              d.resolve @all()
              return

            $.Deferred (deferred) =>

              deferred.progress (us) =>              
                users = [].concat.apply(users, us)

                deferred.resolve(users) if ++completed is PAGES

              deferred.done (us) =>
               @refresh(us, clear: yes)

              LoadPage = (index) ->
                app.modules['admin'].users.getUsers(index)
                   .done deferred.notify

              LoadPage(i) for i in [0...PAGES] by 1

  @countUsersOf: (id) =>

    byHim = =>
      @select (user) ->
        user.createdBy is id

    byHim().length

  @countSuspended: =>

    suspended = =>
      @select (user) -> 
        user.suspended

    suspended().length    

  @countActive: =>

    active = =>
      @select (user) -> 
        not user.suspended

    active().length        

  @Defaults: {
    primaryEmail: '', 
    givenName: '', 
    familyName: '', 
    gender: 'Male',
    homeAddress: {city: window['sGeobytesCity'], country: window['sGeobytesIso2']},
    workAddress: {city: window['sGeobytesCity'], country: window['sGeobytesIso2']}
  }
  
module.exports = User