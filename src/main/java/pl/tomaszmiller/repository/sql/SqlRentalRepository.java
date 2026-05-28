package pl.tomaszmiller.repository.sql;

import pl.tomaszmiller.database.DatabaseConnector;
import pl.tomaszmiller.model.Rental;
import pl.tomaszmiller.model.RentalStatus;
import pl.tomaszmiller.repository.port.RentalRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-backed implementation of {@link RentalRepository}.
 */
public class SqlRentalRepository implements RentalRepository {

    private final DatabaseConnector connector;

    public SqlRentalRepository(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public List<Rental> findAll() throws SQLException {
        List<Rental> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, user_id, book_id, borrow_date, due_date, return_date, status FROM rentals ORDER BY borrow_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public Optional<Rental> findById(long id) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, user_id, book_id, borrow_date, due_date, return_date, status FROM rentals WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Rental> findByUserId(long userId) throws SQLException {
        List<Rental> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, user_id, book_id, borrow_date, due_date, return_date, status FROM rentals WHERE user_id = ? ORDER BY borrow_date DESC")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Rental> findByBookId(long bookId) throws SQLException {
        List<Rental> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, user_id, book_id, borrow_date, due_date, return_date, status FROM rentals WHERE book_id = ? ORDER BY borrow_date DESC")) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Rental> findByStatus(RentalStatus status) throws SQLException {
        List<Rental> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, user_id, book_id, borrow_date, due_date, return_date, status FROM rentals WHERE status = ?")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public Rental save(Rental rental) throws SQLException {
        String sql = "INSERT INTO rentals (user_id, book_id, borrow_date, due_date, return_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rental.userId());
            ps.setLong(2, rental.bookId());
            ps.setDate(3, Date.valueOf(rental.borrowDate()));
            ps.setDate(4, Date.valueOf(rental.dueDate()));
            ps.setDate(5, rental.returnDate() != null ? Date.valueOf(rental.returnDate()) : null);
            ps.setString(6, rental.status().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Rental(keys.getLong(1), rental.userId(), rental.bookId(),
                            rental.borrowDate(), rental.dueDate(), rental.returnDate(), rental.status());
                }
            }
        }
        return rental;
    }

    @Override
    public void update(Rental rental) throws SQLException {
        String sql = "UPDATE rentals SET due_date = ?, return_date = ?, status = ? WHERE id = ?";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(rental.dueDate()));
            ps.setDate(2, rental.returnDate() != null ? Date.valueOf(rental.returnDate()) : null);
            ps.setString(3, rental.status().name());
            ps.setLong(4, rental.id());
            ps.executeUpdate();
        }
    }

    private Rental mapRow(ResultSet rs) throws SQLException {
        Date returnDateSql = rs.getDate("return_date");
        LocalDate returnDate = returnDateSql != null ? returnDateSql.toLocalDate() : null;
        RentalStatus status;
        try {
            status = RentalStatus.valueOf(rs.getString("status"));
        } catch (IllegalArgumentException | NullPointerException e) {
            status = RentalStatus.ACTIVE;
        }
        return new Rental(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("book_id"),
                rs.getDate("borrow_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                returnDate,
                status
        );
    }
}
