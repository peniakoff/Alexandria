# Alexandria

Alexandria is a JavaFX desktop application for simple library management backed by MySQL.
This modernization refreshes the project to Java 21, OpenJFX, MySQL Connector/J 9, HikariCP, BCrypt, SLF4J/Logback, Gradle, JUnit 5, and GitHub Actions CI.

## What changed

- migrated the project to Gradle
- replaced ad-hoc JDBC access with prepared statements and try-with-resources
- moved database configuration to properties/environment variables
- added pooled MySQL connections with HikariCP
- replaced SHA-256 password hashing with BCrypt
- removed the legacy JFoenix dependency in favor of standard JavaFX controls
- added unit tests and a CI workflow

## Requirements

- Java 21+
- Gradle 9.5+ (or use the included Gradle Wrapper)
- MySQL 8+

## Configuration

1. Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties`.
2. For Docker-based local development, default credentials are ready in the example file (`alexandria` / `changeme`).
3. `src/main/resources/application.properties` is ignored by git (`.gitignore`) and should stay local.

You can also override every property with environment variables:

- `ALEXANDRIA_DB_URL`
- `ALEXANDRIA_DB_USERNAME`
- `ALEXANDRIA_DB_PASSWORD`
- `ALEXANDRIA_DB_POOL_SIZE`
- `ALEXANDRIA_DB_MINIMUM_IDLE`

## Recommended local development (Desktop on host + API+DB in Docker)

Run the backend stack (MySQL + Keycloak + Micronaut API) in Docker:

```bash
docker compose up -d --build
docker compose exec db mysqladmin ping -h localhost -u root -prootpassword
```

Then run the desktop app on the host (configured to call the API):

```bash
./gradlew clean test
ALEXANDRIA_DATASOURCE_TYPE=REST_API ALEXANDRIA_API_URL=http://localhost:8080 ./gradlew :desktop:run
```

API endpoints for this setup:

- API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/`
- OpenAPI YAML: `http://localhost:8080/swagger/alexandria-api-1.0.0.yml`
- Keycloak (dev): `http://localhost:8081`

## DB-only Docker (optional)

If you only want MySQL (no API/Keycloak), you can still use:

```bash
docker compose -f docker-compose.dev.yml up -d
```

## Notes about Docker app container

The desktop app is a GUI application and typically should run on the host (not in a headless container). The Docker
setup is intended for `db` + `api` (+ `keycloak`) only.

## Project structure

- `core/` – domain + use-cases (no UI/HTTP)
- `api/` – Micronaut API (Docker)
- `desktop/` – JavaFX desktop client
- `docker-compose.yml` – API+Keycloak+DB local Docker stack
- `docker-compose.dev.yml` – DB-only local Docker setup

## Test accounts (development only)

The database is seeded with two test accounts for local development. **Do not use these accounts in production deployments.**

| Role  | E-mail                    | Password   |
|-------|---------------------------|------------|
| Admin | `admin@alexandria.local`  | `Admin123!` |
| User  | `user@alexandria.local`   | `User1234!` |

## Features

- **Authentication**: Login, registration, logout, BCrypt password hashing
- **Book catalog**: Browse, search (auto-debounce), paginated results (20/50/100 per page)
- **Book details**: Author, title, pages, ISBN, year of publication, publisher, status with labeled fields
- **Borrowing**: Borrow available books, 14-day loan period
- **Rental status**: Color-coded (green=active, yellow=expiring soon, red=overdue)
- **Extension requests**: Users can request a 7-day extension when ≤3 days remain; admin approves/rejects
- **Reservations**: Users can reserve borrowed/unavailable books; admin approves/rejects
- **Admin panel**: User management, rental tracking, book catalog CRUD, extension & reservation management
- **Internationalization**: English and Polish language support with runtime switching
- **Modern UI**: Rounded corners, subtle shadows, card-based layout, clean typography

## Remaining product work

- copy management (multiple copies per book title)
- late fee calculation
- account lockout / brute-force protection
- REST API layer
