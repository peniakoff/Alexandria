package pl.tomaszmiller.repository.port;

import pl.tomaszmiller.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for user account persistence.
 */
public interface UserRepository {
    /**
     * Returns all registered users.
     */
    List<User> findAll() throws Exception;

    /**
     * Finds a user by primary key.
     */
    Optional<User> findById(long id) throws Exception;

    /**
     * Finds a user by e-mail address.
     */
    Optional<User> findByEmail(String email) throws Exception;

    /**
     * Returns the stored BCrypt password hash for the given e-mail,
     * or empty if the user does not exist.
     */
    Optional<String> findPasswordHashByEmail(String email) throws Exception;

    /**
     * Persists a new user; returns the saved entity with generated id.
     */
    User save(User user, String passwordHash) throws Exception;

    /**
     * Updates mutable user profile fields (name, phone).
     */
    void update(User user) throws Exception;

    /**
     * Changes the stored password hash for the given user.
     */
    void updatePassword(long userId, String newPasswordHash) throws Exception;

    /**
     * Removes a user by primary key.
     */
    void deleteById(long id) throws Exception;

    /**
     * Returns {@code true} if the given e-mail is already registered.
     */
    boolean existsByEmail(String email) throws Exception;
}
