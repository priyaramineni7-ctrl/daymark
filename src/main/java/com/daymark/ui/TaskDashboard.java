package com.daymark.ui;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.persistence.PersistenceException;
import com.daymark.service.TaskService;
import com.daymark.service.TaskValidationException;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Main Daymark workspace. It translates user gestures into application-service calls. */
public final class TaskDashboard extends BorderPane {
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final TaskService taskService;
    private final TextField searchField = new TextField();
    private final VBox taskList = new VBox(12);
    private final Label viewTitle = new Label();
    private final Label viewDescription = new Label();
    private final Label resultCount = new Label();
    private final Label feedback = new Label();
    private final Map<TaskFilter, Button> navigationButtons = new EnumMap<>(TaskFilter.class);

    private List<Task> allTasks = List.of();
    private TaskFilter currentFilter = TaskFilter.TODAY;

    public TaskDashboard(TaskService taskService) {
        this.taskService = Objects.requireNonNull(taskService, "taskService must not be null");
        getStyleClass().add("dashboard");
        setTop(createTopBar());
        setLeft(createSidebar());
        setCenter(createWorkspace());
        refreshTasks();
    }

    private Node createTopBar() {
        Label mark = new Label("D");
        mark.getStyleClass().add("brand-mark");
        Label name = new Label("Daymark");
        name.getStyleClass().add("brand-name");
        HBox brand = new HBox(10, mark, name);
        brand.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search tasks");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> renderTasks());

        Button addButton = new Button("+  New task");
        addButton.getStyleClass().addAll("button", "primary-button");
        addButton.setOnAction(event -> showCreateDialog());

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, javafx.scene.layout.Priority.ALWAYS);

        HBox topBar = new HBox(20, brand, leftSpacer, searchField, rightSpacer, addButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");
        return topBar;
    }

    private Node createSidebar() {
        Label planningLabel = new Label("PLAN");
        planningLabel.getStyleClass().add("sidebar-section-label");

        VBox navigation = new VBox(5);
        for (TaskFilter filter : TaskFilter.values()) {
            Button button = new Button(navigationLabel(filter));
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.getStyleClass().add("navigation-button");
            button.setOnAction(event -> selectFilter(filter));
            navigationButtons.put(filter, button);
            navigation.getChildren().add(button);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        VBox sidebar = new VBox(12, planningLabel, navigation, spacer);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(238);
        updateNavigationState();
        return sidebar;
    }

    private String navigationLabel(TaskFilter filter) {
        return switch (filter) {
            case TODAY -> "●   Today";
            case ALL -> "☰   All tasks";
            case COMPLETED -> "✓   Completed";
        };
    }

    private Node createWorkspace() {
        Label date = new Label(FULL_DATE.format(LocalDate.now()).toUpperCase());
        date.getStyleClass().add("date-eyebrow");
        viewTitle.getStyleClass().add("view-title");
        viewDescription.getStyleClass().add("view-description");
        resultCount.getStyleClass().add("result-count");

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox headingRow = new HBox(12, viewTitle, titleSpacer, resultCount);
        headingRow.setAlignment(Pos.BASELINE_LEFT);

        VBox heading = new VBox(6, date, headingRow, viewDescription);
        heading.getStyleClass().add("workspace-heading");

        taskList.getStyleClass().add("task-list");
        taskList.setFillWidth(true);
        ScrollPane scrollPane = new ScrollPane(taskList);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("task-scroll");

        feedback.getStyleClass().add("feedback-banner");
        feedback.setVisible(false);
        feedback.setManaged(false);

        VBox workspace = new VBox(18, heading, feedback, scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        workspace.getStyleClass().add("workspace");
        return workspace;
    }

    private void selectFilter(TaskFilter filter) {
        currentFilter = filter;
        updateNavigationState();
        renderTasks();
    }

    private void updateNavigationState() {
        navigationButtons.forEach((filter, button) -> {
            if (filter == currentFilter) {
                if (!button.getStyleClass().contains("selected")) {
                    button.getStyleClass().add("selected");
                }
            } else {
                button.getStyleClass().remove("selected");
            }
        });
    }

    private void refreshTasks() {
        try {
            allTasks = taskService.findAllTasks();
            renderTasks();
        } catch (PersistenceException exception) {
            showError("Tasks could not be loaded", exception);
        }
    }

    private void renderTasks() {
        viewTitle.setText(currentFilter.displayName());
        viewDescription.setText(currentFilter.description());
        List<Task> visibleTasks = TaskListModel.select(
                allTasks,
                currentFilter,
                searchField.getText(),
                LocalDate.now()
        );
        resultCount.setText(visibleTasks.size() + (visibleTasks.size() == 1 ? " task" : " tasks"));
        taskList.getChildren().clear();

        if (visibleTasks.isEmpty()) {
            taskList.getChildren().add(createEmptyState());
            return;
        }
        visibleTasks.stream().map(this::createTaskCard).forEach(taskList.getChildren()::add);
    }

    private Node createEmptyState() {
        Label icon = new Label(searchField.getText().isBlank() ? "✓" : "⌕");
        icon.getStyleClass().add("empty-icon");
        Label title = new Label(searchField.getText().isBlank()
                ? emptyTitle()
                : "No matching tasks");
        title.getStyleClass().add("empty-title");
        Label description = new Label(searchField.getText().isBlank()
                ? emptyDescription()
                : "Try a different title or keyword.");
        description.getStyleClass().add("empty-description");
        description.setWrapText(true);

        VBox content = new VBox(8, icon, title, description);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(70, 24, 70, 24));
        content.getStyleClass().add("empty-state");

        if (searchField.getText().isBlank() && currentFilter != TaskFilter.COMPLETED) {
            Button button = new Button("Create a task");
            button.getStyleClass().add("secondary-button");
            button.setOnAction(event -> showCreateDialog());
            content.getChildren().add(button);
        }
        return content;
    }

    private String emptyTitle() {
        return switch (currentFilter) {
            case TODAY -> "Your day is clear";
            case ALL -> "Start with one meaningful task";
            case COMPLETED -> "Finished tasks will appear here";
        };
    }

    private String emptyDescription() {
        return switch (currentFilter) {
            case TODAY -> "No overdue tasks and nothing due today.";
            case ALL -> "Capture what matters, then take it one step at a time.";
            case COMPLETED -> "Complete a task to build your progress history.";
        };
    }

    private Node createTaskCard(Task task) {
        CheckBox completion = new CheckBox();
        completion.setSelected(task.status() == TaskStatus.COMPLETED);
        completion.getStyleClass().add("task-checkbox");
        completion.setOnAction(event -> changeCompletion(task, completion.isSelected()));

        Label title = new Label(task.title());
        title.setWrapText(true);
        title.getStyleClass().add("task-title");
        if (task.status() == TaskStatus.COMPLETED) {
            title.getStyleClass().add("completed-title");
        }

        VBox text = new VBox(7, title);
        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);
        if (task.description() != null) {
            Label description = new Label(task.description());
            description.setWrapText(true);
            description.setMaxWidth(620);
            description.getStyleClass().add("task-description");
            text.getChildren().add(description);
        }
        text.getChildren().add(createMetadata(task));

        MenuButton menu = new MenuButton("•••");
        menu.getStyleClass().add("more-button");
        MenuItem edit = new MenuItem("Edit task");
        edit.setOnAction(event -> showEditDialog(task));
        MenuItem toggle = new MenuItem(task.status() == TaskStatus.COMPLETED
                ? "Restore task"
                : "Mark complete");
        toggle.setOnAction(event -> changeCompletion(task, task.status() != TaskStatus.COMPLETED));
        MenuItem delete = new MenuItem("Delete task");
        delete.setOnAction(event -> confirmDelete(task));
        menu.getItems().addAll(edit, toggle, new SeparatorMenuItem(), delete);

        HBox card = new HBox(14, completion, text, menu);
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("task-card");
        if (task.status() == TaskStatus.COMPLETED) {
            card.getStyleClass().add("completed-card");
        }
        return card;
    }

    private Node createMetadata(Task task) {
        FlowPane metadata = new FlowPane(Orientation.HORIZONTAL, 8, 6);
        metadata.getStyleClass().add("task-metadata");
        metadata.getChildren().add(priorityBadge(task.priority()));
        if (task.dueDate() != null) {
            Label due = new Label(dueDateLabel(task));
            due.getStyleClass().add("due-badge");
            if (task.status() == TaskStatus.ACTIVE && task.dueDate().isBefore(LocalDate.now())) {
                due.getStyleClass().add("overdue-badge");
            } else if (task.dueDate().equals(LocalDate.now())) {
                due.getStyleClass().add("today-badge");
            }
            metadata.getChildren().add(due);
        }
        return metadata;
    }

    private Label priorityBadge(Priority priority) {
        Label label = new Label(capitalize(priority.name()) + " priority");
        label.getStyleClass().addAll("priority-badge", "priority-" + priority.name().toLowerCase());
        return label;
    }

    private String dueDateLabel(Task task) {
        LocalDate dueDate = task.dueDate();
        LocalDate today = LocalDate.now();
        if (dueDate.equals(today)) {
            return "Due today";
        }
        if (dueDate.equals(today.plusDays(1))) {
            return "Due tomorrow";
        }
        if (task.status() == TaskStatus.ACTIVE && dueDate.isBefore(today)) {
            return "Overdue · " + SHORT_DATE.format(dueDate);
        }
        return "Due " + SHORT_DATE.format(dueDate);
    }

    private String capitalize(String value) {
        return value.charAt(0) + value.substring(1).toLowerCase();
    }

    private void showCreateDialog() {
        Window owner = getScene() == null ? null : getScene().getWindow();
        new TaskEditorDialog(owner, null).showAndWait().ifPresent(draft -> {
            try {
                taskService.createTask(
                        draft.title(),
                        draft.description(),
                        draft.dueDate(),
                        draft.priority()
                );
                currentFilter = TaskFilter.ALL;
                updateNavigationState();
                showFeedback("Task created");
                refreshTasks();
            } catch (TaskValidationException | PersistenceException exception) {
                showError("The task could not be created", exception);
            }
        });
    }

    private void showEditDialog(Task task) {
        Window owner = getScene() == null ? null : getScene().getWindow();
        new TaskEditorDialog(owner, task).showAndWait().ifPresent(draft -> {
            try {
                taskService.updateTask(
                        task.id(),
                        draft.title(),
                        draft.description(),
                        draft.dueDate(),
                        draft.priority()
                );
                showFeedback("Changes saved");
                refreshTasks();
            } catch (TaskValidationException | PersistenceException exception) {
                showError("The task could not be updated", exception);
            }
        });
    }

    private void changeCompletion(Task task, boolean completed) {
        try {
            if (completed) {
                taskService.completeTask(task.id());
                showFeedback("Task completed");
            } else {
                taskService.restoreTask(task.id());
                showFeedback("Task restored");
            }
            refreshTasks();
        } catch (RuntimeException exception) {
            showError("The task could not be changed", exception);
            refreshTasks();
        }
    }

    private void confirmDelete(Task task) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(getScene().getWindow());
        confirmation.setTitle("Delete task");
        confirmation.setHeaderText("Delete “" + task.title() + "”?");
        confirmation.setContentText("This action cannot be undone.");
        confirmation.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> deleteTask(task));
    }

    private void deleteTask(Task task) {
        try {
            taskService.deleteTask(task.id());
            showFeedback("Task deleted");
            refreshTasks();
        } catch (RuntimeException exception) {
            showError("The task could not be deleted", exception);
        }
    }

    private void showFeedback(String message) {
        feedback.setText(message);
        feedback.setManaged(true);
        feedback.setVisible(true);
    }

    private void showError(String heading, RuntimeException exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (getScene() != null) {
            alert.initOwner(getScene().getWindow());
        }
        alert.setTitle("Daymark");
        alert.setHeaderText(heading);
        alert.setContentText(exception.getMessage() == null
                ? "Please try again."
                : exception.getMessage());
        alert.showAndWait();
    }
}
