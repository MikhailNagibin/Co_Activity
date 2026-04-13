# AI Agent Guidelines for Co_Activity

This document contains essential context and project-specific conventions for AI coding agents to be immediately productive in the Co_Activity codebase.

## System Architecture
- **Backend (`services/core-service`)**: Java 21 + Spring Boot 3. This is the **only** public HTTP API handling auth, users, rooms, and Q&A (which is integrated here, not in a separate service).
- **Async Workers (`services/notifications-service`)**: Kafka consumer for email notifications. Do not add public HTTP controllers here.
- **Frontend (`frontend/web`)**: React 19 + Vite. Completely ignore the `frontend/legacy` folder (dead HTML/CSS mockups).
- **Core Infrastructure**: PostgreSQL 16, Redis 7, Kafka. Managed via `docker-compose.yml` locally.

## Authentication & Data Flow
- **Session Auth (No JWT)**: The project completely removed JWT in favor of Spring Session + Redis.
- **Cookies**: Authentication relies on the `COACTIVITY_SESSION` cookie. Do NOT write frontend code that attempts to save or read tokens from `localStorage`.
- **CSRF**: Mutating REST API endpoints require a valid CSRF token. The frontend expects `XSRF-TOKEN` to be present.
- **API Proxy**: In dev mode, `frontend/web/vite.config.js` proxies `/api` to `localhost:8080`. Do not configure direct cross-origin calls or hardcode localhost in the React app.

## Critical Workflows & Commands
- **Local Dev**: Run `docker compose --env-file .env.local -f docker-compose.yml up --build -d`.
- **Database Drift**: If `core-service` fails to start on DB migrations (e.g., duplicate `email_normalized`), it's due to stale local data. Fix it using: `docker compose down -v && docker compose up --build -d`.
- **Kafka Bootstrapping**: `notifications-service` will **fail-fast** if topics don't exist. Topics are created externally by `scripts/create-kafka-topics.sh` via the `kafka-topics-init` compose service. Do not enable Spring's Kafka auto-topic creation.
- **Integration Tests**: Tests require Docker for Testcontainers. Run them explicitly with the maven profile: `./mvnw -f services/core-service/pom.xml -Pwith-docker-tests test`.
- **Frontend Dependencies**: Always use `npm ci` instead of `npm install` in `frontend/web` to guarantee lockfile consistency.

## Environment & Configuration
- **Mailpit vs Yandex**: `docker-compose.yml` forces the `local` Spring profile for `notifications-service` (routes to Mailpit). `docker-compose.prod.yml` uses the `prod` profile and requires real SMTP credentials in `.env.prod`.
