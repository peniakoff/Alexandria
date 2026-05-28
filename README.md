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

## Recommended local development (JavaFX on host + MySQL in Docker)

Run only the database in Docker:

```bash
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml exec db mysqladmin ping -h localhost -u root -prootpassword
```

Then run the JavaFX app on the host:

```bash
./gradlew clean test
./gradlew run
```

Default MySQL connection for this setup:

- host: `127.0.0.1`
- port: `3306`
- database: `alexandria`
- user: `alexandria`
- password: `changeme`

## SQLite mode (existing database only)

For offline development against an already initialized SQLite database, set:

```properties
alexandria.datasource.type=SQLITE
alexandria.sqlite.path=./alexandria.db
```

in your local `src/main/resources/application.properties`.

The SQLite connector only opens the configured file and does not create the
schema automatically, so `./alexandria.db` must already contain the `users`,
`books`, and `rentals` tables before you start the application.

## Notes about Docker app container

`docker-compose.yml` contains an `app` service, but JavaFX is a GUI application and typically should run on the host (not in a headless container). Use `docker-compose.dev.yml` for daily development.

## Project structure

- `src/main/java` – application code
- `src/main/resources` – FXML, CSS, logging, and config templates
- `src/test/java` – unit tests
- `docker-compose.dev.yml` – DB-only local Docker setup

## Test accounts

The database is seeded with two test accounts:

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
