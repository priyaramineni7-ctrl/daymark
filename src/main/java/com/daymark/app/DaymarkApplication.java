package com.daymark.app;

import com.daymark.persistence.DatabaseManager;
import com.daymark.persistence.PersistenceException;
import com.daymark.persistence.SQLiteTaskRepository;
import com.daymark.service.TaskService;
import com.daymark.ui.TaskBrowser;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.Objects;
import java.time.Clock;

/** JavaFX entry point for Daymark. */
public final class DaymarkApplication extends Application {
    private static final System.Logger LOGGER = System.getLogger(DaymarkApplication.class.getName());
    private TaskBrowser browser;
    // Make sure the local database is ready before building the task screens.
    @Override
    public void start(Stage stage) {
        DatabaseManager database = DatabaseManager.forDefaultLocation();
        try {
            database.initialize();
        } 
        // A missing or unavailable database means the app cannot safely continue.
        catch (PersistenceException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Daymark database startup failed", exception);
            showStartupFailure();
            Platform.exit();
            return;
        }
        // Assemble storage here; the task screen only needs the service.
        TaskService service = new TaskService(new SQLiteTaskRepository(database));
        browser = new TaskBrowser(service, Clock.systemDefaultZone());
        Scene scene = new Scene(browser, 1_000, 700);
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
        stage.setOnCloseRequest(event -> {
            if (!browser.requestClose()) event.consume();
        });
        stage.show();
        // Refresh is explicit so returning from an editor doesn't erase the save feedback.
    }

    @Override
    public void stop() {
        if (browser != null) browser.close();
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
