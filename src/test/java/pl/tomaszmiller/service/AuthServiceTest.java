package pl.tomaszmiller.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.model.UserRole;
import pl.tomaszmiller.repository.port.UserRepository;
import pl.tomaszmiller.session.UserSession;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository);
    }

    @AfterEach
    void tearDown() {
        UserSession.clearCurrentUser();
    }

    @Test
    void login_successWithValidCredentials() throws Exception {
        String email = "admin@example.com";
        String password = "SecretPass123";
        String hash = Utils.hashPassword(password);
        User user = new User(1L, "Admin", "User", email, "600000001", UserRole.ADMIN);

        when(userRepository.findPasswordHashByEmail(email)).thenReturn(Optional.of(hash));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = authService.login(email, password);
        assertTrue(result.isPresent());
        assertEquals(UserRole.ADMIN, result.get().role());
        assertEquals(user, UserSession.getCurrentUser());
    }

    @Test
    void login_failsWithWrongPassword() throws Exception {
        String email = "user@example.com";
        String hash = Utils.hashPassword("correctPassword");

        when(userRepository.findPasswordHashByEmail(email)).thenReturn(Optional.of(hash));

        Optional<User> result = authService.login(email, "wrongPassword");
        assertFalse(result.isPresent());
        assertNull(UserSession.getCurrentUser());
    }

    @Test
    void login_failsWhenUserNotFound() throws Exception {
        when(userRepository.findPasswordHashByEmail(anyString())).thenReturn(Optional.empty());
        Optional<User> result = authService.login("noone@example.com", "password");
        assertFalse(result.isPresent());
    }

    @Test
    void login_returnsEmptyOnException() throws Exception {
        when(userRepository.findPasswordHashByEmail(anyString())).thenThrow(new RuntimeException("DB error"));
        Optional<User> result = authService.login("user@example.com", "pass");
        assertFalse(result.isPresent());
    }

    @Test
    void logout_clearsSession() throws Exception {
        String email = "user@example.com";
        String password = "SecretPass123";
        String hash = Utils.hashPassword(password);
        User user = new User(2L, "Regular", "User", email, "600000002", UserRole.USER);
        when(userRepository.findPasswordHashByEmail(email)).thenReturn(Optional.of(hash));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        authService.login(email, password);

        authService.logout();
        assertNull(UserSession.getCurrentUser());
    }
}
