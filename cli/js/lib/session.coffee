Spine  = require('spine')
$      = require('jqueryify')

class Session extends Spine.Module
  @include Spine.Events
  @include Spine.Log

  logPrefix: '(Session)'

  constructor: ->
    super

    do @getLoginStatus

  getLoginStatus: ->

    doCheckSession = =>

      $.getJSON('/session')
       .done (s) =>

          @log("getLoginStatus success:", arguments)

          return @trigger('session.error') if s.error

          @trigger 'session.loggedin', s

       .fail =>

          @log("getLoginStatus error:", arguments)

          @trigger 'session.error'

    do doCheckSession

    @_refreshInterval = setInterval(doCheckSession, 30000)

  doLogout: =>
    @log("doLogout")

    $.getJSON('/api/v1/logout')
     .done => 

        @log("done doLogout:", arguments)

        @trigger 'session.loggedout'

module.exports = Session