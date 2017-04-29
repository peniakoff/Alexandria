package pl.tomaszmiller;

import javafx.scene.control.Alert;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public static String hashPassword(String password) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashArray = digest.digest(password.getBytes("UTF-8"));
            return DatatypeConverter.printHexBinary(hashArray);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;

    }

}
