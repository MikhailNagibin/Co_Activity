import { auth } from './api.js';

document.addEventListener('DOMContentLoaded', () => {
    console.log('Register script loaded'); // 1. Проверка загрузки скрипта

    const registerBtn = document.getElementById('register-btn');
    const errorDiv = document.getElementById('error-message');

    if (!registerBtn) {
        console.error('Register button not found!');
        return;
    }

    console.log('Register button found, attaching event listener');

    registerBtn.addEventListener('click', async (event) => {
        event.preventDefault();
        console.log('Register button clicked'); // 2. Проверка клика

        const email = document.getElementById('email')?.value;
        const username = document.getElementById('username')?.value;
        const password = document.getElementById('password')?.value;
        const confirmPassword = document.getElementById('confirm-password')?.value;
        const birthDate = document.getElementById('birth-date')?.value;
        const country = document.getElementById('country')?.value;
        const city = document.getElementById('city')?.value;
        const description = document.getElementById('about')?.value;

        console.log('Form values:', { email, username, passwordLength: password?.length, birthDate });

        // Валидация
        if (!email || !username || !password || !confirmPassword || !birthDate) {
            errorDiv.textContent = 'Заполните все обязательные поля';
            return;
        }

        if (password !== confirmPassword) {
            errorDiv.textContent = 'Пароли не совпадают';
            return;
        }

        if (password.length < 8) {
            errorDiv.textContent = 'Пароль должен содержать минимум 8 символов';
            return;
        }

        // Преобразуем дату в ISO строку
        const birthDateObj = new Date(birthDate);
        birthDateObj.setUTCHours(0, 0, 0, 0);

        const requestData = {
            login: email,
            userName: username,
            password: password,
            dateOfBirth: birthDateObj.toISOString(),
            country: country || null,
            city: city || null,
            description: description || null,
            avatarId: null
        };

        console.log('Sending registration data:', requestData);

        try {
            const response = await auth.register(requestData);
            console.log('Registration response:', response);

            if (response.userId) {
                alert('Регистрация успешна! Теперь вы можете войти.');
                window.location.href = '/sign-in';
            }
        } catch (error) {
            console.error('Registration error:', error);
            errorDiv.textContent = 'Ошибка регистрации: ' + (error.message || 'Неизвестная ошибка');
        }
    });
});
