package pl.tomaszmiller.security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or blank");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean verify(String password, String hash) {
        if (password == null || password.isBlank() || hash == null || hash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
