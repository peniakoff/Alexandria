package pl.tomaszmiller;

import javafx.scene.control.Alert;

/**
 * Created by Peniakoff on 26.04.2017.
 */
public class Utils {

    public static void openDialog(String title, String massage) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(massage);
        alert.showAndWait();

    }

    public static void confirmDialog(String title, String massage) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(massage);
        alert.showAndWait();

    }

}
