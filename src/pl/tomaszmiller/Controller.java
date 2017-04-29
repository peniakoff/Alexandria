package pl.tomaszmiller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

public class Controller implements Initializable {

    @FXML
    TextField userEmail;

    @FXML
    PasswordField userPassword;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) { // metoda startowa, wykonje się przed wykonaniem aplikacji

    }

}
