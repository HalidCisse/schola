Session = require('js/lib/session')
Tag     = require('js/controllers/Tag')

### 

# Hack to support hierarchy of stacks

# Activating a child will activate all its direct parent stacks
# and deactivate all siblings (controllers or stacks), their parents and children recursively

###

Spine.Manager::change = (current, args...) ->

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
  @include Spine.Events

  isLoggedIn: -> !! @sesssion.key

  hostname: 'localhost'

  port: 80  

  server: "http://#{@::hostname}#{if @::port then ':' + @::port else ''}"

  session: {}
  
  labels: require('js/lib/labels')    

  redirect: (path) ->
    Spine.Route.redirect path

  cleaned: (k) ->
    k = k.val()
    (k && k.trim()) || ''

  modules: {}

  getApps: -> $.getJSON(jsRoutes.controllers.Utils.getApps().url)

  registerApp: (app) ->
    @topMenu.addApp(app.name, app.defaultUrl)

    @menu.addMenu(app.name, app.menu.controller)
    @menu.addRoute(route.path, route.callback.bind @menu) for route in app.menu.routes

    @view.addView(key, val) for key, val of app.controllers
    @view.addRoute(key, val) for key, val of app.routes    

  onLoad: =>

    @topMenu.addApp('archives', '#/archives')
    @topMenu.addApp('schools', '#/schools')
    
    @topMenu.render()

    @on 'session.loggedin', (s) =>

      if s.suspended or s.changePasswordAtNextLogin
        return setTimeout(-> @redirect '/')

      @session = s

    @on 'session.error', =>
      @session = {}

      $.ajaxSetup
        beforeSend: $.noop

    @on 'session.loggedout', => @redirect '/'

    Spine.Route.setup(redirect: => @navigate('/'))

    if Spine.Route.getPath() in [ '/', '' ]
      for _, _app of @modules
        @menu.delay -> @navigate(_app.defaultUrl)
        break

    @menu.delay -> 
      $('.scrollable').on 'scroll', ->
        $(@).siblings('.shadow')
            .css
              top: $(@).siblings('.toolbar').outerHeight(), 
              opacity: if @scrollTop is 0 then 0 else 0.8

    $(document.body).addClass('loaded')

  constructor: ->
    super    

    @on 'loaded', @onLoad

    @sessionMgr = new Session
    
    @view    = new App.Stack
    @menu    = new App.Menu
    @topMenu = new App.TopMenu

  class @TopMenu extends Spine.Controller

    apps: []

    @tmpl: require('js/views/util/top-menu/list').bind(window)

    constructor: (args={el: '#top-menu'}) ->
      super(args)

      @list = new Spine.List(template: TopMenu.tmpl, selectFirst: yes, className: 'dropdown-menu squared', attributes: {role: 'menu'})

      @list.on 'change', @changeApp

      @append @list.el

    changeApp: (app) =>
      @$('a.current-app').attr('href', app.defaultUrl)
      @$('a.current-app > .navbar-brand').html(S(app.name).capitalize().s)

    render: ->
      @el.addClass("top-menu-count-#{@apps.length}") 
      @list.render(@apps)

    addApp: (name, defaultUrl) =>
      @apps.push({name, defaultUrl})    

  class @Menu extends Spine.Stack

    className: 'menus spine stack'

    constructor: (args={el: '#sidebar'}) ->
      super(args)

    addMenu: (key, val) =>
      val.stack = @
      @[key] = val
      @add(@[key])

    addRoute: (key, val) =>
      callback = val if typeof val is 'function'
      callback or= => @[val].active(arguments...)
      @route(key, callback)      

  class @Stack extends Spine.Stack

    controllers: 
      labels : Tag.Stack

    className: 'app spine stack'

    constructor: (args={el: '#content'}) ->
      super(args)

    addView: (key, val) =>
      @[key] = new val(stack: @)
      @add(@[key])

    addRoute: (key, val) =>
      callback = val if typeof val is 'function'
      callback or= => @[val].active(arguments...)
      @route(key, callback)      


  arraysEqual: (a, b) ->

    return true if a is b
    
    return false if a is null or b is null
    
    return false if a.length isnt b.length

    a.sort()
    b.sort()

    for i in [0..a.length]
      return false if a[i] isnt b[i]
    
    true

module.exports = App
