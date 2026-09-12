function showPanel(name){
  document.getElementById('panel-login').classList.toggle('active', name==='login');
  document.getElementById('panel-register').classList.toggle('active', name==='register');
  document.getElementById('tab-login').classList.toggle('active', name==='login');
  document.getElementById('tab-register').classList.toggle('active', name==='register');
  document.getElementById('tab-login').setAttribute('aria-selected', name==='login');
  document.getElementById('tab-register').setAttribute('aria-selected', name==='register');
}

function setError(fieldId, on){
  document.getElementById(fieldId).classList.toggle('has-error', on);
}

function handleSubmit(evt, kind){
  let ok = true;

  if(kind === 'login'){
    const email = document.getElementById('login-email').value.trim();
    const pass = document.getElementById('login-password').value;
    const emailOk = /\S+@\S+\.\S+/.test(email);
    setError('login-email-field', !emailOk); if(!emailOk) ok = false;
    setError('login-password-field', pass.length === 0); if(pass.length === 0) ok = false;
  }

  if(kind === 'register'){
    const name = document.getElementById('reg-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const pass = document.getElementById('reg-password').value;
    const confirm = document.getElementById('reg-confirm').value;
    const emailOk = /\S+@\S+\.\S+/.test(email);

    setError('reg-name-field', name.length === 0); if(name.length === 0) ok = false;
    setError('reg-email-field', !emailOk); if(!emailOk) ok = false;
    setError('reg-password-field', pass.length < 8); if(pass.length < 8) ok = false;
    setError('reg-confirm-field', confirm !== pass || confirm.length === 0); if(confirm !== pass || confirm.length === 0) ok = false;
  }

  evt.preventDefault();
  if(ok){
    alert('Formulario válido. Aquí se conectaría con el backend Spring Boot (POST /login o /registro).');
  }
  return false;
}
