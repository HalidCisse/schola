Spine  = require('spine')
$      = require('jqueryify')

Mac = require('lib/mac')

class Session extends Spine.Module
  @include Spine.Events
  @include Spine.Log

  logPrefix: '(Session)'

  getAccessScope: (name) ->
    ;

  constructor: (@app) ->
    super

    isAllowedAccess = (s, application) =>
      return yes if s.superUser
      for right in application.accessRights when s.accessRights.map((r) -> r.name).contains(right.name)
        return yes
      no

    $.getJSON('/session')
       .done (session) =>

          return console.log('Error loading session!') if session.error

          if session.suspended or session.changePasswordAtNextLogin
            return setTimeout(-> @redirect '/')

          @app.session = session         

          @app.getApps().done (apps) =>

            res = []
            clbks = []

            for x in apps when isAllowedAccess(session, x)
              res.push "/api/v1/#{x.name}/javascriptRoutes"

            for y in apps when isAllowedAccess(session, y)
              res.push "/assets/#{y.name}.js"
              clbks["#{y.name}.js"] = => 
                Admin = require("#{y.name}")
                setTimeout =>
                  @app.modules["#{y.name}"] = new Admin(app: @app)

            for z in apps when isAllowedAccess(session, z)
              res.push "/assets/#{z.name}.css"

              setTimeout =>
                yepnope
                  load: res
                  callback: clbks
                  complete: =>                    
                    
                    $.ajaxSetup
                      beforeSend: (xhr, req) =>          

                        if req.url.search('/api/v1') is 0 # Authorization hdr only for API access
                          hdr = Mac.createHeader(session, xhr, req)

                          xhr.setRequestHeader('Authorization', hdr)

                          req.url = "#{@app.server}#{req.url}"
                          req.crossDomain = yes        

                          req.xhrFields =
                            withCredentials: yes

                    setTimeout (=> @app.trigger 'loaded'), 0

                    do @setUpLoginStatusChecks
                , 0

       .fail ->
          console.log 'No session found!'

  setUpLoginStatusChecks: (interval=30000) ->

    doCheckSession = =>

      $.getJSON('/session')
       .done (s) =>

          @log("getLoginStatus success:#{arguments}")

          return @app.trigger('session.error') if s.error

          @app.trigger 'session.loggedin', s

       .fail =>

          @log("getLoginStatus error:#{arguments}")

          @app.trigger 'session.error'

    do doCheckSession

    @_refreshInterval = setInterval(doCheckSession, interval)

  doLogout: =>
    @log("doLogout")

    $.getJSON('/api/v1/logout')
     .done => 

        @log("done doLogout:#{arguments}")

        @app.trigger 'session.loggedout'

module.exports = Session