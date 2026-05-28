package pl.tomaszmiller.repository.sql;

import pl.tomaszmiller.database.DatabaseConnector;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookInventory;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.repository.port.BookRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-backed implementation of {@link BookRepository}.
 * Compatible with MySQL and SQLite dialects.
 */
public class SqlBookRepository implements BookRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, author, title, pages, isbn, status, publish_year, publisher,
                   active_copies, available_copies, archived_copies,
                   removed_damaged_copies, removed_stolen_copies
            FROM books
            """;

    private final DatabaseConnector connector;

    public SqlBookRepository(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public List<String> loadBookTitles() throws SQLException {
        List<String> titles = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT title FROM books ORDER BY title");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                titles.add(rs.getString("title"));
            }
        }
        return titles;
    }

    @Override
    public Optional<Book> findByTitle(String title) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_COLUMNS + " WHERE title = ? LIMIT 1")) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Book> findAll() throws SQLException {
        List<Book> books = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_COLUMNS + " ORDER BY title");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                books.add(mapRow(rs));
            }
        }
        return books;
    }

    @Override
    public Optional<Book> findById(long id) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_COLUMNS + " WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Book save(Book book) throws SQLException {
        String sql = """
                INSERT INTO books (
                    author, title, pages, isbn, status, publish_year, publisher,
                    active_copies, available_copies, archived_copies,
                    removed_damaged_copies, removed_stolen_copies
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindBook(ps, book, false);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Book(keys.getLong(1), book.author(), book.title(), book.pages(),
                            book.isbn(), book.status(), book.publishYear(), book.publisher(), book.inventory());
                }
            }
        }
        return book;
    }

    @Override
    public void update(Book book) throws SQLException {
        String sql = """
                UPDATE books
                SET author = ?, title = ?, pages = ?, isbn = ?, status = ?, publish_year = ?, publisher = ?,
                    active_copies = ?, available_copies = ?, archived_copies = ?,
                    removed_damaged_copies = ?, removed_stolen_copies = ?
                WHERE id = ?
                """;
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindBook(ps, book, true);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteById(long id) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private void bindBook(PreparedStatement ps, Book book, boolean includeId) throws SQLException {
        ps.setString(1, book.author());
        ps.setString(2, book.title());
        ps.setInt(3, book.pages());
        ps.setString(4, book.isbn());
        ps.setString(5, book.status().name());
        ps.setInt(6, book.publishYear());
        ps.setString(7, book.publisher());
        ps.setInt(8, book.inventory().activeCopies());
        ps.setInt(9, book.inventory().availableCopies());
        ps.setInt(10, book.inventory().archivedCopies());
        ps.setInt(11, book.inventory().removedDamagedCopies());
        ps.setInt(12, book.inventory().removedStolenCopies());
        if (includeId) {
            ps.setLong(13, book.id());
        }
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        BookStatus status;
        try {
            status = BookStatus.valueOf(rs.getString("status"));
        } catch (IllegalArgumentException | NullPointerException e) {
            status = BookStatus.AVAILABLE;
        }
        BookInventory inventory = new BookInventory(
                rs.getInt("active_copies"),
                rs.getInt("available_copies"),
                rs.getInt("archived_copies"),
                rs.getInt("removed_damaged_copies"),
                rs.getInt("removed_stolen_copies")
        );
        return new Book(
                rs.getLong("id"),
                rs.getString("author"),
                rs.getString("title"),
                rs.getInt("pages"),
                rs.getString("isbn"),
                status,
                rs.getInt("publish_year"),
                rs.getString("publisher"),
                inventory
        );
    }
}
