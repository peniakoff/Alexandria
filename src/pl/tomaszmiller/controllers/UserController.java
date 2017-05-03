package pl.tomaszmiller.controllers;

import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import pl.tomaszmiller.MySqlConnector;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

/**
 * Created by Peniakoff on 29.04.2017.
 */
public class UserController implements Initializable {

    @FXML
    ImageView logo;

    @FXML
    JFXListView theList;

    @FXML
    JFXTextField bookAuthor;

    @FXML
    JFXTextField bookTitle;

    @FXML
    JFXTextField pages;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

//        RotateTransition transition = new RotateTransition();
//        transition.setDuration(Duration.seconds(5));
////        transition.setByAngle(360);
//        transition.setCycleCount(5);
//        transition.setAutoReverse(false);
//        transition.setToAngle(360);
////        transition.setFromAngle(-60);
//        transition.setNode(logo);
//        transition.play();

//        ScaleTransition scale = new ScaleTransition();
//        scale.setNode(logo);
//        scale.setByX(.5);
//        scale.setByY(.5);
//        scale.setDuration(Duration.seconds(5));
//        scale.setCycleCount(10);
//        scale.play();

//        FadeTransition fadeTransition = new FadeTransition();
//        fadeTransition.setNode(logo);
//        fadeTransition.setDuration(Duration.seconds(5));
//        fadeTransition.setFromValue(1.0);
//        fadeTransition.setToValue(0.0);
//        fadeTransition.setAutoReverse(true);
//        fadeTransition.setCycleCount(2);
//        fadeTransition.setOnFinished(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                System.out.println("Koniec animacji.");
//            }
//        });
//        fadeTransition.play();

//    ObservableList<String> items = FXCollections.observableArrayList("Tomek", "Roman", "Pajac");
        ObservableList<String> items = loadBook();
        theList.setItems(items);
        theList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String[] bookData = loadBookData((String)theList.getSelectionModel().getSelectedItem());
                bookAuthor.setText(bookData[0]);
                bookTitle.setText(bookData[1]);
                pages.setText(bookData[2]);
            }
        });

    }

    private String[] loadBookData(String bookTitle) {
        Statement statement = MySqlConnector.getInstance().getNewStatement();
        String[] dataArray = new String[3];
        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `books` WHERE `title`='" + bookTitle + "' LIMIT 1");
            while (resultSet.next()) {
                dataArray[0] = resultSet.getString("author");
                dataArray[1] = resultSet.getString("title");
                dataArray[2] = String.valueOf(resultSet.getInt("pages"));
                return dataArray;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ObservableList<String> loadBook() {

        ObservableList<String> items = FXCollections.observableArrayList();
        Statement statement = MySqlConnector.getInstance().getNewStatement();
        ResultSet resultSet = null;
        try {
            resultSet = statement.executeQuery("SELECT * FROM `books`");
            while (resultSet.next()) {
                items.add(resultSet.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

}
