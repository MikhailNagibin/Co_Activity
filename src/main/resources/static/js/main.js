import { rooms, auth } from './api.js';

// Загрузка комнат
async function loadRooms() {
    const container = document.getElementById('rooms-container');
    const searchInput = document.getElementById('search-input');
    const categorySelect = document.getElementById('category-select');

    try {
        const filters = {
            query: searchInput?.value || '',
            category: categorySelect?.value || null
        };

        const roomList = await rooms.getAll(filters);

        if (!roomList || roomList.length === 0) {
            container.innerHTML = '<div class="no-results">Ничего не найдено</div>';
            return;
        }

        container.innerHTML = roomList.map(room => `
            <article>
                <div>
                    <img src="${room.imageIds?.[0] ? `https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800` : 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800'}" alt="">
                </div>
                <h2 style="margin: 20px 5px 10px 15px;">
                    <a style="color: black;" href="/room/${room.id}">${escapeHtml(room.name)}</a>
                </h2>
                <p>${escapeHtml(room.description?.substring(0, 100) || '')}...</p>
                <hr />
                <p>${escapeHtml(room.city || 'Не указан')}</p>
                <p>${room.dateOfStartEvent ? new Date(room.dateOfStartEvent).toLocaleDateString() : 'Дата не указана'}</p>
                <p>Участников: ${room.participantCount || 0}/${room.maximumParticipants || 0}</p>
                <div style="position: relative;">
                    <i style="position: absolute; margin: 6px 0px 13px 7px;" class="fa-regular fa-circle-user"></i>
                    <h5 style="padding: 7px 7px 15px 22px;">${escapeHtml(room.creator?.userName || 'Неизвестен')}</h5>
                </div>
                ${room.isCurrentUserParticipant ?
                    '<button class="joined-btn" disabled>Вы участник</button>' :
                    `<button onclick="joinRoom(${room.id})" class="join-btn">Присоединиться</button>`
                }
            </article>
        `).join('');

    } catch (error) {
        console.error('Error loading rooms:', error);
        container.innerHTML = '<div class="error">Ошибка загрузки комнат</div>';
    }
}

// Вспомогательная функция для экранирования HTML
function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// Присоединение к комнате
window.joinRoom = async function(roomId) {
    try {
        await rooms.join(roomId);
        alert('Вы успешно присоединились к комнате!');
        loadRooms(); // Обновляем список
    } catch (error) {
        alert('Ошибка: ' + error.message);
    }
};

// Обновление UI в зависимости от авторизации
function updateAuthUI() {
    const authButtons = document.getElementById('auth-buttons');
    const token = localStorage.getItem('authToken');

    if (token) {
        authButtons.innerHTML = `
            <button id="logout-btn" style="background-color: black; color: white; padding: 8px 16px;">Выйти</button>
        `;
        document.getElementById('logout-btn')?.addEventListener('click', async () => {
            await auth.logout();
            location.reload();
        });
    } else {
        authButtons.innerHTML = `
            <button><a href="/sign-in" style="color: white;">Войти</a></button>
        `;
    }
}

// Обработчики событий
document.getElementById('search-btn')?.addEventListener('click', loadRooms);
document.getElementById('search-input')?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') loadRooms();
});
document.getElementById('category-select')?.addEventListener('change', loadRooms);

// Инициализация
document.addEventListener('DOMContentLoaded', () => {
    updateAuthUI();
    loadRooms();
});