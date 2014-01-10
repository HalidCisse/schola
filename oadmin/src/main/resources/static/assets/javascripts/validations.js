
function validEmail(s) {
  if(validateEmpty(s))
    return false;

  var input = document.createElement('input');

  input.type = 'email';
  input.value = s.trim();

  return ! input.checkValidity();
}

function validEmpty(s) {
  return !(s == null && s.trim() === '');
}

function showError(m, msg) {
  var $el = $('#msg');

  m.one('keypress', function() {hideError($(this))})
   .parent().addClass('has-error');

  $el.addClass('alert-danger').html(msg);

  return false;
}

function hideError(m) {
  var $el = $('#msg');

  m.parent().removeClass('has-error');

  $el.removeClass('alert-danger').html('');

  return false;
}