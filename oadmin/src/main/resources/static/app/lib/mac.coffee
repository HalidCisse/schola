

class Mac

  @_genNonce: (issuedAt) ->
    secs = Date.now() - issuedAt
    rnd = CryptoJS.lib.WordArray.random(128/32)
    "#{secs}:#{rnd}" 

  @sign: (key, request) ->
    CryptoJS.enc.Base64.stringify(CryptoJS.HmacSHA1(request, key))

  @reqString: (nonce, method, uri, hostname, port, bodyhash, ext) ->
    [nonce, method.toUpperCase(), uri, hostname, port, bodyhash||"", ext||""].join("\n") + "\n"

  @createHeader: (id, nonce, mac) ->
   "MAC id=\"#{id}\",nonce=\"#{nonce}\",mac=\"mac\""

exports = Mac