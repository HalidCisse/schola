require('lib/setup')

Spine   = require('spine')
Mac     = require('lib/mac')
Session = require('lib/session')

class App extends Spine.Controller

  isLoggedIn: -> !!@sesssion.key

  session: {}
  
  constructor: ->
    super

    @sessionMgr = new Session(@)

    @users = require('lib/users')

    @roles = require('lib/roles')

    @sessionMgr.on('session.loggedin', (s) =>

      @delay -> @navigate('/') if s.user.changePasswordAtNextLogin

      $.extend(true, @session, s)

      $.ajaxSetup(
        beforeSend: (xhr, req) =>
          hdr = Mac.createHeader(@session, xhr, req)
          xhr.setRequestHeader('Authorization', hdr)
      )
    )

    @sessionMgr.on('session.error', =>
      @session = {}

      $.ajaxSetup(
        beforeSend: null
      )

      #
    )
    
    @sessionMgr.on('session.loggedout', => document.location = '/')    
    
    # Getting started - should be removed
    # @html require("views/sample")({version:Spine.version})

module.exports = App
