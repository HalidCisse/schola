Spine   = require('spine')
Manager = require('spine/lib/manager')

UserM = require('models/User')
Menu  = require('controllers/Menu')

SelectionMgr = require('lib/selection')

position = require('lib/position')

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

class User

# For creating and modifying users
class User.Form extends Spine.Stack

  logPrefix: '(User.Form)'

  className: 'user form spine stack' 

  @defaultAvatarUrl: "http://www.gravatar.com/avatar/00000000000000000000000000000000?d=mm&f=y&s=195" 

  @elements:
    'fieldset'  : 'form'   
    '.save'     : 'saveBtn'    

  constructor: ->

    @controllers = {
      edit  : Form.Edit
      New   : Form.New      
    }

    @routes = {      
      '/users/new'      : 'New'
      '/user/:id/edit'  : (params) => @edit.active(params.id)    
    }

    super

    @edit.Save = @New.Save = @Save

    @active -> 
      @log("active")

  @tmpl: require('views/user/form')()
  @toolbarTmpl: require('views/user/form.toolbar')

  @UserInfo: (it, id=undefined) ->
    id: id

    primaryEmail: app.cleaned it.primaryEmail
    givenName: app.cleaned it.givenName
    familyName: app.cleaned it.familyName
    gender: app.cleaned it.gender
    
    homeAddress:{
      city: app.cleaned it.homeCity
      country: app.cleaned it.homeCountry
      postalCode: app.cleaned it.homePostalCode
      streetAddress: app.cleaned it.homeStreetAddress
    },
    
    workAddress:{
      city: app.cleaned it.workCity
      country: app.cleaned it.workCountry
      postalCode: app.cleaned it.workPostalCode
      streetAddress: app.cleaned it.workStreetAddress
    },
    
    contacts: {

      mobiles:{
        mobile1: app.cleaned it.mobile1
        mobile2: app.cleaned it.mobile2
      }, 
      
      home:{
        email: app.cleaned it.homeEmail
        phoneNumber: app.cleaned it.homePhoneNumber
        fax: app.cleaned it.homeFax
      }, 
      
      work:{
        email: app.cleaned it.workEmail
        phoneNumber: app.cleaned it.workPhoneNumber
        fax: app.cleaned it.workFax
      }
    }          

  Save: (user) =>
    @log "Save #{JSON.stringify(user)}"

    @form.prop('disabled', true)
    @saving()

    done = =>
      @doneSaving()
      @form.prop('disabled', false)            

    # app.users.upsertUser(user)
    #    .done (updatedOrNew) =>

    #       if user.id

    #         try
    #           UserM.find(user.id).refresh(updatedOrNew)
    #         catch ex
    #           @log "Error! can't find user for id=#{id}"
    #           UserM.refresh [updatedOrNew]

    #         done()
    #         @delay -> @navigate('/users')

    #         return

    #       UserM.refresh [updatedOrNew]

    #       done()
    #       @delay -> @navigate('/users')

    #       return

    #    .fail =>

    #       @log "Error while saving user #{JSON.stringify(user)}"
    #       done()

    done()
    ;

  saving: ->
    @saveBtn.button('saving')
    @el.addClass('saving')

  doneSaving: ->
    @el.removeClass('saving')
    @saveBtn.button('reset')      

  class @Toolbar extends Spine.Controller

    @events:
      'click .back'   : 'cancel'
      'click .save'   : 'save'    

    constructor: ->
      super  

    cancel: ->
      @navigate '/users'

    save: ->
      @trigger 'save'

  class @It extends Spine.Controller

    className: 'it'    

    @elements:
      '[name="primaryEmail"]'                       : 'primaryEmail'
      '[name="givenName"]'                          : 'givenName'
      '[name="familyName"]'                         : 'familyName'
      '[name="gender"]'                             : 'gender'

      '[name="workAddress[city]"]'                  : 'workCity'
      '[name="workAddress[country]"]'               : 'workCountry'
      '[name="workAddress[postalCode]"]'            : 'workPostalCode'
      '[name="workAddress[streetAddress]"]'         : 'workStreetAddress'

      '[name="homeAddress[city]"]'                  : 'homeCity'
      '[name="homeAddress[country]"]'               : 'homeCountry'
      '[name="homeAddress[postalCode]"]'            : 'homePostalCode'
      '[name="homeAddress[streetAddress]"]'         : 'homeStreetAddress'

      '[name="contacts[mobiles][mobile1]"]'         : 'mobile1'
      '[name="contacts[mobiles][mobile2]"]'         : 'mobile2'

      '[name="contacts[work][phoneNumber]"]'        : 'workPhoneNumber'
      '[name="contacts[work][email]"]'              : 'workEmail'
      '[name="contacts[work][fax]"]'                : 'workFax'

      '[name="contacts[home][phoneNumber]"]'        : 'homePhoneNumber'
      '[name="contacts[home][email]"]'              : 'homeEmail'
      '[name="contacts[home][fax]"]'                : 'homeFax'    
      
      '#avatar'                                     : 'avatar'          

    constructor: ->
      super    

      @avatarImg = new Image

      @avatarImg.onerror = =>
        @avatar[0].src = Form.defaultAvatarUrl

      @avatarImg.onload = =>
        @avatar[0].src = @avatarImg.src

      @render()

    render: ->
      @html Form.tmpl

      opts = (
        for country in countries
          "<option value='#{country['code']}'>#{country['name']}</option>"
      ).join('')

      @homeCountry.html(opts)
      @workCountry.html(opts)

      @el

    loadAvatar: (id) ->        
      @delay ->        
        @avatarImg.src = "/api/v1/avatar/#{id}"

    Load: (user) ->

      @primaryEmail.val user.primaryEmail
      @givenName.val user.givenName
      @familyName.val user.familyName
      @gender.val user.gender

      if avatar = user.avatar
        @loadAvatar(avatar)

      if homeAddress = user.homeAddress
        homeAddress.city and (@homeCity.val homeAddress.city)
        homeAddress.country and (@homeCountry.val homeAddress.country)
        homeAddress.postalCode and (@homePostalCode.val homeAddress.postalCode)
        homeAddress.streetAddress and (@homeStreetAddress.val homeAddress.streetAddress)

      if workAddress = user.workAddress
        workAddress.city and (@workCity.val workAddress.city)
        workAddress.country and (@workCountry.val workAddress.country)
        workAddress.postalCode and (@workPostalCode.val workAddress.postalCode)
        workAddress.streetAddress and (@workStreetAddress.val workAddress.streetAddress)   

      if contacts = user.contacts

        if mobiles = contacts.mobiles
          mobiles.mobile1 and (@mobile1.val mobiles.mobile1)
          mobiles.mobile2 and (@mobile2.val mobiles.mobile2)

        if home = contacts.home
          home.phoneNumber and (@homePhoneNumber.val home.phoneNumber)
          home.email and (@homeEmail.val home.email)
          home.fax and (@homeFax.val home.fax)

        if work = contacts.work
          work.phoneNumber and (@workPhoneNumber.val work.phoneNumber)
          work.email and (@workEmail.val work.email)
          work.fax and (@workFax.val work.fax)
 
  class @New extends Spine.Controller

    @TITLE: 'Create a new user'

    className: 'form new'

    tag: 'form'

    constructor: ->
      super    

      @toolbar = new Form.Toolbar(el: Form.toolbarTmpl(title: New.TITLE))  
      @it      = new Form.It            

      @active ->
        @log("New user")

        @it.Load(UserM.Defaults)

      @toolbar.on 'save', @doSave

      @render()

    render: ->
      @append @toolbar.el
      @append @it.el

    doSave: =>
      @Save Form.UserInfo(@it)

  class @Edit extends Spine.Controller

    @TITLE: 'Edit user'

    className: 'form edit'

    tag: 'form'

    constructor: ->
      super

      @toolbar = new Form.Toolbar(el: Form.toolbarTmpl(title: Edit.TITLE))  
      @it      = new Form.It      

      @active (id) ->
        @log("Edit user id=#{id}")

        try

          if user = UserM.find(id) 
            @it.Load(user.toJSON())
            @id = id         

        catch ex
          @log("Error finding User<#{id}>")

      @toolbar.on 'save', @doSave

      @render()

    render: ->
      @append @toolbar.el
      @append @it.el     

    doSave: =>
      @Save Form.UserInfo(@it, @id)       

# For viewing a user
class User.Single extends Spine.Controller

  logPrefix: '(User.Single)'

  className: 'user single'

  constructor: ->
    super

    @toolbar = new Single.Toolbar(el: Single.toolbarTmpl)  
    @it      = new Single.It

    @active (id) ->
      @log("active User<#{id}>")

      try

        @it.Load(user) if user = UserM.find(id)          

      catch ex
        @log("Error finding User##{id}")

  @tmpl: require('views/user/single')()
  @toolbarTmpl: require('views/user/single.toolbar')()

  render: ->
    @append @toolbar.el
    @append @it.el    

  class @It extends Spine.Controller

    className: 'it'

    constructor: ->
      super

      @render()

    render: ->
      @html Single.tmpl

    Load: (user) ->
      @stopListening(@user) if @user

      @user = user
      @listenTo @user, 'change', @FillData

      @FillData()

    FillData: ->
      # Fill data
      @el

  # Actions to perform on all users or selected user(s)
  class @Toolbar extends Spine.Controller 

    @events:
      'click .back'       : 'back'
      'click .refresh'    : 'refresh'
      'click .edit'       : 'edit'
      'click .purge'      : 'purge'

    constructor: ->
      super   

    back: ->
      @delay => @navigate('/users') 

    refresh: ->
      @log 'Single toolbar.refresh'
    
    edit: -> 
      @log 'Single toolbar.edit'
    
    purge: -> 
      @log 'Single toolbar.purge'

# For viewing all users
class User.List extends Spine.Controller

  logPrefix: '(User.List)'

  className: 'users'
  
  constructor: ->
    super   

    @selMgr = new SelectionMgr

    @selMgr.isWholePageSelected = => 
      for row in @rows
        if not @selMgr.isSelected(row.user)
          return no
      yes    

    @selMgr.on 'toggle-selection', @ToggleSelection

    @toolbar     = new List.Toolbar({@selMgr, el: List.toolbarTmpl})  
    @list        = new Spine.Controller(className: 'list')
    @contextmenu = new List.ContextMenu(el: List.contextmenuTmpl)

    @render()

    @active -> 
      @log('active')
      
      @delay -> 
        app.menu(Menu.USERS).loading()
        UserM.Reload()

    @toolbar.on 'next', @Next
    @toolbar.on 'prev', @Prev
    @toolbar.on 'purge', => @Purge(@selMgr.getSelection())

    UserM.on 'refresh', => @Reload(); app.menu(Menu.USERS).doneLoading()

    @contextmenu.on 'purge', (user) => @Purge([user])

  @rowTmpl: require('views/user/row')()
  @toolbarTmpl: require('views/user/list.toolbar')()
  @contextmenuTmpl: require('views/user/contextmenu')()

  start: 0

  Reload: =>
    @Refresh(UserM.slice(@start, @start + UserM.MaxResults))

  Next: =>
    @start += UserM.MaxResults
    @Reload()

  Prev: =>
    @start -= UserM.MaxResults if @start > 0
    @Reload()

  Purge: (users) ->
    ;

  rows: []

  add: (row) ->
    i = @rows.push(row)

    row.release =>
      delete @rows[i - 1] if @rows[i - 1] 

    row.on 'contextmenu', (evt, role) =>
      @contextmenu.Show(evt, role)             
    
    row

  @_indexof = (rec, all=UserM.all()) -> # Necessary access
    for r, i in all when rec.eql(r)
      return i
    -1    

  AppendOne: (user) ->
    @AppendMany [user]

  AppendMany: (users) ->
    if users.length

      all = UserM.all()

      @toolbar.page List._indexof(users[0], all), List._indexof(users[users.length - 1], all), all.length

      for user in users
        @list.append @add(delegate = new List.Row({user, @selMgr, el: List.rowTmpl})).el
        delegate.DelegateEvents()

  Refresh: (users) ->
    row.release() for row in @rows     

    @AppendMany users

  ToggleSelection: (isOn) =>    
    
    if isOn 
    
      @selMgr.selected(row.user) for row in @rows
    
    else 
    
      @selMgr.removed(row.user) for row in @rows

  render: ->
    @append @toolbar.el
    @append @list.el
    @append @contextmenu.el  

  class @ContextMenu extends Spine.Controller

    logPrefix: '(User.ContextMenu)'

    @events:
      'click .edit'    : 'edit'
      'click .purge'   : 'purge'    
      'click .view'    : 'view'    

    constructor: ->
      super

      @$(document).click(=> @el.hide())

    Show: (evt, @user) ->
      position.positionPopupAtPoint(evt.clientX, evt.clientY, @el[0])
      @el.show()
      
    edit: (e) ->
      @log("edit User<#{@user.id}>")

      e.preventDefault()

      @delay -> @navigate('/user', @user.id, 'edit')
      
      @trigger 'edit', @user

    purge: (e) ->
      @log("purge User<#{@user.id}>")

      e.preventDefault()
      
      @trigger 'purge', @user

    view: (e) ->
      @log("view User<#{@user.id}>")

      e.preventDefault()
      
      @delay -> @navigate('/user', @user.id)

      @trigger 'view', @user          

  # For viewing a user in the table
  class @Row extends Spine.Controller

    @Events:
      'click'           : 'clicked'   
      'click .select'   : 'select'
      'contextmenu'     : 'contextmenu'   

    constructor: ->
      super

      @selMgr.on "selectionChanged_#{@user.cid}", @selectionChanged
      @listenTo @user, 'change', @FillData

      @release -> 
        @selMgr.off "selectionChanged_#{@user.cid}", @selectionChanged

      @FillData()

    DelegateEvents: ->
      @delegateEvents(Row.Events)

    FillData: ->

      @el.toggleClass 'selected', @selMgr.isSelected(@user)
      
      @$('.fullName').html(@user.fullName())
      @$('.primaryEmail').html(@user.primaryEmail)

      @el

    contextmenu: (e) ->
      e.stopPropagation()
      e.preventDefault()

      @trigger 'contextmenu', e, @user      

    selectionChanged: (selected) => 
      @el.toggleClass 'selected', selected

    clicked: (evt) ->
      evt.stopPropagation()

      @delay -> @navigate('/user', @user.id)

    Off: (fn) ->
      @selMgr.off "selectionChanged_#{@user.cid}", @selectionChanged
      do fn
      @selMgr.on "selectionChanged_#{@user.cid}", @selectionChanged  
    
    select: (evt) ->
      evt.stopPropagation()

      @Off =>

        if @el.hasClass('selected')    
          @selMgr.removed(@user)
        else
          @selMgr.selected(@user)

      @el.toggleClass 'selected'

  # Actions to perform on all users or selected user(s)
  class @Toolbar extends Spine.Controller

    @elements:
      '.select'     : 'checkbox'
      '.next'       : 'next'
      '.prev'       : 'prev'

    @Events:
      'click .select'     : 'ToggleSelection'
      'click .new'        : 'New'
      'click .refresh'    : 'refresh'
      'click .purge'      : 'purge'    
      'click .next'       : 'Next'    
      'click .prev'       : 'Prev'  

    allSelected: no  

    constructor: ->
      super   

      @selMgr.on 'selectionChanged', @selectionChanged

      @release ->
        @selMgr.off 'selectionChanged', @selectionChanged

      @delegateEvents(Toolbar.Events)

    selectionChanged: => 
      @el.toggleClass 'has-selection', @selMgr.hasSelection()
      @el.toggleClass 'all', wholePage = @selMgr.isWholePageSelected()

      @allSelected = yes if wholePage

    Off: (fn) ->
      @selMgr.off 'selectionChanged', @selectionChanged
      do fn
      @selMgr.on 'selectionChanged', @selectionChanged 

    ToggleSelection: ->

      @el.toggleClass 'has-selection', @allSelected = not @allSelected
      @el.toggleClass 'all', @allSelected

      @Off =>
        @selMgr.trigger 'toggle-selection', @allSelected

    page: (start, end, total) ->
      if total > UserM.MaxResults # show paging only if necessary
        
        @next.prop('disabled', end is total - 1)
        @prev.prop('disabled', start is 0)

        @$('.start').html(start + 1)
        @$('.end').html(end + 1)
        @$('.count').html("#{total} &nbsp;")

      @el.toggleClass 'needs-paging', total > UserM.MaxResults
    
    New: ->
      @delay -> @navigate('/users/new')

    refresh: ->
      return if @selMgr.hasSelection()

      @delay ->
        app.menu(Menu.USERS).loading()
        UserM.Reload()
    
    purge: ->
      @log 'List toolbar.purge'       
    
    Next: -> 
      @trigger 'next'         
    
    Prev: -> 
      @trigger 'prev'         
    
class User.Stack extends Manager.Stack

  logPrefix: '(User.Stack)'

  className: 'spine stack users'

  controllers:
    list    : User.List
    single  : User.Single
    form    : User.Form

  constructor: (opts) ->

    @routes = {      
      '/users'     : 'list'
      '/user/:id'  : (params) => @single.active(params.id)
    }

    super

    @active =>
      @log('active')

    @manager.on 'change', -> app.menu(Menu.USERS).activate()

module.exports = User