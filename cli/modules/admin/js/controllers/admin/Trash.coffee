TrashM = require('js/models/admin/DeletedUser')
Menu   = require('js/controllers/Menu')

SelectionMgr = require('js/lib/selection')

position = require('js/lib/position')

class Trash

class Trash.Single extends Spine.Controller

  constructor: ->
    super

# For viewing all users in Trash
class Trash.List extends Spine.Controller

  logPrefix: '(Trash.List)'

  className: 'users-trash empty'

  calcHeight: -> $(window).height() - @$('.selections').outerHeight() - @toolbar.el.outerHeight() - $('.navbar').outerHeight() - 8
  
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
      for user in TrashM.all()
        if not @selMgr.isSelected(user)
          return no
      yes                

    @selMgr.on 'toggle-selection', @ToggleSelection

    @toolbar     = new List.Toolbar({@selMgr, el: List.toolbarTmpl, mgr:@})  
    @list        = new List.Body(selMgr:@selMgr, mgr: @)
    @contextmenu = new List.ContextMenu(el: List.contextmenuTmpl)        

    @render()    

    @active (@state, label) -> 
      @log("active State<#{@state}>")
      
      @delay -> 

        app.modules['admin'].menu(app.modules['admin'].TRASH, (item) -> item.loading())

        TrashM.Reload()

        @delay -> @list.calcHeight()

    @toolbar.on 'next', @Next
    @toolbar.on 'prev', @Prev
    @toolbar.on 'purge', => @Purge(@selMgr.getSelection())
    
    @on 'purge', (user) => @Purge [user]

    TrashM.on 'refresh', => @Reload(); app.modules['admin'].menu(app.modules['admin'].TRASH, (item) -> item.doneLoading())

    @list.on 'empty', =>
      @log 'Empty trash'
    
    @contextmenu.on 'purge', (user) => @Purge([user])

  @tmpl: require('js/views/admin/trash/list')({perPage: TrashM.MaxResults})
  @rowTmpl: require('js/views/admin/trash/row')()
  @toolbarTmpl: require('js/views/admin/trash/list.toolbar')()
  @contextmenuTmpl: require('js/views/admin/trash/contextmenu')()

  start: 0

  Reload: =>
    @Refresh TrashM.slice(@start, @start + TrashM.MaxResults)

  Next: =>
    @start += TrashM.MaxResults
    @Reload()

  Prev: =>
    @start -= TrashM.MaxResults if @start > 0
    @Reload()

  Purge: (users) ->
    @log "Purge [#{users}]"
    ;

  rows: []

  add: (row) ->
    index = @rows.push(row)

    row.on 'release', =>
      @rows.splice(index - 1, 1)

    row.on 'contextmenu', (evt, user) =>
      @contextmenu.Show(evt, user)

    row.on 'purge', (user) =>
      @Purge [user]
    
    row

  @_indexof = (rec, all=TrashM.all()) -> # Necessary access
    for r, i in all when rec.eql(r)
      return i
    -1    

  AppendOne: (user) ->
    @AppendMany [user]

  AppendMany: (users) ->
    if users.length

      @el.removeClass('empty')

      all = TrashM.all()

      @toolbar.page List._indexof(users[0], all), List._indexof(users[users.length - 1], all), all.length

      for user in users
        @list.addUser @add(delegate = new List.Row({user, @selMgr, el: List.rowTmpl})).el
        delegate.DelegateEvents()

    else

      @el.addClass('empty')
      @toolbar.page -1, -1, 0      

  Refresh: (users) ->
    @rows[lastIndex - 1].release() while lastIndex = @rows.length     

    @delay -> @AppendMany(users)

  ToggleSelectionAll: (selectionState) =>

    switch selectionState
      
      when List.SELECT_ALL

        @selMgr.selected(user) for user in TrashM.all()

      when List.SELECT_NONE

        @selMgr.removeAll()
        @toolbar.allSelected = no
      
      when List.SELECT_ACTIVE

        for user in TrashM.all()
          if not user.suspended
            @selMgr.selected(user)
          else
            @selMgr.removed(user)
      
      when List.SELECT_SUSPENDED 

        for user in TrashM.all()
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
      'click .empty'        : 'empty'   
      'click .clear'        : 'clearAll'   
      'click .select-all'   : 'selectAll'

    calcHeight: -> 
      @scrollable.css({
        height: @mgr.calcHeight()
        })
    
    constructor: ({selMgr: @selMgr}) ->
      super

      @selMgr.on 'selectionChanged', @selectionChanged
      @on 'page-selected', @selectionChanged

      @on 'clear-all', =>
        @mgr.ToggleSelectionAll(List.SELECT_NONE) 
        @delay(-> @calcHeight())

      @on 'select-all', => 
        @mgr.ToggleSelectionAll(List.SELECT_ALL) 
        @delay(-> @calcHeight())      

      @on 'select', (@state) =>
        @mgr.ToggleSelection(state) 
        @delay(-> @calcHeight())      

      @render()

      @$(window).resize(@calcHeight)

    selectionChanged: =>
      @el.toggleClass 'page-selected', TrashM.count() > TrashM.MaxResults and @selMgr.isWholePageSelected()
      @el.toggleClass 'all-selected', TrashM.count() > TrashM.MaxResults and @selMgr.isAllSelected()
      @calcHeight()

    Off: (fn) ->
      @selMgr.off 'selectionChanged', @selectionChanged
      do fn
      @selMgr.on 'selectionChanged', @selectionChanged

    render: ->
      @html List.tmpl

    addUser: (user) ->
      @list.append user

    empty: (e) ->
      e.stopPropagation()
      e.preventDefault()

      @trigger 'empty'   

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

    logPrefix: '(Trash.ContextMenu)'

    @events:
      'click .purge'    : 'purge'  
      'click .restore'  : 'restore'    

    constructor: ->
      super

      @$(document).click(=> @el.hide())

    Show: (evt, @user) ->
      position.positionPopupAtPoint(evt.clientX, evt.clientY, @el[0])
      @el.show()
      
    purge: (e) ->
      @log("purge User<#{@user.id}>")

      e.preventDefault()
      
      @trigger 'purge', @user

    restore: (e) ->
      @log("restore User<#{@user.id}>")

      e.preventDefault()
      e.stopPropagation()
      
      @trigger 'restore', @user 

  # For viewing a user in the table
  class @Row extends Spine.Controller

    @Events:
      'click'           : 'clicked'   
      'click .purge'    : 'purge'   
      'click .select'   : 'select'
      'contextmenu'     : 'contextmenu'   

    constructor: ->
      super

      @selMgr.on "selectionChanged_#{@user.cid}", @selectionChanged
      @listenTo @user, 'change', @FillData

      @bind 'release', -> 
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

      @delay -> @navigate('/trash/user', @user.id)

    purge: (evt) ->
      evt.preventDefault()
      evt.stopPropagation()

      @trigger 'purge', @user.id     

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
      '.next'                       : 'next'
      '.prev'                       : 'prev'
      '.select-toggle-wrapper'      : 'selectToggle'

    @Events:
      'click .select'        : 'ToggleSelection'
      'click .restore'       : 'restore'
      'click .refresh'       : 'refresh'
      'click .purge'         : 'purge'    
      'click .next'          : 'Next'    
      'click .prev'          : 'Prev'

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

      @bind 'release', ->
        @selMgr.off 'selectionChanged', @selectionChanged

      @delegateEvents(Toolbar.Events)

      (new Toolbar.SelectMenu(el: @$('.selection-menu'))).on 'select', (state) => @mgr.list.trigger 'select', state

      @selectToggle.dropdown()
                   .closest('#users-trash-select').on 'show.bs.dropdown', => 
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
      if total > TrashM.MaxResults # show paging only if necessary
        
        @next.prop('disabled', end is total - 1)
        @prev.prop('disabled', start is 0)

        @$('.start').html(start + 1)
        @$('.end').html("#{end + 1}&nbsp;")
        @$('.count').html("#{numeral(total).format()} &nbsp;")

      @el.toggleClass 'needs-paging', total > TrashM.MaxResults
      @el.toggleClass 'empty', total is 0
    
    refresh: ->
      return if @selMgr.hasSelection()

      @delay ->
        app.modules['admin'].menu(app.modules['admin'].TRASH, (item) -> item.loading())
        TrashM.Reload()
    
    purge: ->
      @log 'List toolbar.purge'       

    restore: ->
      @log 'List toolbar.restore'             
    
    Next: -> 
      @trigger 'next'         
    
    Prev: -> 
      @trigger 'prev'

    getSelectedUsers: -> @selMgr.getSelection()
    
class Trash.Stack extends Spine.Stack

  logPrefix: '(Trash.Stack)'

  className: 'spine stack users-trash'

  controllers:
    list    : Trash.List
    single  : Trash.Single

  constructor: ->

    @routes = {      
      '/trash/users'     : 'list'
      '/trash/user/:id'  : (params) => @single.active(params.id)
    }

    super

module.exports = Trash