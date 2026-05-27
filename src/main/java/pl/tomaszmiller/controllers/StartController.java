package pl.tomaszmiller.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.database.MySqlConnector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class StartController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartController.class);

    @FXML
    private TextField userEmail;

    @FXML
    private PasswordField userPassword;

    @FXML
    private TextField firstName;

    @FXML
    private TextField lastName;

    @FXML
    private TextField phoneNumber;

    @FXML
    private TextField email;

    @FXML
    private TextField emailConfirmed;

    @FXML
    private PasswordField password;

    @FXML
    private PasswordField passwordConfirmed;

    @FXML
    private void login(ActionEvent event) {
        if (!isLoginFormValid()) {
            return;
        }

        Integer userRank = authenticateUser(getValue(userEmail), getPassword(userPassword));
        if (userRank == null) {
            return;
        }

        String source = userRank == 1
                ? "/pl/tomaszmiller/views/adminView.fxml"
                : "/pl/tomaszmiller/views/userView.fxml";
        try {
            switchScene(event, source);
            Utils.confirmDialog("Logowanie", "Zalogowałeś się poprawnie!");
        } catch (IOException exception) {
            LOGGER.error("Unable to open the dashboard view {}.", source, exception);
            Utils.openDialog("Logowanie", "Nie udało się otworzyć panelu użytkownika.");
        }
    }

    @FXML
    private void createAccount() {
        if (!isRegisterFormValid()) {
            return;
        }

        if (emailExists(getValue(email))) {
            Utils.openDialog("Tworzenie nowego konta", "Użytkownik o podanym adresie e-mail już istnieje!");
            return;
        }

        if (insertRegisterSqlData()) {
            Utils.confirmDialog("Tworzenie nowego konta", "Twoje konto zostało pomyślnie utworzone!");
            clearRegisterForm();
        }
    }

    private boolean isLoginFormValid() {
        String emailValue = getValue(userEmail);
        String passwordValue = getPassword(userPassword);
        if (!Utils.isValidEmail(emailValue) || passwordValue.length() < 8) {
            Utils.openDialog("Logowanie", "Adres e-mail lub hasło jest niepoprawne!");
            return false;
        }
        return true;
    }

    private boolean isRegisterFormValid() {
        String firstNameValue = getValue(firstName);
        String lastNameValue = getValue(lastName);
        String phoneNumberValue = getValue(phoneNumber);
        String emailValue = getValue(email);
        String emailConfirmedValue = getValue(emailConfirmed);
        String passwordValue = getPassword(password);
        String passwordConfirmedValue = getPassword(passwordConfirmed);

        boolean valid = Utils.isValidName(firstNameValue)
                && Utils.isValidName(lastNameValue)
                && Utils.isValidPhoneNumber(phoneNumberValue)
                && Utils.isValidEmail(emailValue)
                && emailValue.equalsIgnoreCase(emailConfirmedValue)
                && passwordValue.length() >= 8
                && passwordValue.equals(passwordConfirmedValue);

        if (!valid) {
            Utils.openDialog("Tworzenie nowego konta", "Wpisane dane są niepoprawne! Spróbuj ponownie!");
        }
        return valid;
    }

    private Integer authenticateUser(String emailValue, String passwordValue) {
        String sql = "SELECT password, user_rank FROM users WHERE email = ? LIMIT 1";
        try (Connection connection = MySqlConnector.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, emailValue);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    Utils.openDialog("Logowanie", "Użytkownik o podanym adresie e-mail nie istnieje!");
                    return null;
                }

                String passwordFromDatabase = resultSet.getString("password");
                if (!Utils.verifyPassword(passwordValue, passwordFromDatabase)) {
                    Utils.openDialog("Logowanie", "Podane hasło jest niepoprawne!");
                    return null;
                }
                return resultSet.getInt("user_rank");
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to authenticate user {}.", emailValue, exception);
            Utils.openDialog("Logowanie", "Wystąpił problem podczas logowania. Spróbuj ponownie później.");
            return null;
        }
    }

    private boolean emailExists(String emailValue) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection connection = MySqlConnector.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, emailValue);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            LOGGER.error("Unable to verify whether e-mail {} already exists.", emailValue, exception);
            Utils.openDialog("Tworzenie nowego konta", "Nie udało się zweryfikować adresu e-mail.");
            return true;
        }
    }

    private boolean insertRegisterSqlData() {
        String sql = "INSERT INTO users (f_name, l_name, password, email, phone_number) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = MySqlConnector.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, getValue(firstName));
            statement.setString(2, getValue(lastName));
            statement.setString(3, Utils.hashPassword(getPassword(password)));
            statement.setString(4, getValue(email));
            statement.setString(5, getValue(phoneNumber));
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            LOGGER.error("Unable to create a new account for {}.", getValue(email), exception);
            Utils.openDialog("Tworzenie nowego konta", "Nie udało się utworzyć nowego konta.");
            return false;
        }
    }

    private void switchScene(ActionEvent event, String source) throws IOException {
        Parent nextView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(source), "View is missing: " + source));
        Scene scene = new Scene(nextView);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.hide();
        stage.setScene(scene);
        stage.show();
    }

    private void clearRegisterForm() {
        firstName.clear();
        lastName.clear();
        phoneNumber.clear();
        email.clear();
        emailConfirmed.clear();
        password.clear();
        passwordConfirmed.clear();
    }

    private String getValue(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String getPassword(PasswordField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
