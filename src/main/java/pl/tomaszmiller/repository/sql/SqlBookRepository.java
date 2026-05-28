package pl.tomaszmiller.repository.sql;

import pl.tomaszmiller.database.DatabaseConnector;
import pl.tomaszmiller.model.Book;
import pl.tomaszmiller.model.BookStatus;
import pl.tomaszmiller.repository.port.BookRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-backed implementation of {@link BookRepository}.
 * Compatible with MySQL and SQLite dialects.
 */
public class SqlBookRepository implements BookRepository {

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
        String sql = "SELECT id, author, title, pages, isbn, status, publish_year, publisher FROM books WHERE title = ? LIMIT 1";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
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
             PreparedStatement ps = c.prepareStatement("SELECT id, author, title, pages, isbn, status, publish_year, publisher FROM books ORDER BY title");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                books.add(mapRow(rs));
            }
        }
        return books;
    }

    @Override
    public Optional<Book> findById(long id) throws SQLException {
        String sql = "SELECT id, author, title, pages, isbn, status, publish_year, publisher FROM books WHERE id = ?";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Book save(Book book) throws SQLException {
        String sql = "INSERT INTO books (author, title, pages, isbn, status, publish_year, publisher) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.author());
            ps.setString(2, book.title());
            ps.setInt(3, book.pages());
            ps.setString(4, book.isbn());
            ps.setString(5, book.status().name());
            ps.setInt(6, book.publishYear());
            ps.setString(7, book.publisher());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Book(keys.getLong(1), book.author(), book.title(), book.pages(),
                            book.isbn(), book.status(), book.publishYear(), book.publisher());
                }
            }
        }
        return book;
    }

    @Override
    public void update(Book book) throws SQLException {
        String sql = "UPDATE books SET author = ?, title = ?, pages = ?, isbn = ?, status = ?, publish_year = ?, publisher = ? WHERE id = ?";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, book.author());
            ps.setString(2, book.title());
            ps.setInt(3, book.pages());
            ps.setString(4, book.isbn());
            ps.setString(5, book.status().name());
            ps.setInt(6, book.publishYear());
            ps.setString(7, book.publisher());
            ps.setLong(8, book.id());
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

    private Book mapRow(ResultSet rs) throws SQLException {
        BookStatus status;
        try {
            status = BookStatus.valueOf(rs.getString("status"));
        } catch (IllegalArgumentException | NullPointerException e) {
            status = BookStatus.AVAILABLE;
        }
        int publishYear = 0;
        String publisher = null;
        try {
            publishYear = rs.getInt("publish_year");
            publisher = rs.getString("publisher");
        } catch (SQLException ignored) {
            // columns may not exist in older schemas
        }
        return new Book(
                rs.getLong("id"),
                rs.getString("author"),
                rs.getString("title"),
                rs.getInt("pages"),
                rs.getString("isbn"),
                status,
                publishYear,
                publisher
        );
    }
}
