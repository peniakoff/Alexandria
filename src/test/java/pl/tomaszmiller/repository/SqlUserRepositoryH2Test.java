package pl.tomaszmiller.repository;

import org.junit.jupiter.api.Test;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.model.UserRole;
import pl.tomaszmiller.repository.sql.SqlUserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SqlUserRepositoryH2Test extends H2TestBase {

    private SqlUserRepository getRepo() {
        return new SqlUserRepository(connector);
    }

    @Test
    void saveThenFindById() throws Exception {
        User input = new User(0L, "Jan", "Kowalski", "jan@example.com", "600-000-001", UserRole.USER);
        User saved = getRepo().save(input, "$2a$12$hashedpassword");
        assertTrue(saved.id() > 0);

        Optional<User> found = getRepo().findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("Jan", found.get().firstName());
        assertEquals("Kowalski", found.get().lastName());
        assertEquals("jan@example.com", found.get().email());
        assertEquals(UserRole.USER, found.get().role());
    }

    @Test
    void findByEmail_returnsUser() throws Exception {
        User input = new User(0L, "Anna", "Nowak", "anna@example.com", "600-000-002", UserRole.ADMIN);
        getRepo().save(input, "somehash");
        Optional<User> found = getRepo().findByEmail("anna@example.com");
        assertTrue(found.isPresent());
        assertEquals("Anna", found.get().firstName());
        assertEquals(UserRole.ADMIN, found.get().role());
    }

    @Test
    void findByEmail_returnsEmptyWhenNotFound() throws Exception {
        Optional<User> result = getRepo().findByEmail("notexist@example.com");
        assertFalse(result.isPresent());
    }

    @Test
    void findPasswordHashByEmail_returnsHash() throws Exception {
        User input = new User(0L, "Piotr", "Wiśniewski", "piotr@example.com", "600-000-003", UserRole.USER);
        String hash = "$2a$12$somefakehash12345678";
        getRepo().save(input, hash);
        Optional<String> result = getRepo().findPasswordHashByEmail("piotr@example.com");
        assertTrue(result.isPresent());
        assertEquals(hash, result.get());
    }

    @Test
    void existsByEmail_returnsTrueAndFalse() throws Exception {
        User input = new User(0L, "Maria", "Wiśniewska", "maria@example.com", "600-000-004", UserRole.USER);
        getRepo().save(input, "hash");
        assertTrue(getRepo().existsByEmail("maria@example.com"));
        assertFalse(getRepo().existsByEmail("nonexistent@example.com"));
    }

    @Test
    void findAll_returnsAllUsers() throws Exception {
        getRepo().save(new User(0L, "Jan", "A", "a@example.com", "111", UserRole.USER), "h1");
        getRepo().save(new User(0L, "Jan", "B", "b@example.com", "222", UserRole.ADMIN), "h2");
        List<User> users = getRepo().findAll();
        assertEquals(2, users.size());
    }

    @Test
    void update_changesProfileFields() throws Exception {
        User saved = getRepo().save(
                new User(0L, "OldFirst", "OldLast", "update@example.com", "000", UserRole.USER), "hash");
        User updated = new User(saved.id(), "NewFirst", "NewLast", "update@example.com", "999", UserRole.USER);
        getRepo().update(updated);
        Optional<User> found = getRepo().findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("NewFirst", found.get().firstName());
        assertEquals("999", found.get().phoneNumber());
    }

    @Test
    void updatePassword_changesHash() throws Exception {
        User saved = getRepo().save(
                new User(0L, "Pass", "User", "pass@example.com", "000", UserRole.USER), "old-hash");
        getRepo().updatePassword(saved.id(), "new-hash");
        Optional<String> hash = getRepo().findPasswordHashByEmail("pass@example.com");
        assertTrue(hash.isPresent());
        assertEquals("new-hash", hash.get());
    }

    @Test
    void deleteById_removesUser() throws Exception {
        User saved = getRepo().save(
                new User(0L, "Del", "User", "del@example.com", "000", UserRole.USER), "hash");
        getRepo().deleteById(saved.id());
        assertFalse(getRepo().findById(saved.id()).isPresent());
    }
}
