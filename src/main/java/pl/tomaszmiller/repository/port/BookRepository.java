package pl.tomaszmiller.repository.port;

import pl.tomaszmiller.model.Book;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for all book catalog operations.
 * Implementations may use SQL databases, SQLite files, or remote REST APIs.
 */
public interface BookRepository {
    /** Returns all book titles sorted alphabetically. */
    List<String> loadBookTitles() throws Exception;

    /** Finds a book by its exact title. */
    Optional<Book> findByTitle(String title) throws Exception;

    /** Returns all books in the catalog. */
    List<Book> findAll() throws Exception;

    /** Finds a book by its primary key. */
    Optional<Book> findById(long id) throws Exception;

    /** Persists a new book; returns the saved entity with generated id. */
    Book save(Book book) throws Exception;

    /** Updates an existing book. */
    void update(Book book) throws Exception;

    /** Removes a book by its primary key. */
    void deleteById(long id) throws Exception;
}
