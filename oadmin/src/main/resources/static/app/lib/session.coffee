Spine  = require('spine')

class Session extends Spine.Module
  @extend Spine.Events
  @include Spine.Log

  contructor: ->
    super

  @getLoginStatus: () ->

  getMAcHeader: (xhr) ->

  hasRole: (role) ->

  hasPermission: (permission) ->



exports = Session