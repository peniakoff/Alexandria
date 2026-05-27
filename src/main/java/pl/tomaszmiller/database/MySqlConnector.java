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

public final class MySqlConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlConnector.class);
    private static final String APP_PROPERTIES = "application.properties";

    private final HikariDataSource dataSource;

    private MySqlConnector() {
        Properties properties = loadProperties();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(resolveValue(properties, "alexandria.db.url", "ALEXANDRIA_DB_URL",
                "jdbc:mysql://localhost:3306/alexandria?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true"));
        config.setUsername(resolveValue(properties, "alexandria.db.username", "ALEXANDRIA_DB_USERNAME", "root"));
        config.setPassword(resolveValue(properties, "alexandria.db.password", "ALEXANDRIA_DB_PASSWORD", ""));
        config.setMaximumPoolSize(Integer.parseInt(resolveValue(properties, "alexandria.db.poolSize", "ALEXANDRIA_DB_POOL_SIZE", "10")));
        config.setMinimumIdle(Integer.parseInt(resolveValue(properties, "alexandria.db.minimumIdle", "ALEXANDRIA_DB_MINIMUM_IDLE", "2")));
        config.setPoolName("alexandria-pool");
        config.setConnectionTimeout(10_000);
        config.setValidationTimeout(5_000);
        config.setInitializationFailTimeout(-1);
        this.dataSource = new HikariDataSource(config);
    }

    public static MySqlConnector getInstance() {
        return Holder.INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(APP_PROPERTIES)) {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                LOGGER.info("No {} file found on the classpath; environment variables or defaults will be used.", APP_PROPERTIES);
            }
        } catch (IOException exception) {
            LOGGER.warn("Unable to load {}. Falling back to environment variables/defaults.", APP_PROPERTIES, exception);
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

    private static final class Holder {
        private static final MySqlConnector INSTANCE = new MySqlConnector();
    }
}
