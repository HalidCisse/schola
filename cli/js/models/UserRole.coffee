Spine = require('spine')

class UserRole extends Spine.Model
  @configure 'UserRole', 'userId', 'role', 'grantedAt', 'grantedBy', 'delegated'
  
module.exports = UserRole