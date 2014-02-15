$ = require('jqueryify')

R = jsRoutes.controllers

class labels

  @getLabels: ->
    $.getJSON R.Tags.getTags().url

  @addLabel: (label, color) ->
    route = R.Roles.addTag(label, color)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @updateLabelColor: (name, color) ->
    route = R.Roles.updateTagColor(name, color)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

  @updateLabel: (name, newName) ->
    route = R.Roles.updateTag(name, newName)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )    

  @purgeLabels: (labels) ->
    route = R.Roles.purgeTags(labels)
    $.ajax(
      type: route.type
      url: route.url
      dataType: 'json'
    )

module.exports = labels