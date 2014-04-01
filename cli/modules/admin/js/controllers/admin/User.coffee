Spine   = require('spine')
Manager = require('spine/lib/manager')

UserM     = require('models/admin/User')
TagM      = require('models/Tag')
UserTagM  = require('models/admin/UserTag')
Menu      = require('controllers/Menu')

LabelForm = require('controllers/LabelForm')

SelectionMgr = require('lib/selection')

position = require('lib/position')

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

  @toolbarTmpl: require('views/admin/user/form.toolbar')

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

    # amin.users.upsertUser(user)
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

    className: 'it scrollable'    

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
      @html @tmpl

      opts = (
        for country in countries
          "<option value='#{country['code']}'>#{country['name']}</option>"
      ).join('')

      @homeCountry.html(opts).chosen(allow_single_deselect: yes, width:"330px")
      @workCountry.html(opts).chosen(allow_single_deselect: yes, width:"330px")

      @$('.modules select').chosen({disable_search: true, width: '330px'})

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

    className: 'user form new'

    tag: 'form'

    @tmpl: require('views/admin/user/form.new')()

    calcHeight: -> $(window).height() - $('.selections').outerHeight() - @toolbar.el.outerHeight() - $('.navbar').outerHeight() - 8

    constructor: ->
      super    

      @toolbar = new Form.Toolbar(el: Form.toolbarTmpl(title: New.TITLE))  
      @it      = new Form.It(tmpl: New.tmpl) 

      @$(window).resize(=> @it.el.css({height: @calcHeight()}))       

      @active ->
        @log("New user")

        @it.Load(UserM.Defaults)

        @delay -> @it.el.css({height: @calcHeight()})

      @toolbar.on 'save', @doSave

      @render()

    render: ->
      @append @toolbar.el
      @append @it.el

    doSave: =>
      @Save Form.UserInfo(@it)

  class @Edit extends Spine.Controller

    @TITLE: 'Edit user'

    className: 'user form edit'

    tag: 'form'

    @tmpl: require('views/admin/user/form.edit')()

    calcHeight: -> $(window).height() - $('.selections').outerHeight() - @toolbar.el.outerHeight() - $('.navbar').outerHeight() - 8

    constructor: ->
      super

      @toolbar = new Form.Toolbar(el: Form.toolbarTmpl(title: Edit.TITLE))  
      @it      = new Form.It(tmpl: Edit.tmpl)

      @$(window).resize(=> @it.el.css({height: @calcHeight()}))

      @active (id) ->
        @log("Edit user id=#{id}")

        try

          if user = UserM.find(id) 
            @it.Load(user.toJSON())
            @id = id         

        catch ex
          @log("Error finding User<#{id}>")

        @delay -> @it.el.css({height: @calcHeight()})

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

  @tmpl: require('views/admin/user/single')()
  @toolbarTmpl: require('views/admin/user/single.toolbar')()

  render: ->
    @append @toolbar.el
    @append @it.el    

  class @It extends Spine.Controller

    className: 'it scrollable'

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

  calcHeight: -> $(window).height() - @$('.selections').outerHeight() - @toolbar.el.outerHeight() - $('.navbar').outerHeight() - 8

  @ALL: 'all'
  @OWN: 'byme'
  @ACTIVE: 'active'
  @SUSPENDED: 'suspended'
  @LABELLED: 'labelled'
  
  @SELECT_ALL: 'select:all'
  @SELECT_NONE: 'select:none'
  @SELECT_ACTIVE: 'select:active'
  @SELECT_SUSPENDED: 'select:suspended'
  
  constructor: ->
    super   

    @selMgr = new SelectionMgr

    @selMgr.isWholePageSelected = => 
      for row in @rows
        if not @selMgr.isSelected(row.user)
          return no
      yes

    @selMgr.isAllSelected = => 
      for user in UserM.select(@getFilter())
        if not @selMgr.isSelected(user)
          return no
      yes          

    @selMgr.on 'toggle-selection', @ToggleSelection

    @toolbar     = new List.Toolbar({@selMgr, el: List.toolbarTmpl, mgr:@})  
    @list        = new List.Body({@selMgr, mgr: @})
    @contextmenu = new List.ContextMenu(el: List.contextmenuTmpl)        

    @render()

    # @$('.scrollable').slimScroll(height: @list.calcHeight())

    @active (@state, label) -> 
      @log("active State<#{@state}>")

      @label = decodeURIComponent(label)
      
      @delay ->

        switch @state
          when List.ALL         then app.modules['admin'].menu(app.modules['admin'].USERS, (item) -> item.loading())
          when List.OWN         then app.modules['admin'].menu(app.modules['admin'].USERS_CREATED_BY_ME, (item) -> item.loading())
          when List.ACTIVE      then app.modules['admin'].menu(app.modules['admin'].USERS_ACTIVE, (item) -> item.loading())
          when List.SUSPENDED   then app.modules['admin'].menu(app.modules['admin'].USERS_SUSPENDED, (item) -> item.loading())
          when List.LABELLED    then app.modules['admin'].menu(S(@label).dasherize().chompLeft('-').s, (item) -> item.loading())

        UserM.Reload()

        @delay -> @list.calcHeight()

    @toolbar.on 'next', @Next
    @toolbar.on 'prev', @Prev
    @toolbar.on 'purge', => @Purge(@selMgr.getSelection())

    UserM.on 'refresh', => 
      @Reload()

      switch @state
          when List.ALL         then app.modules['admin'].menu(app.modules['admin'].USERS, (item) -> item.doneLoading())
          when List.OWN         then app.modules['admin'].menu(app.modules['admin'].USERS_CREATED_BY_ME, (item) -> item.doneLoading())
          when List.ACTIVE      then app.modules['admin'].menu(app.modules['admin'].USERS_ACTIVE, (item) -> item.doneLoading())
          when List.SUSPENDED   then app.modules['admin'].menu(app.modules['admin'].USERS_SUSPENDED, (item) -> item.doneLoading())
          when List.LABELLED    then app.modules['admin'].menu(S(@label).dasherize().chompLeft('-').s, (item) -> item.doneLoading())

    @contextmenu.on 'purge', (user) => @Purge([user])

  @tmpl: require('views/admin/user/list')({perPage: UserM.MaxResults})
  @rowTmpl: require('views/admin/user/row')()
  @toolbarTmpl: require('views/admin/user/list.toolbar')()
  @contextmenuTmpl: require('views/admin/user/contextmenu')()

  start: 0

  getFilter: ->    

    switch @state
        when List.ALL         then -> yes
        when List.OWN         then (user) -> user.createdBy is app.session.user.id
        when List.ACTIVE      then (user) -> not user.suspended
        when List.SUSPENDED   then (user) -> user.suspended
        when List.LABELLED    then label = @label; (user) -> user.labels.indexOf(label) > -1    

  Reload: =>
    @Refresh UserM.select(@getFilter()).slice(@start, @start + UserM.MaxResults)

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
    index = @rows.push(row)

    row.on 'release', =>
      @rows.splice(index - 1, 1)

    row.on 'contextmenu', (evt, user) =>
      @contextmenu.Show(evt, user)             
    
    row

  @_indexof = (rec, all=UserM.select(@getFilter())) -> # Necessary access
    for r, i in all when rec.eql(r)
      return i
    -1    

  AppendOne: (user) ->
    @AppendMany [user]

  AppendMany: (users) ->
    if users.length

      all = UserM.select(@getFilter())

      @toolbar.page List._indexof(users[0], all), List._indexof(users[users.length - 1], all), all.length

      for user in users
        @list.addUser @add(delegate = new List.Row({user, @selMgr, el: List.rowTmpl})).el
        delegate.DelegateEvents()

    else

      @toolbar.page -1, -1, 0              

  Refresh: (users) ->
    @rows[lastIndex - 1].release() while lastIndex = @rows.length     

    @delay -> @AppendMany(users)

  ToggleSelectionAll: (selectionState) =>

    switch selectionState
      
      when List.SELECT_ALL

        @selMgr.selected(user) for user in UserM.select(@getFilter())

      when List.SELECT_NONE

        @selMgr.removeAll()
        @toolbar.allSelected = no
      
      when List.SELECT_ACTIVE

        for user in UserM.select(@getFilter())
          if not user.suspended
            @selMgr.selected(user)
          else
            @selMgr.removed(user)
      
      when List.SELECT_SUSPENDED 

        for user in UserM.select(@getFilter())
          if user.suspended
            @selMgr.selected(user)
          else
            @selMgr.removed(user)
  
  ToggleSelection: (selectionState) =>    
    
    switch selectionState
      
      when List.SELECT_ALL

        @selMgr.selected(row.user) for row in @rows

      when List.SELECT_NONE

        selectedOnes = @selMgr.getSelection()
        @selMgr.removed(selectedOnes[lastIndex - 1]) while lastIndex = selectedOnes.length
        
        @toolbar.allSelected = no

        @delay -> @toolbar.selectionChanged()
        @delay -> @list.selectionChanged()
      
      when List.SELECT_ACTIVE

        for row in @rows
          if not row.user.suspended
            @selMgr.selected(row.user)
          else
            @selMgr.removed(row.user)
      
      when List.SELECT_SUSPENDED 

        for row in @rows
          if row.user.suspended
            @selMgr.selected(row.user)
          else
            @selMgr.removed(row.user)

  render: ->
    @append @toolbar.el
    @append @list.el
    @append @contextmenu.el  

  class @Body extends Spine.Controller

    className: 'body'

    @elements:
      '.list'        : 'list'
      '.scrollable'  : 'scrollable'

    @events:
      'click .clear'        : 'clearAll'   
      'click .select-all'   : 'selectAll'

    calcHeight: => 
      @scrollable.css({
        height: @mgr.calcHeight()
        })
    
    constructor: ->
      super

      @selMgr.on 'selectionChanged', @selectionChanged
      @on 'page-selected', @selectionChanged

      @on 'clear-all', =>
        @mgr.ToggleSelectionAll(List.SELECT_NONE) 
        @delay => @calcHeight()

      @on 'select-all', => 
        @mgr.ToggleSelectionAll(List.SELECT_ALL) 
        @delay => @calcHeight()

      @on 'select', (@state) =>
        @mgr.ToggleSelection(state) 
        @delay => @calcHeight()     

      @render()

      @$(window).resize(=> @calcHeight())

    selectionChanged: =>
      needMorThanOnePage = UserM.select(@mgr.getFilter()).length > UserM.MaxResults
      
      @el.toggleClass 'page-selected', needMorThanOnePage and @selMgr.isWholePageSelected()
      @el.toggleClass 'all-selected', needMorThanOnePage and @selMgr.isAllSelected()
      
      @calcHeight()

    Off: (fn) =>
      @selMgr.off 'selectionChanged', @selectionChanged
      do fn
      @selMgr.on 'selectionChanged', @selectionChanged

    render: ->
      @html List.tmpl

    addUser: (user) ->
      @list.append user

    clearAll: (e) ->
      e.stopPropagation()
      e.preventDefault()

      @el.removeClass 'all-selected'
      @el.removeClass 'page-selected'

      @Off =>
        @trigger 'clear-all'

    selectAll: (e) ->
      e.stopPropagation()
      e.preventDefault()

      @el.addClass 'all-selected'
      @el.removeClass 'page-selected'

      @Off =>
        @trigger 'select-all'
  
  class @ContextMenu extends Spine.Controller

    logPrefix: '(User.ContextMenu)'

    @events:
      'click .edit'    : 'edit'
      'click .purge'   : 'purge'  
      'click .disable' : 'suspend'  

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

    suspend: (e) ->
      @log("suspend User<#{@user.id}>")

      e.preventDefault()
      
      @trigger 'suspend', @user          

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

      @on 'release', => 
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
      '.select'                     : 'checkbox'
      '.labels-menu'                : 'labelsElement'
      '.next'                       : 'next'
      '.prev'                       : 'prev'
      '.select-toggle-wrapper'      : 'selectToggle'

    @Events:
      'click .select'     : 'ToggleSelection'
      'click .new'        : 'New'
      'click .refresh'    : 'refresh'
      'click .purge'      : 'purge'    
      'click .next'       : 'Next'    
      'click .prev'       : 'Prev'

    allSelected: no

    class @SelectMenu extends Spine.Controller

      events:
        'click a' : 'select'

      constructor: ->
        super

      select: (e) ->
        e.preventDefault()

        @trigger 'select', @$(e.target).attr('data-state')

    constructor: ->
      super   

      @selMgr.on 'selectionChanged', @selectionChanged

      @bind 'release', =>
        @selMgr.off 'selectionChanged', @selectionChanged

      new Toolbar.Tags(el: @labelsElement, mgr: @)

      @delegateEvents(Toolbar.Events)

      (new Toolbar.SelectMenu(el: @$('.selection-menu'))).on 'select', (state) => @mgr.list.trigger 'select', state

      @selectToggle.dropdown()
                   .closest('#users-select').on 'show.bs.dropdown', => 
                      @delay (-> @selectToggle.closest('button.select').tooltip('hide')), 10

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
        @mgr.list.Off =>
          @selMgr.trigger 'toggle-selection', if @allSelected then List.SELECT_ALL else List.SELECT_NONE

      @mgr.list.trigger 'page-selected'

    page: (start, end, total) ->
      if total > UserM.MaxResults # show paging only if necessary
        
        @next.prop('disabled', end is total - 1)
        @prev.prop('disabled', start is 0)

        @$('.start').html(start + 1)
        @$('.end').html("#{end + 1}&nbsp;")
        @$('.count').html("#{numeral(total).format()} &nbsp;")

      @el.toggleClass 'needs-paging', total > UserM.MaxResults
      @el.toggleClass 'empty', total is 0
    
    New: ->
      @delay -> @navigate('/users/new')

    refresh: ->
      return if @selMgr.hasSelection()

      @delay ->
      
        switch @mgr.state
            when List.ALL         then app.modules['admin'].menu(app.modules['admin'].USERS, (item) -> item.loading())
            when List.OWN         then app.modules['admin'].menu(app.modules['admin'].USERS_CREATED_BY_ME, (item) -> item.loading())
            when List.ACTIVE      then app.modules['admin'].menu(app.modules['admin'].USERS_ACTIVE, (item) -> item.loading())
            when List.SUSPENDED   then app.modules['admin'].menu(app.modules['admin'].USERS_SUSPENDED, (item) -> item.loading())
            when List.LABELLED    then app.modules['admin'].menu(app.modules['admin'].USERS, (item) -> item.loading())
        
        UserM.Reload()
    
    purge: ->
      @log 'List toolbar.purge'       
    
    Next: -> 
      @trigger 'next'         
    
    Prev: -> 
      @trigger 'prev'

    getSelectedUsers: -> @selMgr.getSelection()     

    class @Tags extends Spine.Controller

      @elements:
        '.labels-list'  : 'list'
        'input'         : 'search'

      @events:
        'click .new-label'      : 'NewLabel'
        'click .manage-labels'  : 'MngLabels'
        'click .apply-labels'   : 'ApplyUserLabels'
        'click input'           : (e) -> e.stopPropagation()
        'input input'           : 'Refresh'

      @form: new LabelForm

      @labelTmpl: require('views/admin/user/label')

      class @Tag extends Spine.Controller

        @events:
          'click'  : 'ToggleUserLabel'          

        constructor: ->
          super

          @selMgr.on "selectionChanged_#{@label.cid}", @selectionChanged          

        selectionChanged: (isSelected) =>
          @el.toggleClass 'selected', isSelected

        ToggleUserLabel: (e) =>
          e.preventDefault()
          e.stopPropagation()

          @selMgr.toggle(@label)

      constructor: ->
        super

        @selMgr = new SelectionMgr

        @el.on 'show.bs.dropdown', @onShow

        Tags.form.on 'save', @SaveTag

        @selMgr.on 'selectionChanged', =>
          @el.toggleClass 'labels-selected', not app.arraysEqual(@selMgr.getSelection(), @initiallySelectedLabels)

      Refresh: =>
        @labels[lastIndex - 1].release() while lastIndex = @labels.length

        q = app.cleaned @search        

        if q is ''
  
          for label in TagM.all()      
            @list.append @add(new Tags.Tag({label, @selMgr, el: Tags.labelTmpl({name:label.name})})).el          

        else

          for label in TagM.all() when label.name.match(new RegExp("^(.*)(#{q})(.*)$"))
          
            name = label.name.replace(
              new RegExp("^(.*)(#{q})(.*)$"), 
              (name, $1, $2, $3) -> if !!$2 then "#{$1}<cite class='q'>#{$2}</cite>#{$3}" else name
            )

            @list.append @add(new Tags.Tag({label, @selMgr, el: Tags.labelTmpl({name})})).el

        @delay -> @selMgr.setSelection(@initiallySelectedLabels = @getSelection())

      getSelection: =>    
        selectedLabels = []

        for _, labels of @getSelectionMap()
          selectedLabels = [].concat.apply(selectedLabels, labels)

        selectedLabels

      getSelectionMap: =>    
        selectionMap = {}
        selectedUsers = (user.id for user in @mgr.getSelectedUsers())

        for id in selectedUsers
          selectionMap[id] or= [] 
          selectionMap[id].push UserTagM.getUserTags(id)

        selectionMap

      labels: []      

      add: (label) ->
        index = @labels.push(label)

        label.on 'release', =>
          @labels.splice(index - 1, 1)

        label

      onShow: =>
        TagM.Reload().done @Refresh      

      NewLabel: (e) ->
        e.preventDefault()

        @delay -> Tags.form.New()

      MngLabels: (e) ->
        e.preventDefault()

        @delay -> @navigate('/labels')

      ApplyUserLabels: (e) ->
        e.preventDefault()

        ;

      SaveTag: (info) ->
        ;
    
class User.Stack extends Manager.Stack

  logPrefix: '(User.Stack)'

  className: 'spine stack users'

  controllers:
    list    : User.List
    single  : User.Single
    form    : User.Form

  constructor: ->

    @routes = {      
      '/users'                     : => @list.active(User.List.ALL)
      '/labelled/users/:label'     : (params) => @list.active(User.List.LABELLED, params.label)
      '/users-:state'              : (params) => @list.active(params.state)
      '/user/:id'                  : (params) => @single.active(params.id)
    }

    super

module.exports = User