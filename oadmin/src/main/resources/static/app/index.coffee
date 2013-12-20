require('lib/setup')

Spine   = require('spine')
Session = require('lib/session')

class App extends Spine.Controller
  
  session: new Session(@)

  users: require('lib/users')

  roles: require('lib/roles')

  constructor: ->
    super
    
    # Getting started - should be removed
    @html require("views/sample")({version:Spine.version})

module.exports = App
