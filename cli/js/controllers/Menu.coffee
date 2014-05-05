
class Menu extends Spine.Controller

  className: 'menu' 

  @elements:
    '.menuHeader'    : 'menuHeader'
    '.menuItems'     : 'menuItems'

  constructor: ->
    super

    @manager = new Menu.Manager

    @render()

  @tmpl: require('js/views/menu/single')()
  @itemTmpl: require('js/views/menu/item')()

  render: ->
    @html Menu.tmpl

    @el.attr 'id', "menu-#{@id}"
    @el.addClass "menu-#{@id}"

    @menuHeader.html @header
    @menuItems.append @add(new Menu.Item({item, manager: @manager, menu: @, el: Menu.itemTmpl})).render() for item in @items

    @el

  getItem: (id, clbk) ->
    for _ in @manager.items
      if _.item.id is id
        clbk _
        return
      else if _.item.getChildren
        _.item.getChildren().done (children) =>
          for ch in children when ch.id is id
            clbk ch

  add: (item) ->
    @manager.add(item)
    item

  activateItem: (item) ->
    @manager.activate(item)

  class @Manager extends Spine.Module
    @include Spine.Events

    constructor: ->
      @items = []
      @bind 'change', @change
      @add(arguments...)

    add: (items...) ->
      @addOne(item) for item in items

    addOne: (item) ->
      item.bind 'active', (args...) =>
        @trigger('change', item, args...)
      item.bind 'release', =>
        @items.splice(@items.indexOf(item), 1)

      @items.push(item)

    activate: (item) ->
      @trigger('change', item, arguments...)
      item

    # Private

    change: (current, args...) ->
      for item in @items when item isnt current
        item.deactivate(args...)

      current.activate(args...) if current    

  class @Item extends Spine.Controller

    elements:
      '.item'     : 'itemElement'
      '.children' : 'childElements'

    @events:
      'click' : 'clicked'

    constructor: ->
      super

      @el.addClass "menu-item-#{@item.id.replace('.', '_')}"

    loading: ->
      NProgress.start()

      @itemElement.addClass('loading')
      @

    doneLoading: ->
      NProgress.done()
      
      @itemElement.removeClass('loading')
      @      

    activate: ->
      @itemElement.addClass('active')
      @menu.el.addClass(@item.state) if @item.state
      @

    deactivate: ->
      @itemElement.removeClass('active')
      @menu.el.removeClass(@item.state) if @item.state
      @

    clicked: (e) ->
      @delay -> @trigger('active')

      e.stopPropagation()
      e.preventDefault()

      @navigate @item.href

    @linkTmpl: require('js/views/menu/link')

    add: (item) ->
      @manager.add(item)
      item    

    render: ->
      @itemElement.html (@item.render or Menu.Item.linkTmpl)(@item)

      if @item.getChildren
        @item.getChildren().done (children) =>
          for child in children
            @childElements.append @add(new Menu.Item({item: child, manager: @manager, menu: @, el: Menu.itemTmpl})).render()

      @el

  class @Mgr

    menus: -> @_menus

    routes: -> {}

    _menus: {}

    add: (opts) ->
      @rm opts.id

      @_menus[opts.id] = new Menu(opts)
    
    rm: (id) ->

      if @_menus[id]
        @_menus[id].release()

        delete @_menus[id]

    activate: (id, clbk) ->
      @_getMenu(id, (arg) -> 
        {menu, item} = arg
        menu.activateItem(item)
        try
          clbk(item)
        catch _
          ;
      )      

    # private

    _getMenu: (id, clbk) ->
      for _, menu of @_menus
        menu.getItem(id, (item) => clbk {menu, item} if item)              

module.exports = Menu