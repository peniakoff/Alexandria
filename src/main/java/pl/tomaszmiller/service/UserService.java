package pl.tomaszmiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.repository.port.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Application-level operations on user accounts.
 */
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            LOGGER.error("Unable to load users.", e);
            return Collections.emptyList();
        }
    }

    public Optional<User> findById(long id) {
        try {
            return userRepository.findById(id);
        } catch (Exception e) {
            LOGGER.error("Unable to find user id={}.", id, e);
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        try {
            return userRepository.findByEmail(email);
        } catch (Exception e) {
            LOGGER.error("Unable to find user by email.", e);
            return Optional.empty();
        }
    }

    public boolean emailExists(String email) {
        try {
            return userRepository.existsByEmail(email);
        } catch (Exception e) {
            LOGGER.error("Unable to check email existence.", e);
            return false;
        }
    }

    public Optional<User> register(User user, String plainPassword) {
        try {
            String hash = Utils.hashPassword(plainPassword);
            return Optional.of(userRepository.save(user, hash));
        } catch (Exception e) {
            LOGGER.error("Unable to register user '{}'.", user.email(), e);
            return Optional.empty();
        }
    }

    public boolean updateProfile(User user) {
        try {
            userRepository.update(user);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to update user id={}.", user.id(), e);
            return false;
        }
    }

    public boolean changePassword(long userId, String newPlainPassword) {
        try {
            userRepository.updatePassword(userId, Utils.hashPassword(newPlainPassword));
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to change password for user id={}.", userId, e);
            return false;
        }
    }

    public boolean deleteUser(long id) {
        try {
            userRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to delete user id={}.", id, e);
            return false;
        }
    }
}
