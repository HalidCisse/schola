Spine = require('spine')

class RolePermission extends Spine.Model
  
  @configure 'RolePermission', 
    'role', 
    'permission', 
    'grantedAt', 
    'grantedBy'
  
module.exports = RolePermission