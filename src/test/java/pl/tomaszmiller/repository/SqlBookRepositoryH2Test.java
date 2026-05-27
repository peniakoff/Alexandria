package pl.tomaszmiller.repository;

import org.junit.jupiter.api.Test;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.repository.sql.SqlBookRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SqlBookRepositoryH2Test extends H2TestBase {

    private SqlBookRepository getRepo() {
        return new SqlBookRepository(connector);
    }

    @Test
    void saveThenFindById() throws Exception {
        Book input = new Book(0L, "Stanisław Lem", "Solaris", 320, null, BookStatus.AVAILABLE);
        Book saved = getRepo().save(input);
        assertTrue(saved.id() > 0, "Generated id should be positive");

        Optional<Book> found = getRepo().findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("Stanisław Lem", found.get().author());
        assertEquals("Solaris", found.get().title());
        assertEquals(320, found.get().pages());
    }

    @Test
    void findByTitle_returnsCorrectBook() throws Exception {
        getRepo().save(new Book(0L, "Stanisław Lem", "Solaris", 320, null, BookStatus.AVAILABLE));
        Optional<Book> result = getRepo().findByTitle("Solaris");
        assertTrue(result.isPresent());
        assertEquals("Stanisław Lem", result.get().author());
    }

    @Test
    void findByTitle_returnsEmptyWhenNotFound() throws Exception {
        Optional<Book> result = getRepo().findByTitle("Nieistniejąca Książka");
        assertFalse(result.isPresent());
    }

    @Test
    void loadBookTitles_returnsSortedAlphabetically() throws Exception {
        getRepo().save(new Book(0L, "B Author", "Zemsta", 100, null, BookStatus.AVAILABLE));
        getRepo().save(new Book(0L, "A Author", "Alicja", 200, null, BookStatus.AVAILABLE));
        getRepo().save(new Book(0L, "C Author", "Mała", 150, null, BookStatus.AVAILABLE));
        List<String> titles = getRepo().loadBookTitles();
        assertEquals(3, titles.size());
        assertEquals("Alicja", titles.get(0));
        assertEquals("Mała", titles.get(1));
        assertEquals("Zemsta", titles.get(2));
    }

    @Test
    void findAll_returnsAllBooks() throws Exception {
        getRepo().save(new Book(0L, "Author1", "Title1", 100, null, BookStatus.AVAILABLE));
        getRepo().save(new Book(0L, "Author2", "Title2", 200, null, BookStatus.BORROWED));
        List<Book> books = getRepo().findAll();
        assertEquals(2, books.size());
    }

    @Test
    void update_changesBookFields() throws Exception {
        Book saved = getRepo().save(new Book(0L, "Old Author", "Old Title", 100, null, BookStatus.AVAILABLE));
        Book updated = new Book(saved.id(), "New Author", "New Title", 200, "978-0-000000-00-0", BookStatus.BORROWED);
        getRepo().update(updated);

        Optional<Book> found = getRepo().findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("New Author", found.get().author());
        assertEquals("New Title", found.get().title());
        assertEquals(200, found.get().pages());
        assertEquals(BookStatus.BORROWED, found.get().status());
    }

    @Test
    void deleteById_removesBook() throws Exception {
        Book saved = getRepo().save(new Book(0L, "Author", "Title", 100, null, BookStatus.AVAILABLE));
        getRepo().deleteById(saved.id());
        Optional<Book> found = getRepo().findById(saved.id());
        assertFalse(found.isPresent());
    }
}
