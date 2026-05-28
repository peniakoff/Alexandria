package pl.tomaszmiller.repository.sql;

import pl.tomaszmiller.database.DatabaseConnector;
import pl.tomaszmiller.model.Reservation;
import pl.tomaszmiller.model.RequestStatus;
import pl.tomaszmiller.repository.port.ReservationRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-backed implementation of {@link ReservationRepository}.
 */
public class SqlReservationRepository implements ReservationRepository {

    private final DatabaseConnector connector;

    public SqlReservationRepository(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public List<Reservation> findAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, user_id, book_id, status, request_date FROM reservations ORDER BY request_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public List<Reservation> findByStatus(RequestStatus status) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, user_id, book_id, status, request_date FROM reservations WHERE status = ?")) {
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
    public List<Reservation> findByUserId(long userId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, user_id, book_id, status, request_date FROM reservations WHERE user_id = ?")) {
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
    public List<Reservation> findByBookId(long bookId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, user_id, book_id, status, request_date FROM reservations WHERE book_id = ?")) {
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
    public Optional<Reservation> findById(long id) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, user_id, book_id, status, request_date FROM reservations WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Reservation save(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservations (user_id, book_id, status, request_date) VALUES (?, ?, ?, ?)";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, reservation.userId());
            ps.setLong(2, reservation.bookId());
            ps.setString(3, reservation.status().name());
            ps.setDate(4, Date.valueOf(reservation.requestDate()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Reservation(keys.getLong(1), reservation.userId(),
                            reservation.bookId(), reservation.status(), reservation.requestDate());
                }
            }
        }
        return reservation;
    }

    @Override
    public void updateStatus(long id, RequestStatus status) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE reservations SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("book_id"),
                RequestStatus.fromString(rs.getString("status")),
                rs.getDate("request_date").toLocalDate()
        );
    }
}
