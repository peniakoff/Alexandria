package pl.tomaszmiller.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void shouldHashAndVerifyPassword() {
        String hash = PasswordHasher.hash("SecretPass123");

        assertNotEquals("SecretPass123", hash);
        assertTrue(PasswordHasher.verify("SecretPass123", hash));
        assertFalse(PasswordHasher.verify("wrong", hash));
    }

    @Test
    void shouldRejectBlankPasswordWhenHashing() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(" "));
    }
}

