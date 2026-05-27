# Alexandria

Alexandria is a JavaFX desktop application for simple library management backed by MySQL.
This modernization refreshes the project to Java 17, OpenJFX, MySQL Connector/J 8, HikariCP, BCrypt, SLF4J/Logback, Maven, JUnit 5, and GitHub Actions CI.

## What changed

- migrated the project to Maven
- replaced ad-hoc JDBC access with prepared statements and try-with-resources
- moved database configuration to properties/environment variables
- added pooled MySQL connections with HikariCP
- replaced SHA-256 password hashing with BCrypt
- removed the legacy JFoenix dependency in favor of standard JavaFX controls
- added unit tests and a CI workflow

## Requirements

- Java 17+
- Maven 3.9+
- MySQL 8+

## Configuration

1. Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties`.
2. Fill in your database credentials.

You can also override every property with environment variables:

- `ALEXANDRIA_DB_URL`
- `ALEXANDRIA_DB_USERNAME`
- `ALEXANDRIA_DB_PASSWORD`
- `ALEXANDRIA_DB_POOL_SIZE`
- `ALEXANDRIA_DB_MINIMUM_IDLE`

## Running locally

```bash
mvn clean test
mvn javafx:run
```

## Project structure

- `src/main/java` – application code
- `src/main/resources` – FXML, CSS, logging, and config templates
- `src/test/java` – unit tests

## Remaining product work

The modernization closes the main technical gaps, but some product features still need implementation:

- admin user management
- loan management flows
- user settings persistence
- internationalization
