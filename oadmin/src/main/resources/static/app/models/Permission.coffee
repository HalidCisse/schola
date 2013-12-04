Spine = require('spine')

class Permission extends Spine.Model
  @configure 'Permission', 'name'
  
module.exports = Permission