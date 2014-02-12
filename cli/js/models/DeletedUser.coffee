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
    app.users.getPurgedUsers()
             .done (users) => @refresh(users, clear: yes)
             
module.exports = User