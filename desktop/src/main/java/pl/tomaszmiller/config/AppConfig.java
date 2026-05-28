package pl.tomaszmiller.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.repository.http.HttpBookRepository;
import pl.tomaszmiller.repository.http.HttpRentalRepository;
import pl.tomaszmiller.repository.http.HttpUserRepository;
import pl.tomaszmiller.repository.port.*;
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
            case REST_API -> {
                String apiUrl = resolveValue(properties, "alexandria.api.url", "ALEXANDRIA_API_URL", "http://localhost:8080");
                bookRepository = new HttpBookRepository(apiUrl);
                userRepository = new HttpUserRepository(apiUrl);
                rentalRepository = new HttpRentalRepository(apiUrl);
                extensionRequestRepository = null;
                reservationRepository = null;
            }
            default -> {
                throw new IllegalStateException("Desktop no longer supports direct DB connections. Use REST_API.");
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
     * Desktop no longer owns DB resources.
     */
    public void shutdown() {
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
        String value = resolveValue(properties, "alexandria.datasource.type", "ALEXANDRIA_DATASOURCE_TYPE", "REST_API");
        return DatasourceType.fromString(value, DatasourceType.REST_API);
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
