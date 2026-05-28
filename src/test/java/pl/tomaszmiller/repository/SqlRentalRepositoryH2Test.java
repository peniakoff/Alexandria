package pl.tomaszmiller.repository;

import org.junit.jupiter.api.Test;
import pl.tomaszmiller.model.*;
import pl.tomaszmiller.repository.sql.SqlBookRepository;
import pl.tomaszmiller.repository.sql.SqlRentalRepository;
import pl.tomaszmiller.repository.sql.SqlUserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SqlRentalRepositoryH2Test extends H2TestBase {

    private SqlRentalRepository getRentalRepo() {
        return new SqlRentalRepository(connector);
    }

    private long createUser() throws Exception {
        return new SqlUserRepository(connector)
                .save(new User(0L, "Test", "User", "test" + System.nanoTime() + "@example.com", "000", UserRole.USER), "hash")
                .id();
    }

    private long createBook() throws Exception {
        return new SqlBookRepository(connector)
                .save(new Book(0L, "Author", "Title " + System.nanoTime(), 100, null, BookStatus.AVAILABLE))
                .id();
    }

    @Test
    void saveThenFindById() throws Exception {
        long userId = createUser();
        long bookId = createBook();
        LocalDate today = LocalDate.now();
        Rental input = new Rental(0L, userId, bookId, today, today.plusDays(14), null, RentalStatus.ACTIVE);
        Rental saved = getRentalRepo().save(input);
        assertTrue(saved.id() > 0);

        Optional<Rental> found = getRentalRepo().findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals(userId, found.get().userId());
        assertEquals(bookId, found.get().bookId());
        assertEquals(RentalStatus.ACTIVE, found.get().status());
        assertNull(found.get().returnDate());
    }

    @Test
    void findByUserId_returnsUserRentals() throws Exception {
        long userId = createUser();
        long bookId1 = createBook();
        long bookId2 = createBook();
        LocalDate today = LocalDate.now();
        getRentalRepo().save(new Rental(0L, userId, bookId1, today, today.plusDays(14), null, RentalStatus.ACTIVE));
        getRentalRepo().save(new Rental(0L, userId, bookId2, today, today.plusDays(14), null, RentalStatus.ACTIVE));
        List<Rental> rentals = getRentalRepo().findByUserId(userId);
        assertEquals(2, rentals.size());
    }

    @Test
    void findByStatus_returnsMatchingRentals() throws Exception {
        long userId = createUser();
        long bookId = createBook();
        LocalDate today = LocalDate.now();
        getRentalRepo().save(new Rental(0L, userId, bookId, today, today.plusDays(14), null, RentalStatus.ACTIVE));
        getRentalRepo().save(new Rental(0L, userId, createBook(), today.minusDays(20), today.minusDays(6), today, RentalStatus.RETURNED));
        List<Rental> active = getRentalRepo().findByStatus(RentalStatus.ACTIVE);
        assertTrue(active.stream().allMatch(r -> r.status() == RentalStatus.ACTIVE));
    }

    @Test
    void update_setsReturnDate() throws Exception {
        long userId = createUser();
        long bookId = createBook();
        LocalDate today = LocalDate.now();
        Rental saved = getRentalRepo().save(new Rental(0L, userId, bookId, today.minusDays(5), today.plusDays(9), null, RentalStatus.ACTIVE));
        Rental returned = new Rental(saved.id(), saved.userId(), saved.bookId(),
                saved.borrowDate(), saved.dueDate(), today, RentalStatus.RETURNED);
        getRentalRepo().update(returned);
        Optional<Rental> found = getRentalRepo().findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals(RentalStatus.RETURNED, found.get().status());
        assertEquals(today, found.get().returnDate());
    }

    @Test
    void findAll_returnsAllRentals() throws Exception {
        long userId = createUser();
        LocalDate today = LocalDate.now();
        getRentalRepo().save(new Rental(0L, userId, createBook(), today, today.plusDays(14), null, RentalStatus.ACTIVE));
        getRentalRepo().save(new Rental(0L, userId, createBook(), today, today.plusDays(14), null, RentalStatus.ACTIVE));
        assertTrue(getRentalRepo().findAll().size() >= 2);
    }
}
