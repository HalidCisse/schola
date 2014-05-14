
class UserTag extends Spine.Model

  @configure 'UserTag', 'label', 'userId'

  @Reload: ->

    $.Deferred (deferred) =>

      d = $.Deferred()

      d.done (userTags) => 
        @refresh(userTags, clear: yes)
        deferred.resolve @all()  

      app.labels.getLabels()
         .done d.resolve

  @getUserTags: (userId) ->
    @select (userTag) => userTag.userId is userId
  
module.exports = UserTag