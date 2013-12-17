Spine = require('spine')

class Permission extends Spine.Model
  @configure 'Permission', 'name', 'clientId'
  
module.exports = Permission