Spine = require('spine')

class User extends Spine.Model
  
  @configure 'User', 
    'id', 
    'username', 
    'firstname', 
    'lastname', 
    'gender', 
    'homeAddress', 
    'workAddress', 
    'contacts', 
    'createdAt', 
    'createdBy', 
    'lastModifiedAt', 
    'lastModifiedBy'
  
module.exports = User