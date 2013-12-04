
window.Mac || (var Mac = (function(){
  return {
    _genNonce: function(issuedAt) {
      var secs = Date.now() - issuedAt;
      var rnd = CryptoJS.lib.WordArray.random(128/32)
      return secs + ":" + rnd;
    },

    sign: function(key, request) {
      return CryptoJS.enc.Base64.stringify(CryptoJS.HmacSHA1(request, key));
    },

    reqString: function(nonce, method, uri, hostname, port, bodyhash, ext) {
      return [nonce, method.toUpperCase(), uri, hostname, port, bodyhash||"", ext||""].join("\n") + "\n";
    },

    createHeader: function(id, nonce, mac) {
     return 'MAC id="' + id + '",nonce="' + nonce + '",mac="' + mac + '"';
    }
  };
})())