package com.daymark.app;

import com.daymark.persistence.DatabaseManager;
import com.daymark.persistence.PersistenceException;
import com.daymark.persistence.SQLiteTaskRepository;
import com.daymark.service.TaskService;
import com.daymark.ui.TaskDashboard;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.Objects;

/** JavaFX entry point for Daymark. */
public final class DaymarkApplication extends Application {
    private static final System.Logger LOGGER = System.getLogger(DaymarkApplication.class.getName());

    @Override
    public void start(Stage stage) {
        DatabaseManager databaseManager = DatabaseManager.forDefaultLocation();
        try {
            databaseManager.initialize();
        } catch (PersistenceException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Daymark database startup failed", exception);
            showStartupFailure();
            Platform.exit();
            return;
        }

        TaskService taskService = new TaskService(new SQLiteTaskRepository(databaseManager));
        TaskDashboard dashboard = new TaskDashboard(taskService);

        Scene scene = new Scene(dashboard, 1_120, 760);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        DaymarkApplication.class.getResource("/com/daymark/ui/daymark.css"),
                        "Daymark stylesheet is missing"
                ).toExternalForm()
        );
        stage.setTitle("Daymark");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
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
