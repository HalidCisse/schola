Spine = require('spine')
$     = require('jqueryify')

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

  fullName: -> "#{@givenName} #{@familyName}"

  @Reload: ->

    users = []
    completed = 0

    app.users.getUsersStats()
       .done ({'count': count}) =>

          PAGES = Math.ceil(count / @MaxResults)

          $.Deferred (deferred) =>

            deferred.progress (us) => 
              users.push(u) for u in us

              deferred.resolve(users) if ++completed is PAGES

            deferred.done (us) => 
              @refresh(us, clear: true)

            LoadPage = (index) ->
              app.users.getUsers(index)
                 .done deferred.notify

            LoadPage(i) for i in [0...PAGES] by 1

  @Defaults: {
    primaryEmail: '', 
    givenName: '', 
    familyName: '', 
    gender: 'Male',
    homeAddress: {city: window['sGeobytesCity'], country: window['sGeobytesIso2']},
    workAddress: {city: window['sGeobytesCity'], country: window['sGeobytesIso2']}
  }
  
module.exports = User