
R = jsRoutes.controllers.Tags

class labels

  @getLabels: ->
    $.getJSON R.getTags().url

  @addLabel: (label, color) ->
    route = R.addTag(label, color)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @updateLabelColor: (name, color) ->
    route = R.updateTagColor(name, color)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @updateLabel: (name, newName) ->
    route = R.updateTag(name, newName)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )    

  @purgeLabels: (labels) ->
    route = R.purgeTags(labels)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

module.exports = labels