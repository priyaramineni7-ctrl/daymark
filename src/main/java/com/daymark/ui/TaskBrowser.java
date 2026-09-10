package com.daymark.ui;

import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.service.TaskNotFoundException;
import com.daymark.service.TaskService;
import com.daymark.service.TaskValidationException;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Owns the screen and sends task operations through the service. */
public final class TaskBrowser extends BorderPane implements AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(TaskBrowser.class.getName());
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, uuuu");
    private final TaskService service;
    private final Clock clock;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "daymark-storage");
        thread.setDaemon(true);
        return thread;
    });
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ListView<Task> taskList = new ListView<>();
    private final TextField search = new TextField();
    private final Label heading = new Label();
    private final Label description = new Label();
    private final Label feedback = new Label();
    private List<Task> tasks = List.of();
    private TaskView currentView = TaskView.TODAY;
    private boolean closed;

    public TaskBrowser(TaskService service, Clock clock) {
        this.service = service;
        this.clock = clock;
        getStyleClass().add("app-root");
        setLeft(sidebar());
        heading.getStyleClass().add("page-title");
        description.getStyleClass().add("muted");
        Button create = new Button("+ New task");
        create.getStyleClass().add("primary-button");
        create.setOnAction(event -> edit(null));
        create.disableProperty().bind(busy);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(20, new VBox(6, heading, description), spacer, create);
        header.setAlignment(Pos.CENTER_LEFT);
        search.setPromptText("Search titles and notes");
        search.setAccessibleText("Search tasks");
        search.textProperty().addListener((observable, oldValue, newValue) -> render());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> refresh());
        refresh.disableProperty().bind(busy);
        HBox searchBar = new HBox(10, search, refresh);
        HBox.setHgrow(search, Priority.ALWAYS);
        feedback.getStyleClass().add("muted");
        feedback.setWrapText(true);
        taskList.setCellFactory(list -> new TaskCell());
        taskList.setAccessibleText("Tasks");
        taskList.disableProperty().bind(busy);
        VBox content = new VBox(20, header, searchBar, feedback, taskList);
        content.getStyleClass().add("main-content");
        VBox.setVgrow(taskList, Priority.ALWAYS);
        setCenter(content);
        render();
        refresh();
    }

    private VBox sidebar() {
        Label brand = new Label("Daymark");
        brand.getStyleClass().add("brand");
        Label plan = new Label("PLAN");
        plan.getStyleClass().add("section-label");
        VBox sidebar = new VBox(12, brand, plan);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(190);
        ToggleGroup navigation = new ToggleGroup();
        for (TaskView view : TaskView.values()) {
            ToggleButton button = new ToggleButton(view.title());
            button.getStyleClass().add("nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setToggleGroup(navigation);
            button.setSelected(view == currentView);
            button.setOnAction(event -> {
                button.setSelected(true);
                currentView = view;
                render();
            });
            sidebar.getChildren().add(button);
        }
        return sidebar;
    }

    public void refresh() {
        if (busy.get() || closed) return;
        feedback.setText("Loading tasks...");
        perform(service::findAll, loaded -> {
            tasks = loaded;
            feedback.setText("");
            render();
        }, failure -> feedback.setText("Couldn't load tasks. Use Refresh to try again."));
    }

    private void render() {
        heading.setText(currentView.title());
        description.setText(currentView.description());
        taskList.getItems().setAll(TaskBrowserModel.filter(tasks, currentView, search.getText(), LocalDate.now(clock)));
        boolean searching = !search.getText().isBlank();
        Label title = new Label(searching ? "No matching tasks" : currentView.emptyTitle());
        title.getStyleClass().add("empty-title");
        Label hint = new Label(searching ? "Try a different search or choose another view." : currentView.emptyDescription());
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        VBox empty = new VBox(8, title, hint);
        empty.setAlignment(Pos.CENTER);
        taskList.setPlaceholder(empty);
    }

    private void edit(Task task) {
        TaskEditor editor = new TaskEditor(getScene().getWindow(), task, form -> {
            // Read controls on the FX thread before handing the values to the database worker.
            String title = form.taskTitle();
            String notes = form.description();
            LocalDate due = form.dueDate();
            com.daymark.domain.Priority priority = form.priority();
            perform(() -> task == null ? service.createTask(title, notes, due, priority)
                            : service.updateTask(task.id(), title, notes, due, priority),
                    saved -> {
                        form.close();
                        replace(saved);
                    }, failure -> form.showError(messageFor(failure)));
        });
        editor.getDialogPane().disableProperty().bind(busy);
        editor.showAndWait();
        editor.getDialogPane().disableProperty().unbind();
    }

    private void changeStatus(Task task) {
        perform(() -> task.status() == TaskStatus.ACTIVE ? service.completeTask(task.id()) : service.reopenTask(task.id()),
                this::replace, failure -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, messageFor(failure), ButtonType.OK);
                    alert.initOwner(getScene().getWindow());
                    alert.setHeaderText("The task couldn't be updated");
                    alert.showAndWait();
                });
    }

    private void replace(Task saved) {
        // Only change the visible list after the save succeeds.
        var updated = new java.util.ArrayList<>(tasks);
        updated.removeIf(task -> task.id().equals(saved.id()));
        updated.add(saved);
        tasks = List.copyOf(updated);
        feedback.setText("Task saved.");
        render();
    }

    private String messageFor(Throwable failure) {
        if (failure instanceof TaskValidationException) return failure.getMessage();
        if (failure instanceof TaskNotFoundException) return "This task no longer exists. Close the form and refresh the list.";
        return "Couldn't save your task. Please try again. Your changes have not been saved.";
    }

    private <T> void perform(Supplier<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        if (busy.get() || closed) return;
        busy.set(true);
        // SQLite can wait for a lock. Keep that wait off the thread that draws the window.
        worker.submit(() -> {
            try {
                T result = work.get();
                Platform.runLater(() -> {
                    if (closed) return;
                    busy.set(false);
                    success.accept(result);
                });
            } catch (RuntimeException exception) {
                if (!(exception instanceof TaskValidationException)) {
                    LOGGER.log(System.Logger.Level.ERROR, "Task operation failed", exception);
                }
                Platform.runLater(() -> {
                    if (closed) return;
                    busy.set(false);
                    failure.accept(exception);
                });
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        worker.shutdown();
    }

    private final class TaskCell extends ListCell<Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            // JavaFX reuses cells while scrolling, including cells that used to hold another task.
            setText(null);
            setGraphic(null);
            if (empty || task == null) return;
            Label title = new Label(task.title());
            title.getStyleClass().add("task-title");
            title.setWrapText(true);
            Label details = new Label(task.priority() + " priority" + dueText(task));
            details.getStyleClass().add("muted");
            details.setWrapText(true);
            VBox words = new VBox(6, title, details);
            if (task.description() != null) {
                Label notes = new Label(task.description());
                notes.getStyleClass().add("muted");
                notes.setWrapText(true);
                words.getChildren().add(notes);
            }
            words.setMinWidth(0);
            HBox.setHgrow(words, Priority.ALWAYS);
            Button edit = new Button("Edit");
            edit.setAccessibleText("Edit " + task.title());
            edit.setOnAction(event -> edit(task));
            Button status = new Button(task.status() == TaskStatus.ACTIVE ? "Done" : "Reopen");
            status.setAccessibleText(status.getText() + ": " + task.title());
            status.setOnAction(event -> changeStatus(task));
            HBox row = new HBox(12, words, edit, status);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("task-card");
            row.prefWidthProperty().bind(taskList.widthProperty().subtract(40));
            setGraphic(row);
        }

        private String dueText(Task task) {
            if (task.status() == TaskStatus.COMPLETED) return "  ·  Completed";
            if (task.dueDate() == null) return "  ·  No due date";
            LocalDate today = LocalDate.now(clock);
            if (task.dueDate().isEqual(today)) return "  ·  Due today";
            return "  ·  " + (task.dueDate().isBefore(today) ? "Overdue: " : "Due ") + DATE.format(task.dueDate());
        }
    }
}
