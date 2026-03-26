const API_BASE_URL = '/api';

let authToken = localStorage.getItem('authToken');

export const api = {
    setToken(token) {
        authToken = token;
        if (token) {
            localStorage.setItem('authToken', token);
        } else {
            localStorage.removeItem('authToken');
        }
    },

    getToken() {
        return authToken;
    },

    getHeaders(includeAuth = true) {
        const headers = {
            'Content-Type': 'application/json'
        };
        if (includeAuth && authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }
        return headers;
    },

    async get(endpoint, includeAuth = true) {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'GET',
            headers: this.getHeaders(includeAuth)
        });
        return this.handleResponse(response);
    },

    async post(endpoint, data, includeAuth = true) {
        console.log(`POST ${endpoint}`, data);
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'POST',
            headers: this.getHeaders(includeAuth),
            body: JSON.stringify(data)
        });
        console.log(`POST ${endpoint} response status:`, response.status);
        return this.handleResponse(response);
    },

    async put(endpoint, data, includeAuth = true) {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'PUT',
            headers: this.getHeaders(includeAuth),
            body: JSON.stringify(data)
        });
        return this.handleResponse(response);
    },

    async delete(endpoint, includeAuth = true) {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'DELETE',
            headers: this.getHeaders(includeAuth)
        });
        return this.handleResponse(response);
    },

    async handleResponse(response) {
        console.log('Response status:', response.status);
        
        if (response.status === 401) {
            this.setToken(null);
            window.location.href = '/sign-in';
            throw new Error('Unauthorized');
        }

        if (response.status === 204) {
            return null;
        }

        // Проверяем, есть ли тело ответа (например, для успешного логина)
        const contentLength = response.headers.get('content-length');
        if (response.status === 200 && (!contentLength || contentLength === '0')) {
            return null;
        }

        let data;
        try {
            data = await response.json();
        } catch (e) {
            console.error('Failed to parse response:', e);
            throw new Error('Invalid server response');
        }

        if (!response.ok) {
            const errorMessage = data.message || data.error || 'Request failed';
            throw new Error(errorMessage);
        }

        return data;
    }
};

export const auth = {
    async login(login, password) {
        return api.post('/users/login', { login, password }, false);
    },

    async verify(login, code) {
        console.log('Sending verification request:', {login, code});
        const response = await fetch(`${API_BASE_URL}/users/login/verify?login=${encodeURIComponent(login)}&code=${encodeURIComponent(code)}`, {
            method: 'POST'
        });
        console.log('Verification response status:', response.status);
        const data = await response.json();
        console.log('Verification response data:', data);
        if (!response.ok) {
            throw new Error(data.message || 'Verification failed');
        }
        if (data.token) {
            api.setToken(data.token);
        }
        return data;
    },

    async logout() {
        await api.post('/users/logout');
        api.setToken(null);
        window.location.href = '/main';
    },

    async register(userData) {
        console.log('Register called with:', userData);
        return api.post('/users', userData, false);
    },

    async getProfile() {
        return api.get('/users/me');
    }
};

export const rooms = {
    async getAll(filters = {}, sortBy = null) {
        let url = '/rooms?';
        if (filters.category) url += `category=${filters.category}&`;
        if (filters.query) url += `query=${encodeURIComponent(filters.query)}&`;
        if (filters.city) url += `city=${encodeURIComponent(filters.city)}&`;
        if (filters.isPublic !== undefined) url += `isPublic=${filters.isPublic}&`;
        if (sortBy) url += `sortBy=${sortBy}&`;
        return api.get(url, false);
    },

    async getById(roomId) {
        return api.get(`/rooms/${roomId}`, false);
    },

    async getUserRooms() {
        return api.get('/rooms/me');
    },

    async join(roomId) {
        return api.post(`/rooms/${roomId}/join`, null);
    },

    async leave(roomId) {
        return api.post(`/rooms/${roomId}/leave`, null);
    },

    async create(roomData) {
        return api.post('/rooms/createRoom', roomData);
    }
};

export const qa = {
    async getQuestions(categoryId = null) {
        let url = '/qa/questions';
        if (categoryId) url += `/category?categoryId=${categoryId}`;
        return api.get(url, false);
    },

    async getQuestionWithAnswers(questionId) {
        return api.get(`/qa/questions/${questionId}`, false);
    },

    async askQuestion(questionData) {
        return api.post('/qa/questions', questionData);
    },

    async answerQuestion(answerData) {
        return api.post('/qa/answers', answerData);
    }
};
