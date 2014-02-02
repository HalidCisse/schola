Spine   = require('spine')
Manager = require('spine/lib/manager')

RoleM = require('models/Role')
Menu  = require('controllers/Menu')

SelectionMgr = require('lib/selection')

position = require('lib/position')

class Role

class Role.Tree extends Spine.Controller

  logPrefix: '(Role.Tree)'

  className: 'roles'

  constructor: ->
    super

    @selMgr = new SelectionMgr

    @toolbar = new Tree.Toolbar({@selMgr, el: Tree.toolbarTmpl})  
    @tree    = new Spine.Controller(className: 'tree')    

    @render()

    @active -> 
      @log('active')
      
      @delay -> 
        app.menu(Menu.ROLES).loading()
        RoleM.Reload()

    RoleM.on 'refresh', => @Refresh(); app.menu(Menu.ROLES).doneLoading() 

    @toolbar.on 'filter', (q) =>

      for row in @topRows
        row.Filtered(q)
        
        row.FilterChildren()

      @delay(->
        $q = @$('.q:first')
        if $q.length then $q.scrollIntoView()
        else document.body.scrollIntoView(true)
      )

    @toolbar.on 'purge', => @Purge @selMgr.getSelection()
    @toolbar.on 'new', -> @delay -> Tree.form.New()

    Tree.contextmenu.on 'purge', (role) => @Purge [role]
    Tree.contextmenu.on 'edit', (role) -> @delay -> Tree.form.Edit(role)

    Tree.form.on 'save', @saveRole
    Tree.form.on 'update', @updateRole

  @rowTmpl: require('views/role/row')()
  @formTmpl: require('views/role/form')()
  @toolbarTmpl: require('views/role/tree.toolbar')()
  @contextmenuTmpl: require('views/role/contextmenu')()  

  Purge: (roles) ->
    ;  

  saveRole: (info) ->
    @log "save role info<#{JSON.stringify(info)}>"
  
  updateRole: (name, info) ->
    @log "update role name<#{name}>, info<#{JSON.stringify(info)}>"

  topRows: []

  add: (row) ->
    i = @topRows.push(row)

    row.release =>
      
      ch.release() for ch in row.childRows # Only top level rows are removed

      delete @topRows[i - 1] if @topRows[i - 1]

    row.on 'contextmenu', (evt, role) =>
      Tree.contextmenu.Show(evt, role)
    
    row

  Refresh: ->
    row.release() for row in @topRows

    for role in RoleM.getTopLevel()

      @tree.append @add(delegate = new Tree.Row({role, @selMgr, el: Tree.rowTmpl, q: @toolbar.getInput()})).el

      delegate.AppendChildren()

      delegate.DelegateEvents()
      
  render: ->
    @append @toolbar.el
    @append @tree.el  
    @append Tree.contextmenu.el
    @append Tree.form.el

  class @ContextMenu extends Spine.Controller

    logPrefix: '(Role.ContextMenu)'

    @events:
      'click .edit'    : 'edit'
      'click .purge'   : 'purge'    

    constructor: ->
      super

      @$(document).click(=> @el.hide())

    Show: (evt, @role) ->
      position.positionPopupAtPoint(evt.clientX, evt.clientY, @el[0]);
      @el.show()
      
    edit: (e) ->
      @log("edit Role<#{@role.name}>")

      e.preventDefault()

      @trigger 'edit', @role

    purge: (e) ->
      @log("purge Role<#{@role.name}>")

      e.preventDefault()
      
      @trigger 'purge', @role

  @contextmenu: new Tree.ContextMenu(el: Tree.contextmenuTmpl)

  class @Form extends Spine.Controller

    @elements:
      '[name=name]'     : 'name'
      '[name=parent]'   : 'parent'

    @events:
      'click .ok' : 'save'

    constructor: ->
      super

      @el.on 'hidden.bs.modal', => @role = undefined

    New: ->

      @load -> 
        @el.modal()
        @delay -> @name.focus()       

    Edit: (@role) ->

      @load ->

        @name.val @role.name
        @parent.val @role.parent if @role.parent

        @el.modal()

        @delay -> @name.focus()

    save: ->
      if @role
        @trigger 'update', @role.name, @getInfo()
      else
        @trigger 'save', @getInfo()

    # private

    getInfo: ->
      name: app.cleaned @name.val()
      parent: if (_parent = app.cleaned @parent.val()) isnt ':noparent' then _parent else undefined

    load: (callback) ->

      GetOthers = ->
        RoleM.select (other) =>
          not other.eql @role

      CreateOptions = (roles) ->

        childrenOf = (p1) -> r3 for r3 in roles when r3.parent is p1.name

        getLevel = (rrr) ->
          return 0 unless rrr.parent
          1 + getLevel(RoleM.findByAttribute('name', rrr.parent))

        labelOf = (rr) -> 
          "#{('&nbsp;&nbsp;' for _ in [0..getLevel(rr)] by 1).join('')}#{rr.name}"

        options = (roles0) ->
          (for r0 in roles0
            "<option value='#{r0.name}'>#{labelOf r0}</option>\n" + options(childrenOf r0)).join ''
          
        '<option value=":noparent">-- Choose a role --</option>\n' + options(r1 for r1 in roles when not r1.parent)

      if @role
        @$('.modal-title').html('Edit Role')
        @$('[for=name]').html('Please edit role name:')
      else
        @$('.modal-title').html('New Role')
        @$('[for=name]').html('Please enter a new role name:')

      @parent.html(CreateOptions(if @role then GetOthers() else RoleM.all()))
             .chosen(allow_single_deselect:yes, disable_search_threshold:10, no_results_text:'Oops, nothing found!', width:"100%")

      @delay -> callback.call(@)

  @form: new Tree.Form(el: Tree.formTmpl)

  class @Toolbar extends Spine.Controller

    @elements:
      '.filter input'   : 'input'

    @Events:
      'click .new'               : 'New'
      'click .refresh'           : 'refresh'
      'click .purge'             : 'purge'    
      'input .filter input'      : 'filter'    

    allSelected: no  

    constructor: ->
      super

      @selMgr.on 'selectionChanged', @selectionChanged

      @release ->
        @selMgr.off 'selectionChanged', @selectionChanged

      @delegateEvents(Toolbar.Events)

    selectionChanged: => 
      @el.toggleClass 'has-selection', @selMgr.hasSelection()
    
    New: ->
      @trigger 'new'

    refresh: ->
      @delay ->
        app.menu(Menu.ROLES).loading()
        RoleM.Reload()
    
    purge: ->
      @trigger 'purge'

    filter: (e) ->
      e.stopPropagation()
      
      @trigger 'filter', @getInput()

    getInput: -> @input.val()

  class @Row extends Spine.Controller

    @elements:
      '.tree-children:eq(0)'   : 'childElements'   
      '.tree-row:eq(0)'        : 'rowElement'   
      '.tree-label:eq(0)'      : 'nameElement'   
    
    constructor: ->
      super

      @selMgr.on "selectionChanged_#{@role.cid}", @selectionChanged
      @listenTo @role, 'change', @FillData

      @release -> 
        @selMgr.off "selectionChanged_#{@role.cid}", @selectionChanged

      @FillData()

    AppendChildren: ->
      hasChildren = no

      for child in RoleM.getChildren(@role)
        
        hasChildren = yes

        @childElements.append @add(delegate = new Tree.Row({role: child, @selMgr, el: Tree.rowTmpl, q: @q, parent: @})).el

        delegate.AppendChildren()

        delegate.DelegateEvents()

      @el.toggleClass 'has-children', hasChildren
      @rowElement.toggleClass 'has-children', hasChildren

    DelegateEvents: ->
      @el.on 'contextmenu', @contextmenu

      @rowElement.on 'click', @select
      @rowElement.on 'click', '.expand-icon', @DoToggle  

    childRows: []

    add: (row) ->
      i = @childRows.push(row)

      row.release =>
        delete @childRows[i - 1] if @childRows[i - 1]

      row.on 'contextmenu', (evt, role) =>
        Tree.contextmenu.Show(evt, role)
      
      row

    DoToggle: (e) =>
      e.stopPropagation()
      e.preventDefault()

      collapse = not @el.hasClass('expanded')
      
      @el.toggleClass 'expanded', collapse
      @childElements.toggleClass 'expanded', collapse      

    Expand: ->
      @log("Expand Role<#{@role.name}> with parent=#{@parent?.role.name}")

      if @parent
        @parent.el.addClass 'expanded'
        @parent.childElements.addClass 'expanded'        
        
        @parent.Expand()

    FillData: ->

      @rowElement.toggleClass 'selected', @selMgr.isSelected(@role)
      
      if @q isnt undefined

        @Expand() if @Filtered(@q)          

      else @nameElement.html(@role.name)

      @el

    Filtered: (@q) ->
      found = no

      @nameElement.html(
        @role.name.replace(
          new RegExp("^(.*)(#{@q})(.*)$"), 
          (name, $1, $2, $3) ->
            if !!$2
              found = yes
              "#{$1}<cite class='q'>#{$2}</cite>#{$3}" 
            else name
        )
      )

      found

    FilterChildren: ->
      for child in @childRows when child.Filtered(@q)
        child.Expand()

    contextmenu: (e) =>
      e.stopPropagation()
      e.preventDefault()

      @trigger 'contextmenu', e, @role

    selectionChanged: (selected) => 
      @rowElement.toggleClass 'selected', selected

    Off: (fn) ->
      @selMgr.off "selectionChanged_#{@role.cid}", @selectionChanged
      do fn
      @selMgr.on "selectionChanged_#{@role.cid}", @selectionChanged  
    
    select: (evt) =>
      evt.stopPropagation()
      evt.preventDefault()

      @Off =>
        @selMgr.selectOnly(@role)

      @rowElement.toggleClass 'selected', @selMgr.isSelected(@role) 

      # Load permissions    

class Role.Stack extends Manager.Stack

  logPrefix: '(Role.Stack)'

  className: 'spine stack roles'

  controllers:
    tree    : Role.Tree

  constructor: (opts) ->

    @routes = {      
      '/roles'  : 'tree'
    }

    super

    @active =>
      @log('active')

    @manager.on 'change', -> app.menu(Menu.ROLES).activate()

module.exports = Role