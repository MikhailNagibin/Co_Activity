# `frontend/web`

Это активный frontend Co_Activity на React + Vite.

## Команды

Установка зависимостей:

```bash
npm ci
```

Запуск dev-сервера:

```bash
npm run dev
```

Lint:

```bash
npm run lint
```

Production build:

```bash
npm run build
```

Preview production build:

```bash
npm run preview
```

## Как frontend подключается к backend

Локально frontend не должен напрямую ходить в другой origin с CORS-настройками.

Нормальная схема:

- frontend открыт на `http://localhost:5173`
- запросы на `/api/*` проксируются Vite на `http://localhost:8080`

Это настроено в [vite.config.js](./vite.config.js).

## Backend, который нужен frontend

Чтобы приложение реально работало, должен быть запущен `core-service` на:

```text
http://localhost:8080
```

Auth работает через cookie-based session:

- session cookie: `COACTIVITY_SESSION`
- CSRF cookie: `XSRF-TOKEN`

Frontend не хранит auth token в `localStorage`.

## Переопределение API base URL

По умолчанию код уже рассчитан на локальный backend.

Если всё же нужно вручную переопределить API URL:

```bash
cp .env.example .env
```

И задать:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

Обычно для локальной разработки это не нужно, потому что Vite proxy уже решает задачу.
