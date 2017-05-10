package pl.tomaszmiller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import pl.tomaszmiller.database.MySqlConnector;

import static com.sun.org.apache.bcel.internal.util.SecuritySupport.getResourceAsStream;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        Parent root = FXMLLoader.load(getClass().getResource("views/loginView.fxml"));
//        Parent root = FXMLLoader.load(getClass().getResource("views/userView.fxml"));
        primaryStage.setTitle("Alexandria - simple library management | pre-alpha build");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("images/icon.png")));
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setResizable(false);
        primaryStage.show();

        MySqlConnector mySqlConnector = MySqlConnector.getInstance();

    }


    public static void main(String[] args) {
        launch(args);
    }
}
