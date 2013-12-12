Spine = require('spine')

class Users

  @mainTmpl: require('users/main.jade')

# Base class for toolbars
class Users.Toolsbar extends Spine.Controller

  constructor: ->
    super  

# For creating and modifying users
class Users.Form extends Spine.Controller

  constructor: ->
    super  

  @tmpl: require('users/form.jade')

# For viewing a user
class Users.One extends Spine.Controller

  constructor: ->
    super

  @tmpl: require('users/view.jade')

  # Actions to perform on a user
  class @Toolsbar extends Users.Toolsbar
  
    constructor: ->
      super   

    @tmpl: require('users/toolbar.user.jade')    

# For viewing all users
class Users.List extends Spine.Controller
  
  constructor: ->
    super   

  @tmpl: require('users/users.jade')

  # Actions to perform on all users or selected user(s)
  class @Toolsbar extends Users.Toolsbar 
  
    constructor: ->
      super   

    @tmpl: require('user/toolbar.users.jade')
    
module.exports = Users