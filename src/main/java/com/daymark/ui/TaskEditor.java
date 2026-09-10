package com.daymark.ui;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Window;

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
        priority.getItems().setAll(Priority.values());
        priority.setValue(task == null ? Priority.MEDIUM : task.priority());
        title.setPromptText("Task title");
        notes.setPromptText("Optional notes");
        notes.setWrapText(true);
        notes.setPrefRowCount(4);
        due.setPromptText("Optional due date");
        if (task != null) {
            title.setText(task.title());
            notes.setText(task.description());
            due.setValue(task.dueDate());
        }
        error.getStyleClass().add("error-text");
        error.setWrapText(true);
        VBox form = new VBox(10, fieldLabel("Title", title), title, fieldLabel("Notes", notes), notes,
                fieldLabel("Due date", due), due, fieldLabel("Priority", priority), priority, error);
        form.setPadding(new Insets(8));
        form.setPrefWidth(420);
        getDialogPane().setContent(form);
        getDialogPane().getStylesheets().add(owner.getScene().getStylesheets().getFirst());
        ButtonType save = new ButtonType("Save task", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
        ((Button) getDialogPane().lookupButton(save)).setMinWidth(Region.USE_PREF_SIZE);
        getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            // Don't close the form until storage succeeds; a failed save shouldn't lose the user's typing.
            event.consume();
            try {
                due.commitValue();
            } catch (RuntimeException exception) {
                showError("Enter a valid due date or leave it blank.");
                return;
            }
            onSave.accept(this);
        });
        setOnShown(event -> title.requestFocus());
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
