Spine = require('spine')

class User extends Spine.Model
  
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
    'createdAt', 
    'createdBy', 
    'lastModifiedAt', 
    'lastModifiedBy'     

  
module.exports = User