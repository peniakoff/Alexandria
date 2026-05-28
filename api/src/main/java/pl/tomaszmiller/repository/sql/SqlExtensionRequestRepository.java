package pl.tomaszmiller.repository.sql;

import pl.tomaszmiller.database.DatabaseConnector;
import pl.tomaszmiller.model.ExtensionRequest;
import pl.tomaszmiller.model.RequestStatus;
import pl.tomaszmiller.repository.port.ExtensionRequestRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-backed implementation of {@link ExtensionRequestRepository}.
 */
public class SqlExtensionRequestRepository implements ExtensionRequestRepository {

    private final DatabaseConnector connector;

    public SqlExtensionRequestRepository(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public List<ExtensionRequest> findAll() throws SQLException {
        List<ExtensionRequest> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, rental_id, user_id, status, request_date FROM extension_requests ORDER BY request_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public List<ExtensionRequest> findByStatus(RequestStatus status) throws SQLException {
        List<ExtensionRequest> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, rental_id, user_id, status, request_date FROM extension_requests WHERE status = ?")) {
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
    public List<ExtensionRequest> findByUserId(long userId) throws SQLException {
        List<ExtensionRequest> list = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, rental_id, user_id, status, request_date FROM extension_requests WHERE user_id = ?")) {
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
    public Optional<ExtensionRequest> findById(long id) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, rental_id, user_id, status, request_date FROM extension_requests WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public ExtensionRequest save(ExtensionRequest request) throws SQLException {
        String sql = "INSERT INTO extension_requests (rental_id, user_id, status, request_date) VALUES (?, ?, ?, ?)";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, request.rentalId());
            ps.setLong(2, request.userId());
            ps.setString(3, request.status().name());
            ps.setDate(4, Date.valueOf(request.requestDate()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new ExtensionRequest(keys.getLong(1), request.rentalId(),
                            request.userId(), request.status(), request.requestDate());
                }
            }
        }
        return request;
    }

    @Override
    public void updateStatus(long id, RequestStatus status) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE extension_requests SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private ExtensionRequest mapRow(ResultSet rs) throws SQLException {
        return new ExtensionRequest(
                rs.getLong("id"),
                rs.getLong("rental_id"),
                rs.getLong("user_id"),
                RequestStatus.fromString(rs.getString("status")),
                rs.getDate("request_date").toLocalDate()
        );
    }
}
