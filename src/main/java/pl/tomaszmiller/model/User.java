package pl.tomaszmiller.model;

/**
 * Immutable representation of a library user.
 *
 * @param id          unique identifier (0 for unsaved entities)
 * @param firstName   first name
 * @param lastName    last name
 * @param email       e-mail address (unique login identifier)
 * @param phoneNumber contact phone number
 * @param role        assigned role ({@link UserRole})
 */
public record User(
        long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UserRole role
) {
    /** Returns the full display name. */
    public String fullName() {
        return firstName + " " + lastName;
    }

    /** Returns {@code true} if this user has administrator privileges. */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
