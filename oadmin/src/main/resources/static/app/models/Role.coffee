Spine = require('spine')

class Role extends Spine.Model
  @configure 'Role', 'name', 'parent', 'public', 'createdAt', 'createdBy'
  
module.exports = Role