package pl.tomaszmiller.session;

import pl.tomaszmiller.model.User;

/**
 * Holds the currently authenticated {@link User} for the lifetime of the application session.
 * This is a simple thread-local-free singleton suitable for a single-user desktop application.
 */
public final class UserSession {

    private static User currentUser;

    private UserSession() {
    }

    /**
     * Returns the currently logged-in user, or {@code null} if not authenticated.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the authenticated user.
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Clears the authenticated user (logout).
     */
    public static void clearCurrentUser() {
        currentUser = null;
    }

    /**
     * Returns {@code true} if a user is currently logged in.
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
