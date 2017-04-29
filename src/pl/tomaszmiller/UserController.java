package pl.tomaszmiller;

import com.jfoenix.controls.JFXListView;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.util.Duration;


import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by Peniakoff on 29.04.2017.
 */
public class UserController implements Initializable {

    @FXML
    ImageView logo;

    @FXML
    JFXListView theList;

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

    ObservableList<String> items = FXCollections.observableArrayList("Tomek", "Roman", "Pajac");
    theList.setItems(items);

    }
}
