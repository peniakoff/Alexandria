package pl.tomaszmiller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;

public class Controller implements Initializable {

    @FXML
    TextField userEmail;

    @FXML
    PasswordField userPassword;

    @FXML
    TextField firstName;

    @FXML
    TextField lastName;

    @FXML
    TextField phoneNumber;

    @FXML
    TextField email;

    @FXML
    TextField emailConfirmed;

    @FXML
    PasswordField password;

    @FXML
    PasswordField passwordConfirmed;


//    public void openDialog() {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Akademia Kodu");
//        alert.setHeaderText(null);
//        alert.setContentText("Siema, ziomeczki!");
//
//        ButtonType buttonCancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
//        ButtonType buttonDrugs = new ButtonType("Drugs are OK, bro!");
//
//        alert.getButtonTypes().setAll(buttonCancel, buttonDrugs);
//
//        Optional<ButtonType> result = alert.showAndWait();
//        if (result.get() == buttonDrugs) {
//            System.out.println("Drugs, Love and Rock&Roll!");
//        } else if (result.get() == buttonCancel) {
//            System.out.println("Ktoś to, kurde mać, zrąbał.");
//        }
//    }

    private boolean isLoginFormValid() {

        if (userEmail.getText().trim().length() < 5 || userPassword.getText().trim().length() < 4) {
            Utils.openDialog("Logowanie", "Adres e-mail lub hasło jest niepoprawne!");
            return false;
        }
        return true;

    }

    private boolean isRagisterFormValid() {
        if (getValue(firstName).length() < 3
                || getValue(lastName).length() < 3
                || getValue(phoneNumber).length() < 9
                || getPassword(password).length() < 6
                || getValue(email).length() < 6
                || !getValue(email).equals(getValue(emailConfirmed))
                || !getPassword(password).equals(getPassword(passwordConfirmed))) {
            Utils.openDialog("Tworzenie nowego konta", "Wpisane dane są niepoprawne! Spróbuj ponownie!");
            return false;
        }
        return true;

    }

    public void openDialog() {
        if (!isLoginFormValid()) {
            return;
        }

        System.out.println("Login: " + userEmail.getText() + ", Password: " + userPassword.getText());
        Statement statement = MySqlConnector.getInstance().getNewStatement();
        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `users` WHERE email='" + userEmail.getText() + "' LIMIT 1");
            int counter = 0;
                while (resultSet.next()) {
                    String passwordFromDatabase = resultSet.getString("password");
                    if (passwordFromDatabase.equals(userPassword.getText())) {
                        Utils.openDialog("Logowanie", "Zalogowałeś się poprawnie!");
                    } else {
                        Utils.openDialog("Logowanie", "Podane hasło jest niepoprawne!");
                    }
                    counter ++;
                }
            if (counter == 0) {
                Utils.openDialog("Logowanie", "Użytkownik o podanym mailu nie istnieje!");
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void createAccount() {

        if (!isRagisterFormValid()) {
            return;
        }

        System.out.println("First name: " + getValue(firstName) + ", last name: " + getValue(lastName) + ", phone number: " + getValue(phoneNumber) + ", email: " + getValue(email) + ", password: " + getPassword(password));
        Statement statement = MySqlConnector.getInstance().getNewStatement();
        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `users` WHERE email='" + getValue(email) + "' LIMIT 1");
            int counter = 0;
            while (resultSet.next()) {
                counter ++;
            }
            statement.close();
            if (counter > 0) {
                Utils.openDialog("Tworzenie nowego konta", "Użytkownik o podanym adresie e-mail już istnieje!");
            } else {
                insertSqlData();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void insertSqlData() {
        try {
            String sql = "INSERT INTO `users`(`f_name`, `l_name`, `password`, `email`, `phone_number`) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement statement1 = MySqlConnector.getInstance().getConnection().prepareStatement(sql);
            statement1.setString(1, getValue(firstName));
            statement1.setString(2, getValue(lastName));
            statement1.setString(3, getPassword(password));
            statement1.setString(4, getValue(email));
            statement1.setString(5, getValue(phoneNumber));
            statement1.execute();
            statement1.close();
            Utils.confirmDialog("Tworzenie nowego konta", "Twoje konto zostało pomyślnie utworzone!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getValue(TextField var) {
        return var.getText().trim();
    }

    public String getPassword(PasswordField var) {
        return var.getText().trim();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) { // metoda startowa, wykonje się przed wykonaniem aplikacji

    }

}
