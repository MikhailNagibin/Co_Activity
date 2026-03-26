import { auth } from './api.js';

let currentLogin = '';

document.addEventListener('DOMContentLoaded', () => {
    const loginSubmitBtn = document.getElementById('login-submit-btn');
    const verifySubmitBtn = document.getElementById('verify-submit-btn');
    const backToLoginBtn = document.getElementById('back-to-login-btn');

    console.log('Auth.js loaded');

    if (loginSubmitBtn) {
        loginSubmitBtn.addEventListener('click', async () => {
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const errorDiv = document.getElementById('error-message');

            console.log('Login attempt:', { email, passwordLength: password?.length });

            if (!email || !password) {
                errorDiv.textContent = 'Заполните все поля';
                return;
            }

            try {
                await auth.login(email, password);
                currentLogin = email;
                document.getElementById('email-display').textContent = email;
                document.getElementById('login-step1').style.display = 'none';
                document.getElementById('verify-step').style.display = 'block';
                errorDiv.textContent = '';
                console.log('Verification code sent to email');
            } catch (error) {
                console.error('Login error:', error);
                errorDiv.textContent = 'Ошибка входа: ' + error.message;
            }
        });
    } else {
        console.error('Login button not found!');
    }

    if (verifySubmitBtn) {
        verifySubmitBtn.addEventListener('click', async () => {
            const code = document.getElementById('verification-code').value;
            const errorDiv = document.getElementById('error-message');

            console.log('Verify button clicked, code:', code);

            if (!code) {
                errorDiv.textContent = 'Введите код подтверждения';
                return;
            }

            try {
                const response = await auth.verify(currentLogin, code);
                console.log('Verify response:', response);
                if (response && response.token) {
                    console.log('Login successful, redirecting...');
                    window.location.href = '/main';
                } else {
                    console.log('No token in response:', response);
                    errorDiv.textContent = 'Не удалось получить токен';
                }
            } catch (error) {
                console.error('Verification error:', error);
                errorDiv.textContent = 'Неверный код подтверждения';
            }
        });
    }

    if (backToLoginBtn) {
        backToLoginBtn.addEventListener('click', () => {
            document.getElementById('login-step1').style.display = 'block';
            document.getElementById('verify-step').style.display = 'none';
            document.getElementById('verification-code').value = '';
            document.getElementById('error-message').textContent = '';
        });
    }
});
