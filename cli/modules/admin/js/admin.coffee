Spine   = require('spine')
Manager = require('spine/lib/manager')

UserM = require('models/admin/User')
TagM  = require('models/Tag')

User  = require('controllers/admin/User')
Trash = require('controllers/admin/Trash')
Menu  = require('controllers/Menu')

class Admin extends Spine.Controller

  USERS                  :   'users'
  TRASH                  :   'trash'
  ACCESS_RIGHTS          :   'access_rights'
  USERS_CREATED_BY_ME    :   'users.created_by_me'
  USERS_ACTIVE           :   'users.active'
  USERS_SUSPENDED        :   'users.suspended'
  USERS_SETTINGS         :   'settings.users'
  USERS_STATS            :   'stats.users'  

  class @MenuMgr extends Menu.Mgr

    constructor: ->
      super
      
    delegate: -> 
      controller: new MenuMgr.S(mgr:@)
      routes: @routes

    setButton: (@button) ->

    routes: [
      {path: /^\/user/                   , callback: -> @admin.active(); app.modules['admin'].menu(app.modules['admin'].USERS)},
      {path: '/users-byme'               , callback: -> @admin.active(); app.modules['admin'].menu(app.modules['admin'].USERS_CREATED_BY_ME)},
      {path: '/users-active'             , callback: -> @admin.active(); app.modules['admin'].menu(app.modules['admin'].USERS_ACTIVE)},
      {path: '/users-suspended'          , callback: -> @admin.active(); app.modules['admin'].menu(app.modules['admin'].USERS_SUSPENDED)},
      {path: '/trash/users'              , callback: -> @admin.active(); app.modules['admin'].menu(app.modules['admin'].TRASH)},
      {path: '/settings/users'           , callback: -> @admin.active(); app.modules['admin'].menu(app.modules['admin'].USERS_SETTINGS)}
      {path: '/labelled/users/:label'    , callback: (params) -> @admin.active(); app.modules['admin'].menu(decodeURIComponent(S(params.label).dasherize().chompLeft('-').s))}
    ]

    class @S extends Spine.Controller
      @include Spine.Log

      logPrefix: '(Admin.Menu)'

      className: 'menu-admin'
      
      constructor: ->
        super

        @append @mgr.button if @mgr.button

        for key, value of @mgr.menus()
          @append value

        @active =>
          @log 'Admin-Menu active'      

  @mgr: new Admin.MenuMgr

  menu: (id, clbk) -> Admin.mgr.activate(id, clbk)

  users: require('lib/admin/users')

  @labelTmpl: require('views/admin/menu/tag')
  @stateTmpl: require('views/menu/link')  

  defaultUrl: '/users'

  name: 'admin'

  constructor: ->
    super

    renderLabel = (label) -> Admin.labelTmpl(label)
    
    renderState = (state) =>
      
      countClass = switch state
        when @USERS_CREATED_BY_ME then 'byme'
        when @USERS_ACTIVE        then 'active'
        when @USERS_SUSPENDED     then 'suspended'

      (item) ->
        item.countClass = "count-#{countClass}"
        Admin.stateTmpl(item)

    getUsersChildMenus = =>
      $.Deferred (deferred) =>
        app.on 'loaded', =>
          TagM.Reload().done (objs) =>

            labels = 
              for obj in objs
                id: S(obj.name).dasherize().chompLeft('-').s,
                title: S(obj.name).capitalize().s,
                render: renderLabel,
                href: "/labelled/users/#{encodeURIComponent(obj.name)}"
      
            deferred.resolve [{
              id: @USERS_CREATED_BY_ME
              title: 'Created by me'
              render: renderState(@USERS_CREATED_BY_ME)
              # icon: 'glyphicon-trash'
              href: '/users-byme'
            },{
              id: @USERS_ACTIVE
              title: 'Active'
              render: renderState(@USERS_ACTIVE)
              # icon: 'glyphicon-stats'
              href: '/users-active'
              state: 'users-active'
            },{
              id: @USERS_SUSPENDED
              title: 'Suspended'
              render: renderState(@USERS_SUSPENDED)
              # icon: 'glyphicon-trash'
              href: '/users-suspended'
              state: 'suspended'
            }].concat labels

            if objs.length > 6
              
              setTimeout ->

                jQuery('ul.children:not(:empty)').slimscroll({height: '203px'})  

                jQuery('ul.children:not(:empty)')
                    .parent()
                    .siblings('.shadow')
                    .css
                      opacity: 0.4          

                # jQuery('ul.children:not(:empty)').on 'scroll', ->
                    
                #     $(@).parent()
                #         .siblings('.shadow.up')
                #         .css
                #           opacity: if @scrollTop is 0 then 0 else 0.6

                #     $(@).parent()
                #         .siblings('.shadow.down')
                #         .css
                #           opacity: if @scrollTop is 0 then 0.6 else 0                         

    Admin.mgr.setButton new Admin.NewButton

    Admin.mgr.add
      id: 'admin'
      header: 'ADMIN'
      items: [{
        id: @USERS
        title: 'Users'
        icon: 'glyphicon-user'
        href: '/users'
        getChildren: getUsersChildMenus
      },{
        id: @ACCESS_RIGHTS
        title: 'Access Rights'
        icon: 'glyphicon-tags'
        href: '/roles'
      },{
        id: @TRASH
        title: 'Trash'
        icon: 'glyphicon-trash'
        href: '/trash/users'
      },{
        id: @USERS_STATS
        title: 'Stats'
        icon: 'glyphicon-stats'
        href: '/stats/users'
      },{
        id: @USERS_SETTINGS
        title: 'Settings'
        icon: 'glyphicon-wrench'
        href: '/settings/users'
      }]

    @app.registerApp(
      name: @name
      defaultUrl: @defaultUrl
      menu: Admin.mgr.delegate()
      controllers:
        users  : User.Stack
        trash  : Trash.Stack
      routes: {}
    )

    UserM.on 'refresh', =>
      jQuery('.count-byme').text numeral(UserM.countUsersOf(app.session.user.id)).format()
      jQuery('.count-active').text numeral(UserM.countActive()).format()
      jQuery('.count-suspended').text numeral(UserM.countSuspended()).format()

    console.log 'Loaded #admin module'

  class @NewButton extends Spine.Controller

    @tmpl: require('views/admin/menu/new')()

    className: 'admin menu new-user'

    events:
      'click button.new-user' : 'clicked'    

    constructor: ->
      super

      @render()

    render: ->
      @html NewButton.tmpl

    clicked: (e) =>
      e.preventDefault()
      e.stopPropagation()

      @navigate('/users/new')

module.exports = Admin
