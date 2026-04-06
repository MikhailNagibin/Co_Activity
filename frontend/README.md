# Frontend

Каталог `frontend` разделён на две части:

- `web` — активное React-приложение
- `legacy` — старые статические макеты и референсы

Правило простое:

- весь новый frontend-код пишется только в `frontend/web`
- `legacy` не участвует в runtime и не должен получать новые фичи

## Как запустить frontend

```bash
cd frontend/web
npm ci
npm run dev
```

Открыть:

```text
http://localhost:5173
```

Для локальной разработки frontend ожидает backend на `http://localhost:8080` и использует Vite proxy для `/api`.

Подробности по active app смотри в [frontend/web/README.md](/Users/bomnik/IdeaProjects/Co_Activity/frontend/web/README.md).
