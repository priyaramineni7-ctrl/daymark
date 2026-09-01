package com.daymark.ui;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.service.TaskService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Objects;

/** Focused create/edit form with immediate, user-readable validation. */
public final class TaskEditorDialog extends Dialog<TaskDraft> {
    private final TextField titleField = new TextField();
    private final TextArea descriptionField = new TextArea();
    private final DatePicker dueDatePicker = new DatePicker();
    private final ComboBox<Priority> priorityPicker = new ComboBox<>();
    private final Label validationMessage = new Label();

    public TaskEditorDialog(Window owner, Task existing) {
        boolean editing = existing != null;
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle(editing ? "Edit task" : "New task");
        setHeaderText(null);

        ButtonType saveButtonType = new ButtonType(
                editing ? "Save changes" : "Create task",
                ButtonBar.ButtonData.OK_DONE
        );
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        getDialogPane().getStyleClass().add("task-dialog");
        getDialogPane().getStylesheets().add(Objects.requireNonNull(
                TaskEditorDialog.class.getResource("/com/daymark/ui/daymark.css"),
                "Daymark stylesheet is missing"
        ).toExternalForm());
        getDialogPane().setContent(createForm(editing));

        configureFields(existing);
        installValidation(saveButtonType);
        setResultConverter(button -> button == saveButtonType
                ? new TaskDraft(
                        titleField.getText(),
                        descriptionField.getText(),
                        dueDatePicker.getValue(),
                        priorityPicker.getValue()
                )
                : null);

        setOnShown(event -> titleField.requestFocus());
    }

    private Node createForm(boolean editing) {
        Label eyebrow = new Label(editing ? "TASK DETAILS" : "CAPTURE A TASK");
        eyebrow.getStyleClass().add("dialog-eyebrow");

        Label heading = new Label(editing ? "Make a few changes" : "What needs to get done?");
        heading.getStyleClass().add("dialog-heading");

        titleField.setPromptText("e.g. Submit database assignment");
        titleField.getStyleClass().add("editor-title");

        descriptionField.setPromptText("Add useful context (optional)");
        descriptionField.setPrefRowCount(4);
        descriptionField.setWrapText(true);

        dueDatePicker.setPromptText("No due date");
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);

        priorityPicker.getItems().setAll(Priority.LOW, Priority.MEDIUM, Priority.HIGH);
        priorityPicker.setMaxWidth(Double.MAX_VALUE);

        GridPane metadata = new GridPane();
        metadata.setHgap(12);
        metadata.add(fieldGroup("Due date", dueDatePicker), 0, 0);
        metadata.add(fieldGroup("Priority", priorityPicker), 1, 0);
        GridPane.setHgrow(metadata.getChildren().get(0), javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(metadata.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

        validationMessage.getStyleClass().add("validation-message");
        validationMessage.setVisible(false);
        validationMessage.setManaged(false);

        VBox form = new VBox(
                10,
                eyebrow,
                heading,
                spacer(4),
                fieldGroup("Title", titleField),
                fieldGroup("Notes", descriptionField),
                metadata,
                validationMessage
        );
        form.setPadding(new Insets(4, 6, 4, 6));
        form.setPrefWidth(520);
        return form;
    }

    private VBox fieldGroup(String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        VBox group = new VBox(6, label, field);
        HBox.setHgrow(group, javafx.scene.layout.Priority.ALWAYS);
        return group;
    }

    private Node spacer(double height) {
        HBox spacer = new HBox();
        spacer.setMinHeight(height);
        return spacer;
    }

    private void configureFields(Task existing) {
        titleField.setTextFormatter(lengthFormatter(TaskService.MAX_TITLE_LENGTH));
        descriptionField.setTextFormatter(lengthFormatter(TaskService.MAX_DESCRIPTION_LENGTH));
        priorityPicker.setValue(Priority.MEDIUM);

        if (existing != null) {
            titleField.setText(existing.title());
            descriptionField.setText(existing.description() == null ? "" : existing.description());
            dueDatePicker.setValue(existing.dueDate());
            priorityPicker.setValue(existing.priority());
        }
    }

    private TextFormatter<String> lengthFormatter(int maximumLength) {
        return new TextFormatter<>(change -> change.getControlNewText().length() <= maximumLength
                ? change
                : null);
    }

    private void installValidation(ButtonType saveButtonType) {
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleField.getText() == null || titleField.getText().isBlank()) {
                validationMessage.setText("Add a title before saving this task.");
                validationMessage.setManaged(true);
                validationMessage.setVisible(true);
                titleField.requestFocus();
                event.consume();
            }
        });
    }
}
