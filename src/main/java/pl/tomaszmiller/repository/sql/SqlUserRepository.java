package pl.tomaszmiller.repository.sql;

import pl.tomaszmiller.database.DatabaseConnector;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.model.UserRole;
import pl.tomaszmiller.repository.port.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-backed implementation of {@link UserRepository}.
 */
public class SqlUserRepository implements UserRepository {

    private final DatabaseConnector connector;

    public SqlUserRepository(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, f_name, l_name, email, phone_number, user_rank FROM users ORDER BY l_name, f_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    @Override
    public Optional<User> findById(long id) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, f_name, l_name, email, phone_number, user_rank FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<User> findByEmail(String email) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, f_name, l_name, email, phone_number, user_rank FROM users WHERE email = ? LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<String> findPasswordHashByEmail(String email) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT password FROM users WHERE email = ? LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("password")) : Optional.empty();
            }
        }
    }

    @Override
    public User save(User user, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (f_name, l_name, email, password, phone_number, user_rank) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.firstName());
            ps.setString(2, user.lastName());
            ps.setString(3, user.email());
            ps.setString(4, passwordHash);
            ps.setString(5, user.phoneNumber());
            ps.setInt(6, user.role().getDbValue());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new User(keys.getLong(1), user.firstName(), user.lastName(),
                            user.email(), user.phoneNumber(), user.role());
                }
            }
        }
        return user;
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET f_name = ?, l_name = ?, phone_number = ? WHERE id = ?";
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.firstName());
            ps.setString(2, user.lastName());
            ps.setString(3, user.phoneNumber());
            ps.setLong(4, user.id());
            ps.executeUpdate();
        }
    }

    @Override
    public void updatePassword(long userId, String newPasswordHash) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {
            ps.setString(1, newPasswordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteById(long id) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean existsByEmail(String email) throws SQLException {
        try (Connection c = connector.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE email = ? LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("f_name"),
                rs.getString("l_name"),
                rs.getString("email"),
                rs.getString("phone_number"),
                UserRole.fromDbValue(rs.getInt("user_rank"))
        );
    }
}
