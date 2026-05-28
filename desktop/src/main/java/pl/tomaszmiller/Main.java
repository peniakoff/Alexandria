package pl.tomaszmiller;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.tomaszmiller.config.AppConfig;
import pl.tomaszmiller.i18n.I18n;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Parent root;
        try {
            root = Utils.loadView("/pl/tomaszmiller/views/loginView.fxml");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load the login view.", exception);
        }

        primaryStage.setTitle(I18n.get("app.title"));
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
