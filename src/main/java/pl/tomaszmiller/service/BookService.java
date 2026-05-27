package pl.tomaszmiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.repository.port.BookRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Application-level operations on the book catalog.
 */
public class BookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<String> getAllTitles() {
        try {
            return bookRepository.loadBookTitles();
        } catch (Exception e) {
            LOGGER.error("Unable to load book titles.", e);
            return Collections.emptyList();
        }
    }

    public Optional<Book> findByTitle(String title) {
        try {
            return bookRepository.findByTitle(title);
        } catch (Exception e) {
            LOGGER.error("Unable to find book by title '{}'.", title, e);
            return Optional.empty();
        }
    }

    public List<Book> findAll() {
        try {
            return bookRepository.findAll();
        } catch (Exception e) {
            LOGGER.error("Unable to load all books.", e);
            return Collections.emptyList();
        }
    }

    public Optional<Book> findById(long id) {
        try {
            return bookRepository.findById(id);
        } catch (Exception e) {
            LOGGER.error("Unable to find book by id {}.", id, e);
            return Optional.empty();
        }
    }

    public Optional<Book> addBook(Book book) {
        try {
            return Optional.of(bookRepository.save(book));
        } catch (Exception e) {
            LOGGER.error("Unable to save book '{}'.", book.title(), e);
            return Optional.empty();
        }
    }

    public boolean updateBook(Book book) {
        try {
            bookRepository.update(book);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to update book id={}.", book.id(), e);
            return false;
        }
    }

    public boolean deleteBook(long id) {
        try {
            bookRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to delete book id={}.", id, e);
            return false;
        }
    }
}
