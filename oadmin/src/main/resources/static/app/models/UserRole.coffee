Spine = require('spine')

class UserRole extends Spine.Model
  @configure 'UserRole', 'userId', 'role', 'grantedAt', 'grantedBy'
  
module.exports = UserRole