package com.daymark.app;

import com.daymark.persistence.DatabaseManager;
import com.daymark.persistence.PersistenceException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Objects;

/** JavaFX entry point for Daymark. */
public final class DaymarkApplication extends Application {
    private static final System.Logger LOGGER = System.getLogger(DaymarkApplication.class.getName());
    // Make sure the local database is ready before building the task screens.
    @Override
    public void start(Stage stage) {
        try {
            DatabaseManager.forDefaultLocation().initialize();
        } 
        // A missing or unavailable database means the app cannot safely continue.
        catch (PersistenceException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Daymark database startup failed", exception);
            showStartupFailure();
            Platform.exit();
            return;
        }
        // Temporary foundation screen; this will become the Today task view.
        Label title = new Label("Daymark foundation is ready");
        title.getStyleClass().add("foundation-title");

        StackPane root = new StackPane(title);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("app-root");
        // Temporary foundation screen; this will become the Today task view.
        // The first real screen will eventually contain navigation, task lists, and task actions.
        Scene scene = new Scene(root, 1_000, 700);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        DaymarkApplication.class.getResource("/com/daymark/ui/daymark.css"),
                        "Daymark stylesheet is missing"
                ).toExternalForm()
        );
     
        stage.setTitle("Daymark");
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.show();
    }

    private void showStartupFailure() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Daymark could not start");
        alert.setHeaderText("Your task database could not be opened.");
        alert.setContentText("Check that your local application-data folder is available and try again.");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
