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

  
module.exports = User