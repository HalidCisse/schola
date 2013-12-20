Spine  = require('spine')
$      = require('jquerify')
Mac    = require('lib/mac')

class Session extends Spine.Module
  @extend Spine.Events
  @include Spine.Log

  isLoggedIn: ->
    @key or false

  beforeSend: (xhr, req) =>
    xhr.setRequestHeader("Authorization", @getMacHeader(xhr, req))

  contructor: (@app) ->
    super

  changePasswd: ->
    if not @user.passwordValid
      @app.delay (=> @app.navigate('/')), 0
      return true

    false


  getLoginStatus: ->

    checkSession = =>
      x = $.ajax(
        type:"GET",
        url: "/api/v1/session",
        dataType: 'json'
      )

      x.success (session) ->

        $.extend(true, @, session)

        if(@changePasswd())
          return true

        #
        @trigger 'session.loggedin'

        false

      x.error ->

        # Clear session data
        delete @key
        delete @secret
        delete @clientId
        delete @issuedTime
        delete @user
        delete @lastAccessTime
        @expiresIn and delete @expiresIn
        @refreshExpiresIn and delete @refreshExpiresIn
        @refresh and delete @refresh
        delete @userAgent
        delete @roles
        delete @permissions
        delete @scopes

        clearInterval(@_interval)
        @trigger 'session.loggedout'

        false

    @_interval = setInterval(checkSession, 15 * 1000)

  getMacHeader: (xhr, req) ->
    nonce = Mac._genNonce(parseInt(@issuedTime))
    req = Mac.reqString(nonce, req.type.toUpperCase(), req.url, document.location.hostname, document.location.port||80)
    mac = Mac.sign(@secret, req)
    Mac.createHeader(@key, nonce, mac)    

  # login: (email, passwd) ->
  #   p = $.post(
  #     '/oauth/token', 
  #     {username: email, password: passwd, grantType: 'password', client_id: 'oadmin', client_secret: 'oadmin'}, 
  #     'json'
  #   )
    
  #   p.success (session) => 
  #     $.extend(true, @, session)
      
  #     $.ajaxSetup(beforeSend: @beforeSend)

  #     if(@changePasswd())
  #         return true

  #     @trigger 'session.loggedin'      

  #     @getLoginStatus()

  #     false

  #   p.error ->
  #     @trigger 'session.loggedout'
  #     false

  #   p

  logout: =>
    $.ajax(
      type: "GET",
      url: "/api/v1/logout",
      dataType: 'json'
    ).done => 

        # Clear session data
        delete @key
        delete @secret
        delete @clientId
        delete @issuedTime
        delete @user
        delete @lastAccessTime
        @expiresIn and delete @expiresIn
        @refreshExpiresIn and delete @refreshExpiresIn
        @refresh and delete @refresh
        delete @userAgent
        delete @roles
        delete @permissions
        delete @scopes        

        @_interval and clearInterval(@_interval)

  hasRole: (role) ->
    @roles[role] or false

  hasPermission: (permission) ->
    @permissions[permission] or false

exports = Session