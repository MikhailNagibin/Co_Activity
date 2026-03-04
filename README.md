# Co_Activity

Backend service based on Java 21 + Spring Boot + PostgreSQL.

## Prerequisites

- Docker Desktop (with `docker compose`)
- Java 21 (for local run)
- Optional: Maven (or use `./mvnw`)

Quick checks:

```bash
java -version
./mvnw -v
docker --version
docker compose version
```

## Why `config.json` exists

This project has custom JDBC repositories that use `DataRepository`.
`DataRepository` reads DB settings from `DB_*` environment variables first, and if they are absent,
falls back to `src/main/resources/config.json`.

This allows:

- Docker run: use `DB_*` from compose (`postgres:5432`)
- Local run: keep using `config.json` (`localhost:5430`) if needed

## Start with Docker Compose (recommended)

1. Create local environment file:

```bash
cp .env.example .env
```

2. Start database + app:

```bash
docker compose up --build -d
```

3. Check status:

```bash
docker compose ps
docker compose logs -f postgres
docker compose logs -f app
```

4. Health check:

```bash
curl http://localhost:8080/actuator/health
```

5. Stop:

```bash
docker compose down
```

If you need a clean database:

```bash
docker compose down -v
```

## Mixed mode (DB in Docker, app local)

1. Start only PostgreSQL:

```bash
docker compose up -d postgres
```

2. Run app locally:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5430/postgres_db \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
DB_HOST=localhost \
DB_PORT=5430 \
DB_NAME=postgres_db \
DB_USER=postgres \
DB_PASSWORD=postgres \
./mvnw spring-boot:run
```
