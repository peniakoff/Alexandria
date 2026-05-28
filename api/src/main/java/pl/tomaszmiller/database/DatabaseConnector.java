package pl.tomaszmiller.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstraction over any SQL-based database connection source.
 * Implementations include MySQL (production), SQLite (local/offline).
 */
public interface DatabaseConnector extends AutoCloseable {
    Connection getConnection() throws SQLException;

    @Override
    void close();
}
