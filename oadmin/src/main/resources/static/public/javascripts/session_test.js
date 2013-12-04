
var validBody = {
  grant_type: 'password',
  client_id: 'oadmin',
  client_secret: 'oadmin',
  username: 'root',
  password: 'amsayk'
};


var Mac = {

  _genNonce: function(issuedAt) {
      var secs = (new Date().getTime()) - issuedAt;
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
}

var xhr = $.post('/oauth/token', validBody, function(resp) { console.log(JSON.stringify(resp)); window.session = resp; }, 'json')

$.ajax({
  type:"GET",
  beforeSend: function (xhr, req){

    var nonce = Mac._genNonce(parseInt(session["issued_time"]));
    var req = Mac.reqString(nonce, req.type.toUpperCase(), req.url, document.location.hostname, document.location.port||80);
    var mac = Mac.sign(session['secret'], req);
    var header = Mac.createHeader(session['access_token'], nonce, mac);

    xhr.setRequestHeader("Authorization", header);
  },
  url: "/api/session",
  // data: validBody,
  // dataType: 'json',
  success: function(msg) {
    console.log(msg)
 
    $.ajax({
      type:"GET",
      beforeSend: function (xhr, req){

        var nonce = Mac._genNonce(parseInt(session["issued_time"]));
        var req = Mac.reqString(nonce, req.type.toUpperCase(), req.url, document.location.hostname, document.location.port||80);
        var mac = Mac.sign(session['secret'], req);
        var header = Mac.createHeader(session['access_token'], nonce, mac);

        xhr.setRequestHeader("Authorization", header);
      },
      url: "/api/logout",
      success: function(msg) {
          console.log(msg)
      }
    });
  }
});

