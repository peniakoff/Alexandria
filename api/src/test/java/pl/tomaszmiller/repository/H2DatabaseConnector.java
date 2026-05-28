package pl.tomaszmiller.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pl.tomaszmiller.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * In-memory H2 {@link DatabaseConnector} for integration tests.
 * Uses MODE=MySQL to approximate MySQL dialect behaviour.
 */
public class H2DatabaseConnector implements DatabaseConnector {

    private final HikariDataSource dataSource;

    public H2DatabaseConnector() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setPoolName("h2-test-pool");
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
