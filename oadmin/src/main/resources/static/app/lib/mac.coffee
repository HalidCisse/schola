

class Mac

  @beforeSend: (xhr, req) ->
    # bodyHash = if (req.type is 'POST' or req.type is 'PUT') and req.data then CryptoJS.enc.Base64.stringify(CryptoJS.SHA1(req.data)) else undefined
    nonce = Mac._genNonce(parseInt(session.issuedTime))
    r = Mac.reqString(nonce, req.type, req.url, document.location.hostname, document.location.port||80)
    mac = Mac.sign(session.secret, r)
    header = Mac.createHeader(session.access_token || session.key, nonce, mac)

    xhr.setRequestHeader('Authorization', header)

  @_genNonce: (issuedAt) ->
    secs = Date.now() - issuedAt
    rnd = CryptoJS.lib.WordArray.random(128/32)
    "#{secs}:#{rnd}" 

  @sign: (secret, request) ->
    CryptoJS.enc.Base64.stringify(CryptoJS.HmacSHA1(request, secret))

  @reqString: (nonce, method, uri, hostname, port, bodyHash, ext) ->
    [nonce, method, uri, hostname, port, bodyHash||"", ext||""].join("\n") + "\n"

  @createHeader: (id, nonce, mac, bodyHash) ->
    bodyHash = if bodyHash then ",bodyhash=\"#{bodyHash}\"" else ''
    "MAC id=\"#{id}\",nonce=\"#{nonce}\"#{bodyHash},mac=\"#{mac}\""

exports = Mac