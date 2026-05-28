package pl.tomaszmiller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.config.AppConfig;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class Main extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                getClass().getResource("/pl/tomaszmiller/views/loginView.fxml"),
                "Login view is missing"
        ));

        primaryStage.setTitle("Alexandria – library management");
        addWindowIcon(primaryStage);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    @Override
    public void stop() {
        AppConfig.getInstance().shutdown();
    }

    private void addWindowIcon(Stage primaryStage) {
        URL iconUrl = getClass().getResource("/pl/tomaszmiller/images/icon.png");
        if (iconUrl == null) {
            LOGGER.debug("Application icon not found on classpath; starting without a custom icon.");
            return;
        }
        primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
    }
}
