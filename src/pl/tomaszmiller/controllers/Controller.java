package pl.tomaszmiller.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import pl.tomaszmiller.MySqlConnector;
import pl.tomaszmiller.Utils;

import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

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

    public void openDialog(MouseEvent event) throws IOException { //wyjątek do FXMLLoader
        if (!isLoginFormValid()) {
            return;
        }

        System.out.println("Login: " + userEmail.getText() + ", Password: " + Utils.hashPassword(userPassword.getText()));
        Statement statement = MySqlConnector.getInstance().getNewStatement();
        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `users` WHERE email='" + userEmail.getText() + "' LIMIT 1");
            int counter = 0;
                while (resultSet.next()) {
                    String passwordFromDatabase = resultSet.getString("password");
                    int userRank = resultSet.getInt("user_rank");
                    if (passwordFromDatabase.equals(Utils.hashPassword(userPassword.getText()))) {
                        String source = (userRank == 1 ? "../views/adminView.fxml" : "../views/userView.fxml");
                        Parent nextView = FXMLLoader.load(getClass().getResource(source)); //tworzenie nowego widoku po zalogowaniu
                        Scene scene = new Scene(nextView);
                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); //każy widżet musi być przypisany do jakiejś sceny
                        stage.hide();
                        stage.setScene(scene);
                        stage.show();
                        Utils.confirmDialog("Logowanie", "Zalogowałeś się poprawnie!");
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
            statement1.setString(3, Utils.hashPassword(getPassword(password)));
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
