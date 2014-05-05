TagM = require('js/models/Tag')

LabelForm = require('js/controllers/LabelForm')

class Tag

class Tag.Mgr extends Spine.Controller

  logPrefix: '(Tag.Mgr)'

  className: 'labels'

  @elements:
    '.tags-list'       : 'tagList'
    '.filter input'   : 'filterInput'    

  @events:
    'click .new'      : 'New'
    'input .filter'   : 'Filter'

  constructor: ->
    super

    @render()

    @active ->
      @log("active")

      @delay =>
        TagM.Reload()

    TagM.on 'refresh', @Refresh

    @on 'edit', @Edit
    @on 'del', @Del

    Mgr.form.on 'save', @saveTag
    Mgr.form.on 'update', @updateTag    

  tags: []

  Refresh: =>
    @tags[lastIndex - 1].release() while lastIndex = @tags.length

    for item in TagM.all()

      @tagList.append @add(new Mgr.Row({item, el: Mgr.rowTmpl, q: @filterInput.val(), mgr: @})).el

  add: (tag) ->
    index = @tags.push(tag)

    tag.on 'release', =>
      @tags.splice(index - 1, 1)

    tag    

  Edit: (item) ->
    Mgr.form.Edit(item)

  Del: (item) ->
    @log "Del #{item}"
    ;

  saveTag: (info) ->
    @log "saveTag #{JSON.stringify(info)}"
    ;

  updateTag: (name, info) ->
    @log "updateTag #{name} : #{JSON.stringify(info)}"
    ;

  render: ->
    @html Mgr.tmpl    

  @tmpl: require('js/views/label/tags')()  
  @rowTmpl: require('js/views/label/single')()

  New: (e) ->
    e.preventDefault()
    e.stopPropagation()

    Mgr.form.New()

  Filter: ->
    q = @filterInput.val()
    tag.Filtered(q) for tag in @tags

  class @Row extends Spine.Controller

    @events:
      'click .edit'     : 'Edit'
      'click .del'      : 'Del'

    constructor: ->
      super

      @item.on 'change', @FillData

      @FillData()

    Filtered: (@q) ->
      found = no

      @$('.color').css('background-color', @item.color)

      @$('.name').html(
        @item.name.replace(
          new RegExp("^(.*)(#{@q})(.*)$"), 
          (name, $1, $2, $3) ->
            if !!$2
              found = yes
              "#{$1}<cite class='q'>#{$2}</cite>#{$3}" 
            else name
        )
      )

      found    

    FillData: ->

      if @q isnt undefined

        @Filtered(@q)          

      else
        @$('.color').css('background-color', @item.color)
        @$('.name').html(@item.name)

      @el      

    Edit: (e) ->
      e.preventDefault()
      e.stopPropagation()

      @mgr.trigger('edit', @item)

    Del: (e) ->
      e.preventDefault()
      e.stopPropagation()

      @mgr.trigger('del', @item)

  @form: new LabelForm  

class Tag.Stack extends Spine.Stack

  logPrefix: '(Tag.Stack)'

  className: 'spine stack labels'

  controllers:
    mgr  : Tag.Mgr

  constructor: ->

    @routes = {      
      '/labels'  : 'mgr'
    }

    super  

module.exports = Tag