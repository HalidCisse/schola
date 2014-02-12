
var validBody = {
  grant_type: 'password',
  client_id: 'oadmin',
  client_secret: 'oadmin',
  username: 'root@oadmin.pro',
  password: 'amsayk'
};


$.ajaxSetup({
  beforeSend: Mac.beforeSend
});

function login() {
  return $.post('/oauth/token', validBody, function(resp) { console.log(JSON.stringify(resp)); window.session = resp; }, 'json')
}

function logout() {
    return $.ajax({
      type:"GET",
      // beforeSend: Mac.beforeSend,
      url: "/api/v1/logout",
      dataType: 'json',
      success: function(msg) {
          console.log(JSON.stringify(msg))
      }
    });
}

function getUser(id) {
  return $.ajax({
    type:"GET",
    url: "/api/v1/user/" + id,
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function getSession() {
  return $.ajax({
    type:"GET",
    url: "/api/v1/session",
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function getUsers() {
  return $.ajax({
    type:"GET",
    url: "/api/v1/users",
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}


function addUser(email, password, givenName, familyName, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin) {
  return $.ajax({
    type:"POST",
    url: "/api/v1/users",
    dataType: 'json',
    contentType: 'application/json; charset=UTF-8',
    data: JSON.stringify({
      primaryEmail: email,
      password: password,
      givenName: givenName,
      familyName: familyName,
      gender: gender,
      homeAddress: homeAddress,
      workAddress: workAddress,
      contacts: contacts,
      changePasswordAtNextLogin: changePasswordAtNextLogin
    }),
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function modifyUser(id, data) {
  return $.ajax({
    type:"PUT",
    url: "/api/v1/user/" + id,
    dataType: 'json',
    contentType: 'application/json; charset=UTF-8',
    data: JSON.stringify(data),
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function changePasswd(id, newPasswd, old_password) {
return modifyUser(id, {
      password: newPasswd,
      old_password: old_password
    })
}

function addAddress(id, address) {
return modifyUser(id, address)
}

function removeHomeAddress(id) {
  return $.ajax({
    type:"PUT",
    url: "/api/v1/user/" + id,
    dataType: 'json',
    contentType: 'application/json; charset=UTF-8',
    data: JSON.stringify({homeAddress: {}}),
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function removeWorkAddress(id) {
  return $.ajax({
    type:"PUT",
    url: "/api/v1/user/" + id,
    dataType: 'json',
    contentType: 'application/json; charset=UTF-8',
    data: JSON.stringify({workAddress: {}}),
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function removeUser(id) {
  return $.ajax({
    type:"DELETE",
    url: "/api/v1/user/" + id,
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function purgeUser(id) {
  return $.ajax({
    type:"DELETE",
    url: "/api/v1/user/" + id + '/_purge',
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function addContact(id, contact) {
  return $.ajax({
    type:"PUT",
    url: "/api/v1/user/" + id + '/contacts',
    contentType: 'application/json; charset=UTF-8',
    dataType: 'json',
    data: JSON.stringify({contacts: [contact]}),
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function removeContact(id, contact) {
  return $.ajax({
    type:"DELETE",
    url: "/api/v1/user/" + id + '/contacts',
    contentType: 'application/json; charset=UTF-8',
    dataType: 'json',
    data: JSON.stringify({contacts: [contact]}),
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });
}

function userExists(email) {
  return $.ajax({
    type:"GET",
    url: "/api/v1/userexists",
    dataType: 'json',
    data: {username: email},
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });  
}

function getTrash() {
  return $.ajax({
    type:"GET",
    url: "/api/v1/users/_trash",
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });    
}

function addAvatar(id) {
  // https://developer.mozilla.org/en-US/docs/Web/Guide/Using_FormData_Objects?redirectlocale=en-US&redirectslug=Web%2FAPI%2FFormData%2FUsing_FormData_Objects
  // https://developer.mozilla.org/en-US/docs/Web/Guide/Using_FormData_Objects?redirectlocale=en-US&redirectslug=Web%2FAPI%2FFormData%2FUsing_FormData_Objects
  // https://developer.mozilla.org/en-US/docs/Using_files_from_web_applications

  // https://github.com/lgersman/jquery.orangevolt-ampere/blob/master/src/jquery.upload.js
/*
  $.upload(myForm.action, new FormData(myForm))
   .progress(function(progressEvent, upload) {
    if( progressEvent.lengthComputable) {
        var percent = Math.round( progressEvent.loaded * 100 / progressEvent.total) + '%';
        if(upload)
          console.log( percent + ' uploaded');
        else
          console.log( percent + ' downloaded');
    }
  }).done(function() {
    console.log('Finished upload');                    
  });

  function progressHandlingFunction(e){
    if(e.lengthComputable){
      $('progress').attr({value: e.loaded, max: e.total});
    }
  }

  f = @path[0].files[0]

  reader = new FileReader
  reader.onload = (evt) ->
    params['data'] = evt.target.result.split(',')[1]
    params['data_len'] = f.size
    
    self.delay -> self.fn(params)

  reader.readAsDataURL f */

  var url = '/api/v1/user/' + id + '/avatars'

  var data = new FormData()

  // JavaScript file-like object...
  var oFileBody = '<a id="a"><b id="b">hey!</b></a>'; // the body of the new file...
  var oBlob = new Blob([oFileBody], {type: "text/xml"});

  data.append("f", oBlob);  

  $.upload(url, data, 'json')
   .progress(function(e, upload) {
    if( e.lengthComputable) {
        var percent = Math.round( e.loaded * 100 / e.total) + '%';
        if(upload)
          console.log( percent + ' uploaded');
        else
          console.log( percent + ' downloaded');
    }
  }).done(function(data) {
    console.log('Finished upload: ', data);                    
  });

  function progressHandlingFunction(e){
    if(e.lengthComputable){
      console.log('Value:', e.loaded, '/Max:', e.total)
      // $('progress').attr({value: e.loaded, max: e.total});
    }
  }  
}

function remAvatar(id) {
 return $.ajax({
    type:"DELETE",
    url: '/api/v1/user/' + id + '/avatars',
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });     
}

function getAvatar(userId) {
 return $.ajax({
    type:"GET",
    url: '/api/v1/user/' + userId + '/avatars',
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });     
}

function grantRoles(id, roles) {
 return $.ajax({
    type:"PUT",
    url: '/api/v1/user/' + id + '/roles',
    dataType: 'json',
    data: {roles: roles},
    traditional: true,
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });       
}

function revokeRoles(id, roles) {
 return $.ajax({
    type:"DELETE",
    url: '/api/v1/user/' + id + '/roles?' + $.param({roles: roles}),
    dataType: 'json',
    traditional: true,
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });       
}

function getUserRoles(id) {
 return $.ajax({
    type:"GET",
    url: '/api/v1/user/' + id + '/roles',
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });       
}

function getRoles() {
 return $.ajax({
    type:"GET",
    url: '/api/v1/roles',
    dataType: 'json',
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });       
}

function addRole(name, parent) {
 return $.ajax({
    type:"POST",
    url: '/api/v1/roles',
    dataType: 'json',
    contentType: 'application/json; charset=UTF-8',
    data: JSON.stringify({name: name, parent: parent}),
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });       
}

function updateRole(name, newName, parent) {
 return $.ajax({
    type:"PUT",
    url: '/api/v1/role/' + name,
    dataType: 'json',
    contentType: 'application/json; charset=UTF-8',
    data: JSON.stringify({name: newName, parent: parent}),
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });       
}

function roleExists(name) {
 return $.ajax({
    type:"GET",
    url: '/api/v1/roleexists',
    dataType: 'json',
    data: {name: name},
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });       
}

function grantPermissions(role, permissions) {
 return $.ajax({
    type:"PUT",
    url: '/api/v1/role/permissions',
    dataType: 'json',
    data: {role: role, permissions: permissions},
    traditional: true,
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });       
}

function revokePermissions(role, permissions) {
 return $.ajax({
    type:"DELETE",
    url: '/api/v1/role/permissions?' + $.param({role: role, permissions: permissions}),
    dataType: 'json',
    traditional: true,
    success: function(msg) {
      console.log(JSON.stringify(msg))
    }
  });       
}

function getClientSession() {
  return $.ajax({
    type:"GET",
    url: "/session",
    dataType: 'json',
    beforeSend: function(xhr, req) {},
    success: function(msg) {
      window.session = msg;
      console.log(JSON.stringify(msg))
    }
  });
}

// -------------------------------------


getClientSession().done( function(){

  
  userExists("cisse.amadou.9@gmail.com").success(function(i){
    if(i.success === false)
      addUser("cisse.amadou.9@gmail.com", "amsayk", 'Ousman', 'Cisse', 'Male', {
            city: 'RABAT',
            country: 'Morocco',
            zipCode: 10032,
            addressLine: "Imm. B, Appt. 23, Cite Mabella, Mabella"
          }, {
            city: 'RABAT',
            country: 'Morocco',
            zipCode: 10032,
            addressLine: "Imm. B, Appt. 23, Cite Mabella, Mabella"
          }, {mobiles:{}}, true);    
  })

  userExists("amadou.cisse@epsilon.ma").success(function(i){
    if(i.success === false)
      addUser("amadou.cisse@epsilon.ma", "amsayk", 'Amadou', 'Cisse', 'Male', {
              city: 'RABAT',
              country: 'Morocco',
              zipCode: 10032,
              addressLine: "Imm. B, Appt. 23, Cite Mabella, Mabella"
            }, {
              city: 'RABAT',
              country: 'Morocco',
              zipCode: 10032,
              addressLine: "Imm. B, Appt. 23, Cite Mabella, Mabella"
            }, {mobiles:{}}, true)
  })
})

app.users.userExists("cisse.amadou.9@gmail.com").success(function(i){
  if(i.success === false)
    app.users.upsertUser({primaryEmail: "cisse.amadou.9@gmail.com", password:"amsayk", givenName:'Ousman', familyName:'Cisse', gender:'Male', homeAddress:{
          city: 'RABAT',
          country: 'Morocco',
          zipCode: 10032,
          addressLine: "Imm. B, Appt. 23, Cite Mabella, Mabella"
        }, workAddress:{
          city: 'RABAT',
          country: 'Morocco',
          zipCode: 10032,
          addressLine: "Imm. B, Appt. 23, Cite Mabella, Mabella"
        }, contacts:{mobiles:{}}, changePasswordAtNextLogin:true});    
})

app.users.userExists("amadou.cisse@epsilon.ma").success(function(i){
  if(i.success === false)
    app.users.upsertUser({primaryEmail:"amadou.cisse@epsilon.ma", password:"amsayk", givenName:'Amadou', familyName:'Cisse', gender:'Male', homeAddress:{
            city: 'RABAT',
            country: 'Morocco',
            zipCode: 10032,
            addressLine: "Imm. B, Appt. 23, Cite Mabella, Mabella"
          }, workAddress:{
            city: 'RABAT',
            country: 'Morocco',
            zipCode: 10032,
            addressLine: "Imm. B, Appt. 23, Cite Mabella, Mabella"
          }, contacts:{mobiles:{}}, changePasswordAtNextLogin:true})
})


// addContact(id, {type: 'HomeContactInfo', home: {type: 'PhoneNumber', number: '+231886582873'}})

// removeContact(id, {type: 'HomeContactInfo', home: {type: 'PhoneNumber', number: '+231886582873'}})

// addAddress(id, {homeAddress: {city: 'CASABLANCA', 'country': 'Morocco', zipCode: '10032', addressLine: '5, Rue Jabal Tazaka, Agdal'}})
