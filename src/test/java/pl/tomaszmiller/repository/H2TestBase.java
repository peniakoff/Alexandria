package pl.tomaszmiller.repository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import pl.tomaszmiller.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for H2-backed repository integration tests.
 * Creates an in-memory H2 database with the Alexandria schema before each test class.
 */
public abstract class H2TestBase {

    protected static DatabaseConnector connector;

    @BeforeAll
    static void initDatabase() {
        connector = new H2DatabaseConnector();
        createSchema();
    }

    @AfterAll
    static void closeDatabase() {
        connector.close();
    }

    @BeforeEach
    void clearTables() throws SQLException {
        try (Connection c = connector.getConnection(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM extension_requests");
            st.execute("DELETE FROM reservations");
            st.execute("DELETE FROM rentals");
            st.execute("DELETE FROM books");
            st.execute("DELETE FROM users");
        }
    }

    private static void createSchema() {
        try (Connection c = connector.getConnection(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                        f_name     VARCHAR(100) NOT NULL,
                        l_name     VARCHAR(100) NOT NULL,
                        email      VARCHAR(255) NOT NULL UNIQUE,
                        password   VARCHAR(255) NOT NULL,
                        phone_number VARCHAR(30),
                        user_rank  INT NOT NULL DEFAULT 0
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS books (
                        id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                        author       VARCHAR(200) NOT NULL,
                        title        VARCHAR(300) NOT NULL,
                        pages        INT NOT NULL DEFAULT 0,
                        isbn         VARCHAR(20),
                        status       VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
                        publish_year INT DEFAULT 0,
                        publisher    VARCHAR(200)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS rentals (
                        id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id     BIGINT NOT NULL,
                        book_id     BIGINT NOT NULL,
                        borrow_date DATE NOT NULL,
                        due_date    DATE NOT NULL,
                        return_date DATE,
                        status      VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS extension_requests (
                        id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                        rental_id    BIGINT NOT NULL,
                        user_id      BIGINT NOT NULL,
                        status       VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        request_date DATE NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id      BIGINT NOT NULL,
                        book_id      BIGINT NOT NULL,
                        status       VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        request_date DATE NOT NULL
                    )
                    """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create H2 schema", e);
        }
    }
}
