Spine = require('spine')

class SelectionMgr extends Spine.Module
  @include Spine.Events
  
  _selections: []
  
  constructor: ->
    super

  selectOnly: (item) ->
    @removed(other) for other in @getSelection()
    @selected(item)

  selected: (item) ->
    if @_indexof(item) is -1
      @_selections.push(item)

      @trigger 'selectionChanged'
      @trigger "selectionChanged_#{item.cid}", yes

    false

  count: -> @_selections.length

  removed: (item) ->
    index = @_indexof(item)
    
    if index > -1
      @_selections.splice(index, 1)

      @trigger 'selectionChanged'
      @trigger "selectionChanged_#{item.cid}", no

    false

  isSelected: (item) -> 
    @_indexof(item) > -1
  
  getSelection: -> 
    @_selections
  
  hasSelection: -> 
    @count() > 0

  # private

  _indexof: (rec) ->
    for r, i in @getSelection() when rec.eql(r)
      return i
    -1    

module.exports = SelectionMgr