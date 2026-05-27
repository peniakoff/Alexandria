package pl.tomaszmiller.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.model.UserRole;
import pl.tomaszmiller.repository.port.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void findAll_returnsAllUsers() throws Exception {
        User u1 = new User(1L, "Jan", "Kowalski", "jan@example.com", "600000001", UserRole.USER);
        User u2 = new User(2L, "Anna", "Nowak", "anna@example.com", "600000002", UserRole.ADMIN);
        when(userRepository.findAll()).thenReturn(Arrays.asList(u1, u2));
        List<User> result = userService.findAll();
        assertEquals(2, result.size());
    }

    @Test
    void findAll_returnsEmptyListOnException() throws Exception {
        when(userRepository.findAll()).thenThrow(new RuntimeException("DB error"));
        assertTrue(userService.findAll().isEmpty());
    }

    @Test
    void emailExists_returnsTrueWhenFound() throws Exception {
        when(userRepository.existsByEmail("jan@example.com")).thenReturn(true);
        assertTrue(userService.emailExists("jan@example.com"));
    }

    @Test
    void emailExists_returnsFalseWhenNotFound() throws Exception {
        when(userRepository.existsByEmail("missing@example.com")).thenReturn(false);
        assertFalse(userService.emailExists("missing@example.com"));
    }

    @Test
    void register_createsUserWithHashedPassword() throws Exception {
        User input = new User(0L, "Jan", "Kowalski", "jan@example.com", "600000001", UserRole.USER);
        User saved = new User(1L, "Jan", "Kowalski", "jan@example.com", "600000001", UserRole.USER);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        when(userRepository.save(eq(input), hashCaptor.capture())).thenReturn(saved);
        Optional<User> result = userService.register(input, "SecretPass123");
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
        String capturedHash = hashCaptor.getValue();
        assertNotEquals("SecretPass123", capturedHash);
        assertTrue(capturedHash.startsWith("$2"), "Expected a BCrypt hash starting with $2");
    }

    @Test
    void register_returnsEmptyOnException() throws Exception {
        User input = new User(0L, "Jan", "Kowalski", "jan@example.com", "600000001", UserRole.USER);
        when(userRepository.save(any(), anyString())).thenThrow(new RuntimeException("error"));
        Optional<User> result = userService.register(input, "password");
        assertFalse(result.isPresent());
    }

    @Test
    void updateProfile_returnsTrueOnSuccess() throws Exception {
        User user = new User(1L, "Jan", "Kowalski", "jan@example.com", "600000001", UserRole.USER);
        doNothing().when(userRepository).update(user);
        assertTrue(userService.updateProfile(user));
    }

    @Test
    void changePassword_returnsTrueOnSuccess() throws Exception {
        doNothing().when(userRepository).updatePassword(anyLong(), anyString());
        assertTrue(userService.changePassword(1L, "NewPass123"));
    }

    @Test
    void deleteUser_returnsTrueOnSuccess() throws Exception {
        doNothing().when(userRepository).deleteById(1L);
        assertTrue(userService.deleteUser(1L));
    }

    @Test
    void deleteUser_returnsFalseOnException() throws Exception {
        doThrow(new RuntimeException("error")).when(userRepository).deleteById(anyLong());
        assertFalse(userService.deleteUser(1L));
    }
}
