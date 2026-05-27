package pl.tomaszmiller.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * SQLite-backed {@link DatabaseConnector} using HikariCP.
 * Suitable for local/offline library deployments without a server database.
 * The database file path is configured via {@code alexandria.sqlite.path} property
 * or the {@code ALEXANDRIA_SQLITE_PATH} environment variable.
 */
public final class SqliteConnector implements DatabaseConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqliteConnector.class);
    private static final String APP_PROPERTIES = "application.properties";
    private static volatile SqliteConnector instance;

    private final HikariDataSource dataSource;

    private SqliteConnector() {
        Properties properties = loadProperties();
        String filePath = resolveValue(properties, "alexandria.sqlite.path", "ALEXANDRIA_SQLITE_PATH", "./alexandria.db");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + filePath);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setPoolName("alexandria-sqlite-pool");
        config.setConnectionTestQuery("SELECT 1");
        this.dataSource = new HikariDataSource(config);
        LOGGER.info("SQLite connector initialized with file '{}'.", filePath);
    }

    public static SqliteConnector getInstance() {
        SqliteConnector current = instance;
        if (current == null) {
            synchronized (SqliteConnector.class) {
                current = instance;
                if (current == null) {
                    current = new SqliteConnector();
                    instance = current;
                }
            }
        }
        return current;
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
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

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(APP_PROPERTIES)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            LOGGER.warn("Unable to load {}. Using defaults.", APP_PROPERTIES, exception);
        }
        return properties;
    }

    private String resolveValue(Properties properties, String propertyKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return defaultValue;
    }
}
