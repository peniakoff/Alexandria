package pl.tomaszmiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.repository.port.UserRepository;
import pl.tomaszmiller.security.PasswordHasher;

import java.util.Optional;

/**
 * Handles user authentication.
 */
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Attempts to authenticate the user.
     *
     * @return the authenticated {@link User}, or empty on failure
     */
    public Optional<User> login(String email, String plainPassword) {
        try {
            Optional<String> hashOpt = userRepository.findPasswordHashByEmail(email);
            if (hashOpt.isEmpty()) {
                return Optional.empty();
            }
            if (!PasswordHasher.verify(plainPassword, hashOpt.get())) {
                return Optional.empty();
            }
            return userRepository.findByEmail(email);
        } catch (Exception e) {
            LOGGER.error("Authentication error for email '{}'.", email, e);
            return Optional.empty();
        }
    }

    /**
     * Logout is a client concern (desktop/frontend). Server-side auth is token-based.
     */
    public void logout() {
    }
}
