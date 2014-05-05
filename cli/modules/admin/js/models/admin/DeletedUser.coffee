
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
    
    $.Deferred (deferred) =>

      d = $.Deferred()

      d.done (users) => 
        @refresh(users, clear: yes)
        deferred.resolve @all()      
  
      app.modules['admin'].users.getUsersInTrash()
               .done d.resolve
             
module.exports = User