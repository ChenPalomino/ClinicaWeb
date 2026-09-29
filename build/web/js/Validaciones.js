document.addEventListener("DOMContentLoaded", function () {
    const celularInput = document.getElementById('celular');
    const passwordInput = document.getElementById('password');

    const errorCelular = document.getElementById('errorCelular');
    const errorPassword = document.getElementById('errorPassword');

    if (celularInput) {
        celularInput.addEventListener('input', function () {
            const regexCelular = /^9\d{8}$/;
            if (!regexCelular.test(celularInput.value) && celularInput.value.length > 0) {
                errorCelular.style.display = 'block';
                celularInput.style.borderColor = '#b91c1c';
            } else {
                errorCelular.style.display = 'none';
                celularInput.style.borderColor = '#15803d';
            }
        });
    }

    if (passwordInput) {
        passwordInput.addEventListener('input', function () {
            const regexPassword = /^(?=.*[A-Z])(?=.*\d).{8,}$/;
            if (!regexPassword.test(passwordInput.value) && passwordInput.value.length > 0) {
                errorPassword.style.display = 'block';
                passwordInput.style.borderColor = '#b91c1c';
            } else {
                errorPassword.style.display = 'none';
                passwordInput.style.borderColor = '#15803d';
            }
        });
    }
});