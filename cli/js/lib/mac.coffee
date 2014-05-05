
class Mac

  @_genNonce: (issuedAt) ->
    "#{Date.now() - issuedAt}:#{Crypto.util.bytesToHex(Crypto.util.randomBytes(128/32))}" 

  @sign: (secret, rs) ->
    Crypto.util.bytesToBase64(Crypto.HMAC(Crypto.SHA1, rs, secret, { asBytes: yes }))

  @toReqString: (nonce, method, uri, hostname, port, bodyHash, ext) ->
    [nonce, method, uri, hostname, port, bodyHash or "", ext or ""].join("\n") + "\n"

  @createHeader: (session, xhr, req) ->
    nonce = Mac._genNonce(parseInt(session.issuedTime))
    rs    = Mac.toReqString(nonce, req.type, req.url, app.hostname or 'localhost', app.port or 80)
    mac   = Mac.sign(session.secret, rs)

    "MAC id=\"#{session.access_token or session.key}\",nonce=\"#{nonce}\",mac=\"#{mac}\""

module.exports = Mac