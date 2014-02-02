Spine = require('spine')

class Menu extends Spine.Controller

  @USERS: 'users'
  @TRASH: 'trash'
  @ROLES: 'roles'
  @STATS: 'stats'

  className: 'menu'

  @elements:
    '.menuHeader'    : 'menuHeader'
    '.menuItems'     : 'menuItems'

  constructor: ->
    super

    @mgmt = new Menu.Mgmt

  @tmpl: require('views/menu/single')

  render: ->
    @html Menu.tmpl()

    @el.attr 'id', "menu-#{@id}"

    @menuHeader.html(@header)
    @menuItems.append @add(new Menu.Item({item})).render() for item in @items

    @el

  getItem: (id) ->
    for _ in @mgmt.items
      return _  if _.item.id is id

  add: (item) ->
    @mgmt.add(item)
    item

  class @Mgmt extends Spine.Module
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

    deactivate: ->
      @trigger('change', false, arguments...)

    # Private

    change: (current, args...) ->
      for item in @items when item isnt current
        item.deactivate(args...)

      current.activate(args...) if current    

  class @Item extends Spine.Controller

    tag: 'li'

    className: 'menuItem'

    @events:
      'click' : 'clicked'

    constructor: ->
      super      

    loading: ->
      @el.addClass('loading')
      @

    doneLoading: ->
      @el.removeClass('loading')
      @      

    activate: ->
      @el.addClass('active')
      @

    deactivate: ->
      @doneLoading()
      @el.removeClass('active')
      @

    clicked: (e) ->
      @delay -> @trigger('active')

      e.stopPropagation()
      e.preventDefault()

      @navigate(@item.href)

    @tmpl: require('views/menu/item')

    render: ->
      @html Item.tmpl()

      @$('a').attr('href', @item.href)
      @$('.glyphicon').addClass(@item.icon)
      @$('.title').html(@item.title)

      @el

class Menu.Mgr

  @el: new Spine.Controller(el: '#sidebar')

  @menus: {}

  @add: (opts) ->
    @rm opts.id

    @el.append (@menus[opts.id] = new Menu(opts)).render()
  
  @rm: (id) ->

    if @menus[id]
      @menus[id].release()

      delete @menus[id]

  @getMenu: (id) ->
    for _, menu of @menus
      item = menu.getItem(id)
      return item if item

module.exports = Menu