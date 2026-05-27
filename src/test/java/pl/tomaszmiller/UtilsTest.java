package pl.tomaszmiller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void shouldHashAndVerifyPassword() {
        String hash = Utils.hashPassword("SecretPass123");

        assertNotEquals("SecretPass123", hash);
        assertTrue(Utils.verifyPassword("SecretPass123", hash));
        assertFalse(Utils.verifyPassword("wrong", hash));
    }

    @Test
    void shouldValidateEmailAddresses() {
        assertTrue(Utils.isValidEmail("user@example.com"));
        assertFalse(Utils.isValidEmail("user@example"));
        assertFalse(Utils.isValidEmail(""));
    }

    @Test
    void shouldValidatePhoneNumbers() {
        assertTrue(Utils.isValidPhoneNumber("+48 600-700-800"));
        assertTrue(Utils.isValidPhoneNumber("600700800"));
        assertFalse(Utils.isValidPhoneNumber("12"));
    }

    @Test
    void shouldValidateNames() {
        assertTrue(Utils.isValidName("Tomasz"));
        assertTrue(Utils.isValidName("Anna Maria"));
        assertFalse(Utils.isValidName("1User"));
    }
}
