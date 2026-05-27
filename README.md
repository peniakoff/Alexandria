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

## SQLite mode (no Docker)

For quick offline development, set:

```properties
alexandria.datasource.type=SQLITE
alexandria.sqlite.path=./alexandria.db
```

in your local `src/main/resources/application.properties`.

## Notes about Docker app container

`docker-compose.yml` contains an `app` service, but JavaFX is a GUI application and typically should run on the host (not in a headless container). Use `docker-compose.dev.yml` for daily development.

## Project structure

- `src/main/java` – application code
- `src/main/resources` – FXML, CSS, logging, and config templates
- `src/test/java` – unit tests
- `docker-compose.dev.yml` – DB-only local Docker setup

## Remaining product work

The modernization closes the main technical gaps, but some product features still need implementation:

- admin user management
- loan management flows
- user settings persistence
- internationalization
