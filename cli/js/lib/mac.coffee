
class Mac

  @_genNonce: (issuedAt) ->
    secs = Date.now() - issuedAt
    rnd = CryptoJS.lib.WordArray.random(128/32)
    "#{secs}:#{rnd}" 

  @sign: (secret, rs) ->
    CryptoJS.enc.Base64.stringify(CryptoJS.HmacSHA1(rs, secret))

  @toReqString: (nonce, method, uri, hostname, port, bodyHash, ext) ->
    [nonce, method, uri, hostname, port, bodyHash or "", ext or ""].join("\n") + "\n"

  @createHeader: (session, xhr, req) ->
    nonce = Mac._genNonce(parseInt(session.issuedTime))
    rs    = Mac.toReqString(nonce, req.type, req.url, app.server_host, app.server_port or 80)
    mac   = Mac.sign(session.secret, rs)

    "MAC id=\"#{session.access_token or session.key}\",nonce=\"#{nonce}\",mac=\"#{mac}\""

module.exports = Mac