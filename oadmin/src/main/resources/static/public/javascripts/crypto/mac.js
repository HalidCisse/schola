(function() {

  Mac = (function() {
    function Mac() {}

    Mac.beforeSend = function(xhr, req) {
      var header, mac, nonce, r;
      nonce = Mac._genNonce(parseInt(session.issuedTime));
      r = Mac.reqString(nonce, req.type, req.url, document.location.hostname, document.location.port || 80);
      mac = Mac.sign(session.secret, r);
      header = Mac.createHeader(session.access_token || session.key, nonce, mac);
      return xhr.setRequestHeader('Authorization', header);
    };

    Mac._genNonce = function(issuedAt) {
      var rnd, secs;
      secs = Date.now() - issuedAt;
      rnd = CryptoJS.lib.WordArray.random(128 / 32);
      return "" + secs + ":" + rnd;
    };

    Mac.sign = function(secret, request) {
      return CryptoJS.enc.Base64.stringify(CryptoJS.HmacSHA1(request, secret));
    };

    Mac.reqString = function(nonce, method, uri, hostname, port, bodyHash, ext) {
      return [nonce, method, uri, hostname, port, bodyHash || "", ext || ""].join("\n") + "\n";
    };

    Mac.createHeader = function(id, nonce, mac, bodyHash) {
      bodyHash = bodyHash ? ",bodyhash=\"" + bodyHash + "\"" : '';
      return "MAC id=\"" + id + "\",nonce=\"" + nonce + "\"" + bodyHash + ",mac=\"" + mac + "\"";
    };

    return Mac;

  })();

}).call(this);