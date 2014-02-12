require('lib/setup')

Spine   = require('spine')
Manager = require('spine/lib/manager')

Mac     = require('lib/mac')
Session = require('lib/session')
User    = require('controllers/User')
Role    = require('controllers/Role')
Menu    = require('controllers/Menu');

### 

# Hack to support hierarchy of stacks

# Activating a child will activate all its direct parent stacks
# and deactivate all siblings (controllers or stacks), their parents and children recursively

###
Manager::change = (current, args...) ->

  deactivate = (cont) ->
    cont.deactivate(args...)

    if cont.controllers
      deactivate(child) for child in cont.controllers

  for cont in @controllers when cont isnt current
    deactivate(cont)

  if current

    if current.stack and parent = current.stack.stack # Test `current.stack` to make sure in cases where Manager is used without stacks
      parent.manager.trigger('change', current.stack, args...)

    current.activate(args...)

class App extends Spine.Module
  @include Spine.Log

  isLoggedIn: -> !! @sesssion.key

  server_host: 'localhost'

  server_port: 80  

  session: {}

  users: require('lib/users')

  roles: require('lib/roles')
  
  labels: require('lib/labels')

  mgr: Menu.Mgr

  menu: (id) -> @mgr.activate(id)

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

      @delay -> 
        @$('.scrollable').on 'scroll', -> 
          $(this).siblings('.shadow')
                 .css({
                    top: $(this).siblings('.toolbar').outerHeight(), 
                    opacity: if this.scrollTop is 0 then 0 else 0.8
                  })

    pdefault: 'users'

module.exports = App
