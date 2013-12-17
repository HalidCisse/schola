Spine = require('spine')

class User extends Spine.Model
  
  @configure 'User', 
    'id', 
    'email', 
    'firstname', 
    'lastname', 
    'gender', 
    'homeAddress', 
    'workAddress', 
    'contacts', 
    'avatar', 
    'createdAt', 
    'createdBy', 
    'lastModifiedAt', 
    'lastModifiedBy'  

  grantRoles: (roles) ->

  revokeRoles: (roles) ->

  addContacts: (contacts) ->

  purgeContacts: (contacts) ->

  addAddress: (address) ->

  purgeHomeAddress: (address) ->      
  
  purgeWorkAddress: (address) ->      

  
module.exports = User