require('lib/setup')

Spine   = require('spine')
Mac     = require('lib/mac')
Session = require('lib/session')
User    = require('controllers/User')
Role    = require('controllers/Role')
Menu    = require('controllers/Menu');

class App extends Spine.Module
  @include Spine.Log

  isLoggedIn: -> !! @sesssion.key

  server_host: 'localhost'

  server_port: 80  

  session: {}

  users: require('lib/users')

  roles: require('lib/roles')

  mgr: Menu.Mgr

  menu: (id) -> @mgr.getMenu(id)

  redirect: (path) ->
    Spine.Route.redirect path

  cleaned: (k) ->
    k = k.val()
    (k && k.trim()) || ''  

  constructor: ->
    super

    @server = "http://#{@server_host}:#{@server_port}"

    @sessionMgr = new Session

    @sessionMgr.on 'session.loggedin', (s) =>

      setTimeout -> @redirect '/' if s.user.changePasswordAtNextLogin

      @session = s

      $.ajaxSetup
        beforeSend: (xhr, req) =>          

          if req.url.search('/api/v1') is 0 # Authorization hdr only for API access
            hdr = Mac.createHeader(@session, xhr, req)

            xhr.setRequestHeader('Authorization', hdr)

            req.url = "#{app.server}#{req.url}"
            req.crossDomain = yes        

            req.xhrFields =
              withCredentials: yes

    @sessionMgr.on 'session.error', =>
      @session = {}

      $.ajaxSetup
        beforeSend: $.noop

    @sessionMgr.on 'session.loggedout', => @redirect '/'

    @sessionMgr.one 'session.loggedin', =>
      @log 'Loading application'

      @mgr.add {
        id: 'admin'
        header: 'ADMIN'
        items: [{
          id: Menu.USERS
          title: 'Users'
          icon: 'glyphicon-user'
          href: '/users'
          fn: $.noop
        },{
          id: Menu.TRASH
          title: 'Trash'
          icon: 'glyphicon-trash'
          href: '/trash'
          fn: $.noop
        },{
          id: Menu.ROLES
          title: 'Roles'
          icon: 'glyphicon-tags'
          href: '/roles'
          fn: $.noop
        },{
          id: Menu.STATS
          title: 'Stats'
          icon: 'glyphicon-stats'
          href: '/stats'
          fn: $.noop
        }]
      }
      
      @view = new App.Stack

  class @Stack extends Spine.Stack

    className: 'app spine stack'

    controllers:
      users  : User.Stack
      roles  : Role.Stack

    constructor: (args={el: '#content'}) ->
      super(args)

      Spine.Route.setup()

      if Spine.Route.getPath() in [ '/', '' ]
        @delay -> @navigate('/' + @pdefault)

    pdefault: 'roles'

module.exports = App
