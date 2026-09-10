package com.daymark.ui;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.time.LocalDate;
import java.util.function.Consumer;

/** The same form is used for a new task and an existing one. */
final class TaskEditor extends Dialog<Void> {
    private final TextField title = new TextField();
    private final TextArea notes = new TextArea();
    private final DatePicker due = new DatePicker();
    private final ComboBox<Priority> priority = new ComboBox<>();
    private final Label error = new Label();

    TaskEditor(Window owner, Task task, Consumer<TaskEditor> onSave) {
        initOwner(owner);
        setTitle(task == null ? "New task" : "Edit task");
        setHeaderText(task == null ? "What's next?" : "Update your task");
        setResizable(true);
        title.setId("task-title");
        notes.setId("task-notes");
        due.setId("task-due");
        priority.setId("task-priority");
        error.setId("task-error");
        priority.getItems().setAll(Priority.values());
        priority.setValue(task == null ? Priority.MEDIUM : task.priority());
        title.setPromptText("Task title");
        notes.setPromptText("Optional notes");
        notes.setWrapText(true);
        notes.setPrefRowCount(4);
        due.setPromptText("Optional due date");
        Button clearDate = new Button("Clear date");
        clearDate.setId("clear-date");
        clearDate.setOnAction(event -> {
            due.setValue(null);
            due.getEditor().clear();
        });
        if (task != null) {
            title.setText(task.title());
            notes.setText(task.description());
            due.setValue(task.dueDate());
        }
        error.getStyleClass().add("error-text");
        error.setWrapText(true);
        VBox form = new VBox(10, fieldLabel("Title", title), title, fieldLabel("Notes", notes), notes,
                fieldLabel("Due date", due), new HBox(10, due, clearDate), fieldLabel("Priority", priority), priority, error);
        form.setPadding(new Insets(8));
        form.setPrefWidth(420);
        getDialogPane().setContent(form);
        getDialogPane().getStylesheets().setAll(owner.getScene().getStylesheets());
        ButtonType save = new ButtonType("Save task", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
        getDialogPane().lookupButton(save).setId("save-task");
        getDialogPane().lookupButton(ButtonType.CANCEL).setId("cancel-task");
        ((Button) getDialogPane().lookupButton(save)).setMinWidth(Region.USE_PREF_SIZE);
        getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            // Don't close the form until storage succeeds; a failed save shouldn't lose the user's typing.
            event.consume();
            error.setText("");
            try {
                due.commitValue();
            } catch (RuntimeException exception) {
                showError("Enter a valid due date or leave it blank.");
                return;
            }
            onSave.accept(this);
        });
        setOnCloseRequest(event -> {
            // Disabling the buttons doesn't disable the title-bar X or Escape.
            if (getDialogPane().isDisabled()) event.consume();
        });
        setOnShown(event -> {
            // The native window has its own close event, separate from Dialog's Cancel handling.
            getDialogPane().getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, close -> {
                if (getDialogPane().isDisabled()) close.consume();
            });
            title.requestFocus();
        });
    }

    private Label fieldLabel(String text, Control control) {
        Label label = new Label(text);
        label.setLabelFor(control);
        return label;
    }

    String taskTitle() { return title.getText(); }
    String description() { return notes.getText(); }
    LocalDate dueDate() { return due.getValue(); }
    Priority priority() { return priority.getValue(); }
    void showError(String message) { error.setText(message); }
}
