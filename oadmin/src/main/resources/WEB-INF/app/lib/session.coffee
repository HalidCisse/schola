Spine  = require('spine')
$      = require('jqueryify')

class Session extends Spine.Module
  @include Spine.Events
  @include Spine.Log

  logPrefix: '(Session)'

  constructor: (@app) ->
    super

    do @getLoginStatus

  getLoginStatus: =>

    doCheckSession = =>

      x = $.ajax(
        type:"GET",
        url: "/session",
        dataType: 'json'
      )

      x.success (s) =>
        @log("getLoginStatus success:", arguments)

        return @trigger('session.error') if s.error

        @trigger 'session.loggedin', s

        false

      x.error =>

        @log("getLoginStatus error:", arguments)

        @trigger 'session.error'

        false

    do doCheckSession

    @_refreshInterval = setInterval(doCheckSession, 15000)

  doLogout: =>
    @log("doLogout")

    $.ajax(
      type: "GET",
      url: "/api/v1/logout",
      dataType: 'json'
    ).done => 

        @log("done doLogout:", arguments)

        @trigger 'session.loggedout'

module.exports = Session