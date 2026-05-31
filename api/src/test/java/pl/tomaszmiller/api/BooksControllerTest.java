package pl.tomaszmiller.api;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.Test;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookInventory;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.repository.port.BookRepository;
import pl.tomaszmiller.service.BookService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BooksControllerTest {

    @Test
    void create_preservesRequestedInitialCopyCount() {
        BooksController controller = new BooksController(new BookService(new InMemoryBookRepository()));
        Book request = new Book(0L, "Adam Mickiewicz", "Pan Tadeusz", 328,
                "9788370819410", BookStatus.AVAILABLE, 1834, "PWN", new BookInventory(3));

        HttpResponse<Book> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatus());
        Book created = response.getBody(Book.class).orElseThrow();
        assertEquals(3, created.inventory().activeCopies());
        assertEquals(3, created.inventory().availableCopies());
    }

    @Test
    void create_rejectsNonPositiveInitialCopyCount() {
        BooksController controller = new BooksController(new BookService(new InMemoryBookRepository()));
        Book request = new Book(0L, "Adam Mickiewicz", "Pan Tadeusz", 328,
                "9788370819410", BookStatus.UNAVAILABLE, 1834, "PWN", new BookInventory(0));

        HttpResponse<Book> response = controller.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
    }

    private static final class InMemoryBookRepository implements BookRepository {

        @Override
        public List<String> loadBookTitles() {
            return List.of();
        }

        @Override
        public Optional<Book> findByTitle(String title) {
            return Optional.empty();
        }

        @Override
        public List<Book> findAll() {
            return List.of();
        }

        @Override
        public Optional<Book> findById(long id) {
            return Optional.empty();
        }

        @Override
        public Book save(Book book) {
            return new Book(1L, book.author(), book.title(), book.pages(), book.isbn(), book.status(),
                    book.publishYear(), book.publisher(), book.inventory());
        }

        @Override
        public void update(Book book) {
        }

        @Override
        public void deleteById(long id) {
        }
    }
}
