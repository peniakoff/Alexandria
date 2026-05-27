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
import pl.tomaszmiller.config.AppConfig;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.model.UserRole;
import pl.tomaszmiller.service.AuthService;
import pl.tomaszmiller.service.UserService;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class StartController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartController.class);

    private final AuthService authService = AppConfig.getInstance().getAuthService();
    private final UserService userService = AppConfig.getInstance().getUserService();

    @FXML private TextField userEmail;
    @FXML private PasswordField userPassword;
    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private TextField phoneNumber;
    @FXML private TextField email;
    @FXML private TextField emailConfirmed;
    @FXML private PasswordField password;
    @FXML private PasswordField passwordConfirmed;

    @FXML
    private void login(ActionEvent event) {
        if (!isLoginFormValid()) {
            return;
        }

        Optional<User> userOpt = authService.login(getValue(userEmail), getPassword(userPassword));
        if (userOpt.isEmpty()) {
            Utils.openDialog("Login", "Invalid e-mail address or password.");
            return;
        }

        User user = userOpt.get();
        String source = user.role() == UserRole.ADMIN
                ? "/pl/tomaszmiller/views/adminView.fxml"
                : "/pl/tomaszmiller/views/userView.fxml";
        try {
            switchScene(event, source);
            Utils.confirmDialog("Login", "Logged in successfully as " + user.fullName() + ".");
        } catch (IOException exception) {
            LOGGER.error("Unable to open the dashboard view {}.", source, exception);
            Utils.openDialog("Login", "Failed to open user panel.");
        }
    }

    @FXML
    private void createAccount() {
        if (!isRegisterFormValid()) {
            return;
        }

        if (userService.emailExists(getValue(email))) {
            Utils.openDialog("Create new account", "A user with this e-mail address already exists!");
            return;
        }

        User newUser = new User(0L, getValue(firstName), getValue(lastName),
                getValue(email), getValue(phoneNumber), UserRole.USER);
        Optional<User> saved = userService.register(newUser, getPassword(password));
        if (saved.isPresent()) {
            Utils.confirmDialog("Create new account", "Your account has been successfully created!");
            clearRegisterForm();
        } else {
            Utils.openDialog("Create new account", "Failed to create new account.");
        }
    }

    private boolean isLoginFormValid() {
        String emailValue = getValue(userEmail);
        String passwordValue = getPassword(userPassword);
        if (!Utils.isValidEmail(emailValue) || passwordValue.length() < 8) {
            Utils.openDialog("Login", "E-mail address or password is incorrect!");
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
            Utils.openDialog("Create new account", "The data entered is incorrect! Check the form and try again.");
        }
        return valid;
    }

    private void switchScene(ActionEvent event, String source) throws IOException {
        Parent nextView = FXMLLoader.load(Objects.requireNonNull(
                getClass().getResource(source), "View is missing: " + source));
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
