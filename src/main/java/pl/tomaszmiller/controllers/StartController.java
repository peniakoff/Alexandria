package pl.tomaszmiller.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.Utils;
import pl.tomaszmiller.config.AppConfig;
import pl.tomaszmiller.i18n.I18n;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.model.UserRole;
import pl.tomaszmiller.service.AuthService;
import pl.tomaszmiller.service.UserService;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class StartController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartController.class);

    private final AuthService authService = AppConfig.getInstance().getAuthService();
    private final UserService userService = AppConfig.getInstance().getUserService();

    @FXML
    private ComboBox<String> languageCombo;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (languageCombo != null) {
            languageCombo.setItems(FXCollections.observableArrayList(I18n.get("lang.en"), I18n.get("lang.pl")));
            languageCombo.setValue(I18n.getCurrentLocale().getLanguage().equals("pl")
                    ? I18n.get("lang.pl")
                    : I18n.get("lang.en"));
        }
    }

    @FXML
    private void onLanguageChange() {
        if (languageCombo == null) {
            return;
        }
        String selected = languageCombo.getValue();
        if (I18n.get("lang.pl").equals(selected)) {
            I18n.switchToPolish();
        } else {
            I18n.switchToEnglish();
        }
        // Reload the login view to reflect the new language
        try {
            Parent loginView = Utils.loadView("/pl/tomaszmiller/views/loginView.fxml");
            Stage stage = (Stage) languageCombo.getScene().getWindow();
            stage.setTitle(I18n.get("app.title"));
            stage.setScene(new Scene(loginView, 800, 600));
            stage.show();
        } catch (IOException e) {
            LOGGER.error("Failed to reload login view after language change.", e);
        }
    }

    @FXML
    private void login(ActionEvent event) {
        if (!isLoginFormValid()) {
            return;
        }

        Optional<User> userOpt = authService.login(getValue(userEmail), getPassword(userPassword));
        if (userOpt.isEmpty()) {
            Utils.openDialog(I18n.get("login.tab"), I18n.get("login.error"));
            return;
        }

        User user = userOpt.get();
        String source = user.role() == UserRole.ADMIN
                ? "/pl/tomaszmiller/views/adminView.fxml"
                : "/pl/tomaszmiller/views/userView.fxml";
        try {
            switchScene(event, source);
            Utils.confirmDialog(I18n.get("login.tab"), I18n.get("login.success", user.fullName()));
        } catch (IOException exception) {
            LOGGER.error("Unable to open the dashboard view {}.", source, exception);
            Utils.openDialog(I18n.get("login.tab"), I18n.get("login.failed"));
        }
    }

    @FXML
    private void createAccount() {
        if (!isRegisterFormValid()) {
            return;
        }

        if (userService.emailExists(getValue(email))) {
            Utils.openDialog(I18n.get("register.title"), I18n.get("register.exists"));
            return;
        }

        User newUser = new User(0L, getValue(firstName), getValue(lastName),
                getValue(email), getValue(phoneNumber), UserRole.USER);
        Optional<User> saved = userService.register(newUser, getPassword(password));
        if (saved.isPresent()) {
            Utils.confirmDialog(I18n.get("register.title"), I18n.get("register.success"));
            clearRegisterForm();
        } else {
            Utils.openDialog(I18n.get("register.title"), I18n.get("register.failed"));
        }
    }

    private boolean isLoginFormValid() {
        String emailValue = getValue(userEmail);
        String passwordValue = getPassword(userPassword);
        if (!Utils.isValidEmail(emailValue) || passwordValue.length() < 8) {
            Utils.openDialog(I18n.get("login.tab"), I18n.get("login.error"));
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
            Utils.openDialog(I18n.get("register.title"), I18n.get("register.invalid"));
        }
        return valid;
    }

    private void switchScene(ActionEvent event, String source) throws IOException {
        Parent nextView = Utils.loadView(source);
        Scene scene = new Scene(nextView);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.hide();
        stage.setTitle(I18n.get("app.title"));
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
