package pl.tomaszmiller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

//        Parent root = FXMLLoader.load(getClass().getResource("loginView.fxml"));
        Parent root = FXMLLoader.load(getClass().getResource("views/userView.fxml"));
        primaryStage.setTitle("The Library | pre-alpha build");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setResizable(false);
        primaryStage.show();

        MySqlConnector mySqlConnector = MySqlConnector.getInstance();

    }


    public static void main(String[] args) {
        launch(args);
    }
}
