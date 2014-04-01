Spine = require('spine')

class Tag extends Spine.Model

  @configure 'Tag', 'name', 'color'

  @Reload: ->

    $.Deferred (deferred) =>

      d = $.Deferred()

      d.done (tags) => 
        @refresh(tags, clear: yes)
        deferred.resolve @all()

      app.labels.getLabels()
         .done d.resolve

module.exports = Tag