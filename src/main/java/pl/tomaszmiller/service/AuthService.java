package pl.tomaszmiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.repository.port.UserRepository;
import pl.tomaszmiller.session.UserSession;

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
     * On success, sets the authenticated user in {@link UserSession}.
     *
     * @return the authenticated {@link User}, or empty on failure
     */
    public Optional<User> login(String email, String plainPassword) {
        try {
            Optional<String> hashOpt = userRepository.findPasswordHashByEmail(email);
            if (hashOpt.isEmpty()) {
                return Optional.empty();
            }
            if (!Utils.verifyPassword(plainPassword, hashOpt.get())) {
                return Optional.empty();
            }
            Optional<User> userOpt = userRepository.findByEmail(email);
            userOpt.ifPresent(UserSession::getInstance);
            if (userOpt.isPresent()) {
                UserSession.setCurrentUser(userOpt.get());
            }
            return userOpt;
        } catch (Exception e) {
            LOGGER.error("Authentication error for email '{}'.", email, e);
            return Optional.empty();
        }
    }

    /** Clears the current session. */
    public void logout() {
        UserSession.clearCurrentUser();
    }
}
