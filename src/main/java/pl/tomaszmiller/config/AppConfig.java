package pl.tomaszmiller.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.database.DatabaseConnector;
import pl.tomaszmiller.database.MySqlConnector;
import pl.tomaszmiller.database.SqliteConnector;
import pl.tomaszmiller.repository.port.*;
import pl.tomaszmiller.repository.rest.SupabaseBookRepository;
import pl.tomaszmiller.repository.rest.SupabaseRentalRepository;
import pl.tomaszmiller.repository.rest.SupabaseUserRepository;
import pl.tomaszmiller.repository.sql.*;
import pl.tomaszmiller.service.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application-wide object graph / service locator.
 *
 * <p>Reads the configured datasource type from {@code application.properties}
 * (key: {@code alexandria.datasource.type}) or the environment variable
 * {@code ALEXANDRIA_DATASOURCE_TYPE}, then wires up the appropriate
 * repository implementations and services.
 *
 * <p>Supported values: {@code MYSQL} (default), {@code SQLITE}, {@code REST_API}.
 */
public final class AppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);
    private static final AppConfig INSTANCE = new AppConfig();

    private final DatasourceType datasourceType;
    private final DatabaseConnector databaseConnector;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;
    private final ExtensionRequestRepository extensionRequestRepository;
    private final ReservationRepository reservationRepository;
    private final BookService bookService;
    private final UserService userService;
    private final RentalService rentalService;
    private final AuthService authService;
    private final ExtensionRequestService extensionRequestService;
    private final ReservationService reservationService;
    private final OpenLibraryService openLibraryService;

    private AppConfig() {
        Properties properties = loadProperties();
        datasourceType = resolveDatasourceType(properties);
        LOGGER.info("Datasource type: {}", datasourceType);

        switch (datasourceType) {
            case SQLITE -> {
                databaseConnector = SqliteConnector.getInstance();
                bookRepository = new SqlBookRepository(databaseConnector);
                userRepository = new SqlUserRepository(databaseConnector);
                rentalRepository = new SqlRentalRepository(databaseConnector);
                extensionRequestRepository = new SqlExtensionRequestRepository(databaseConnector);
                reservationRepository = new SqlReservationRepository(databaseConnector);
            }
            case REST_API -> {
                databaseConnector = null;
                String apiUrl = resolveValue(properties, "alexandria.api.url", "ALEXANDRIA_API_URL", "");
                String apiKey = resolveValue(properties, "alexandria.api.key", "ALEXANDRIA_API_KEY", "");
                if (apiUrl.isBlank()) {
                    throw new IllegalStateException(
                            "REST_API datasource requires 'alexandria.api.url' or env ALEXANDRIA_API_URL to be set.");
                }
                if (apiKey.isBlank()) {
                    throw new IllegalStateException(
                            "REST_API datasource requires 'alexandria.api.key' or env ALEXANDRIA_API_KEY to be set.");
                }
                bookRepository = new SupabaseBookRepository(apiUrl, apiKey);
                userRepository = new SupabaseUserRepository(apiUrl, apiKey);
                rentalRepository = new SupabaseRentalRepository(apiUrl, apiKey);
                // REST API does not yet support extension requests / reservations;
                // fall back to null (features will be unavailable)
                extensionRequestRepository = null;
                reservationRepository = null;
            }
            default -> {
                databaseConnector = MySqlConnector.getInstance();
                bookRepository = new SqlBookRepository(databaseConnector);
                userRepository = new SqlUserRepository(databaseConnector);
                rentalRepository = new SqlRentalRepository(databaseConnector);
                extensionRequestRepository = new SqlExtensionRequestRepository(databaseConnector);
                reservationRepository = new SqlReservationRepository(databaseConnector);
            }
        }

        bookService = new BookService(bookRepository);
        userService = new UserService(userRepository);
        rentalService = new RentalService(rentalRepository, bookRepository);
        authService = new AuthService(userRepository);
        extensionRequestService = extensionRequestRepository != null
                ? new ExtensionRequestService(extensionRequestRepository, rentalRepository, reservationRepository)
                : null;
        reservationService = reservationRepository != null
                ? new ReservationService(reservationRepository)
                : null;
        openLibraryService = new OpenLibraryService();
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public DatasourceType getDatasourceType() {
        return datasourceType;
    }

    public BookService getBookService() {
        return bookService;
    }

    public UserService getUserService() {
        return userService;
    }

    public RentalService getRentalService() {
        return rentalService;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public ExtensionRequestService getExtensionRequestService() {
        return extensionRequestService;
    }

    public ReservationService getReservationService() {
        return reservationService;
    }

    public OpenLibraryService getOpenLibraryService() {
        return openLibraryService;
    }

    /**
     * Closes the database connector if applicable (called on application stop).
     */
    public void shutdown() {
        if (databaseConnector != null) {
            databaseConnector.close();
        }
    }

    private Properties loadProperties() {
        Properties p = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to load application.properties.", e);
        }
        return p;
    }

    private DatasourceType resolveDatasourceType(Properties properties) {
        String value = resolveValue(properties, "alexandria.datasource.type", "ALEXANDRIA_DATASOURCE_TYPE", "MYSQL");
        return DatasourceType.fromString(value, DatasourceType.MYSQL);
    }

    private String resolveValue(Properties props, String key, String envKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }
        String propVal = props.getProperty(key);
        if (propVal != null && !propVal.isBlank()) {
            return propVal.trim();
        }
        return defaultValue;
    }
}
