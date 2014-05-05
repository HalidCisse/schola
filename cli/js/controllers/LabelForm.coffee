
class LabelForm extends Spine.Controller

  @elements:
    '[name=name]'     : 'name'
    '[name=color]'    : 'color'

  @events:
    'submit form' : 'save'

  constructor: ->
    @el = require('js/views/label/form')()

    super

    @$('.color-cooser-color').click (evt) =>
      evt.preventDefault()
      evt.stopPropagation()

      @color.val @$(evt.target).data('hex-color')

  New: ->

    @label = undefined

    @$('.modal-title').html('New Label')
    @name.val ''
    @color.val '#fff'

    @el.modal()
    @delay -> @name.focus()       

  Edit: (@label) ->

    @$('.modal-title').html('Edit Label')

    @name.val @label.name
    @color.val @label.color

    @el.modal()

    @delay -> @name.focus()

  save: (e) ->
    e.preventDefault()

    @el.modal('hide')

    if @label
      @trigger 'update', @label.name, @getInfo()
    else
      @trigger 'save', @getInfo()

  # private

  getInfo: ->
    name: app.cleaned @name
    color: app.cleaned @color

module.exports = LabelForm