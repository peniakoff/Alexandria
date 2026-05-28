package pl.tomaszmiller.repository;

import org.junit.jupiter.api.Test;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookInventory;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.repository.sql.SqlBookRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlBookRepositoryH2Test extends H2TestBase {

    private SqlBookRepository getRepo() {
        return new SqlBookRepository(connector);
    }

    @Test
    void saveThenFindById_persistsInventory() throws Exception {
        Book input = new Book(0L, "Stanisław Lem", "Solaris", 320, null,
                BookStatus.AVAILABLE, 1961, "MON", new BookInventory(4, 3, 1, 0, 0));
        Book saved = getRepo().save(input);

        Optional<Book> found = getRepo().findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals(4, found.get().inventory().activeCopies());
        assertEquals(3, found.get().inventory().availableCopies());
        assertEquals(1, found.get().inventory().archivedCopies());
    }

    @Test
    void loadBookTitles_returnsSortedAlphabetically() throws Exception {
        getRepo().save(new Book(0L, "B Author", "Zenith", 100, null, BookStatus.AVAILABLE));
        getRepo().save(new Book(0L, "A Author", "Alpha", 200, null, BookStatus.AVAILABLE));
        getRepo().save(new Book(0L, "C Author", "Minor", 150, null, BookStatus.AVAILABLE));

        List<String> titles = getRepo().loadBookTitles();

        assertEquals(List.of("Alpha", "Minor", "Zenith"), titles);
    }

    @Test
    void update_changesMetadataAndInventoryFields() throws Exception {
        Book saved = getRepo().save(new Book(0L, "Old Author", "Old Title", 100, null,
                BookStatus.AVAILABLE, 1990, "Old Publisher", new BookInventory(2, 2, 0, 0, 0)));
        Book updated = new Book(saved.id(), "New Author", "New Title", 200, "9780000000000",
                BookStatus.BORROWED, 2001, "New Publisher", new BookInventory(5, 1, 2, 1, 1));

        getRepo().update(updated);

        Optional<Book> found = getRepo().findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("New Author", found.get().author());
        assertEquals(5, found.get().inventory().activeCopies());
        assertEquals(2, found.get().inventory().archivedCopies());
        assertEquals(1, found.get().inventory().removedDamagedCopies());
        assertEquals(1, found.get().inventory().removedStolenCopies());
    }
}
