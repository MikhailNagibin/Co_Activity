import { qa } from './api.js';

// Загрузка вопросов
async function loadQuestions() {
    const container = document.getElementById('questions-container');
    const searchInput = document.getElementById('search-input');
    const categorySelect = document.getElementById('category-select');

    try {
        let questions = await qa.getQuestions(categorySelect?.value || null);

        // Фильтрация по поиску
        const searchQuery = searchInput?.value?.toLowerCase();
        if (searchQuery) {
            questions = questions.filter(q =>
                q.question?.toLowerCase().includes(searchQuery)
            );
        }

        if (!questions || questions.length === 0) {
            container.innerHTML = '<div class="no-results">Вопросов не найдено</div>';
            return;
        }

        container.innerHTML = questions.map(question => `
            <article>
                <div style="width: 70px; height: 100px;">
                    <i style="margin: 6px 0px 5px 20px; font-size: 30px;" class="fa-regular fa-circle-user"></i>
                    <h4 style="margin: 7px 7px 11px 5px; text-align: center;">${escapeHtml(question.author?.userName || 'Неизвестен')}</h4>
                    <em style="font-size: 12px; text-align: center;">${question.createdAt ? new Date(question.createdAt).toLocaleString() : ''}</em>
                </div>
                <div>
                    <h2>
                        <a style="color: black;" href="/question/${question.id}">${escapeHtml(question.question)}</a>
                    </h2>
                    <br />
                    <p style="color: rgb(26, 26, 26); font-size: 11px; margin-bottom: 7px;">${escapeHtml(question.question?.substring(0, 200) || '')}...</p>
                    <button>${escapeHtml(question.category || 'Общее')}</button>
                    <br />
                    <br />
                    <i class="fa-etch fa-solid fa-comment"></i>
                    <em>${question.answerCount || 0} ответов</em>
                </div>
            </article>
        `).join('');

    } catch (error) {
        console.error('Error loading questions:', error);
        container.innerHTML = '<div class="error">Ошибка загрузки вопросов</div>';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// Обновление UI авторизации
function updateAuthUI() {
    const authButtons = document.getElementById('auth-buttons');
    const token = localStorage.getItem('authToken');

    if (token) {
        authButtons.innerHTML = `
            <button id="logout-btn" style="background-color: black; color: white; padding: 8px 16px;">Выйти</button>
            <button id="ask-question-btn" style="background-color: black; color: white; padding: 8px 16px; margin-left: 10px;">Задать вопрос</button>
        `;
        document.getElementById('logout-btn')?.addEventListener('click', async () => {
            const { auth } = await import('./api.js');
            await auth.logout();
            location.reload();
        });
        document.getElementById('ask-question-btn')?.addEventListener('click', () => {
            window.location.href = '/ask-question';
        });
    } else {
        authButtons.innerHTML = `
            <button><a href="/sign-in" style="color: white;">Войти</a></button>
        `;
    }
}

// Обработчики событий
document.getElementById('search-btn')?.addEventListener('click', loadQuestions);
document.getElementById('search-input')?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') loadQuestions();
});
document.getElementById('category-select')?.addEventListener('change', loadQuestions);

// Инициализация
document.addEventListener('DOMContentLoaded', () => {
    updateAuthUI();
    loadQuestions();
});