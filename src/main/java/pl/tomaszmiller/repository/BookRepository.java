package pl.tomaszmiller.repository;

import pl.tomaszmiller.database.MySqlConnector;
import pl.tomaszmiller.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookRepository {

    public List<String> loadBookTitles() throws SQLException {
        String sql = "SELECT title FROM books ORDER BY title";
        List<String> titles = new ArrayList<>();
        try (Connection connection = MySqlConnector.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                titles.add(resultSet.getString("title"));
            }
        }
        return titles;
    }

    public Optional<Book> findByTitle(String title) throws SQLException {
        String sql = "SELECT author, title, pages FROM books WHERE title = ? LIMIT 1";
        try (Connection connection = MySqlConnector.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Book(
                        resultSet.getString("author"),
                        resultSet.getString("title"),
                        resultSet.getInt("pages")
                ));
            }
        }
    }
}
