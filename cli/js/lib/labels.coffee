$ = require('jqueryify')

class labels

  @getLabels: ->
    $.getJSON "/api/v1/labels"

  @addLabel: (label, color) ->
    $.ajax(
      type: 'POST',
      url: "/api/v1/labels",
      dataType: 'json',
      data: {label, color}
    )

  @updateLabelColor: (name, color) ->
    $.ajax(
      type: 'PUT',
      url: "/api/v1/label/#{name}/color",
      dataType: 'json',
      data: {color: color}
    )

  @updateLabelname: (name, newName) ->
    $.ajax(
      type: 'PUT',
      url: "/api/v1/label/#{name}",
      dataType: 'json',
      data: {label: newName}
    )    

  @purgeLabels: (labels) ->
    $.ajax(
      type: 'DELETE',
      url: "/api/v1/labels?" + $.param({labels}, true),
      dataType: 'json'
    )

  @labelUser: (userId, labels) ->
    $.ajax(
      type: 'PUT',
      url: "/api/v1/user/#{userId}/labels",
      dataType: 'json',
      traditional: true,
      data: {labels}
    )

  @unLabelUser: (userId, labels) ->
    $.ajax(
      type: 'DELETE',
      url: "/api/v1/user/#{userId}/labels?" + $.param({labels}, true),
      dataType: 'json'    
    )

  @getUserLabels: (userId) ->
    $.getJSON "/api/v1/user/#{userId}/labels"

module.exports = labels