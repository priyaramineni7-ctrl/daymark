package com.daymark.ui;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.persistence.DatabaseManager;
import com.daymark.persistence.PersistenceException;
import com.daymark.persistence.SQLiteTaskRepository;
import com.daymark.repository.TaskRepository;
import com.daymark.service.TaskService;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Opt in on a machine with a desktop: mvn test -Ddaymark.uiTests=true */
@EnabledIfSystemProperty(named = "daymark.uiTests", matches = "true")
class TaskBrowserWorkflowTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-09T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.now(CLOCK);
    @TempDir Path directory;
    private SQLiteTaskRepository disk;
    private ControlledRepository repository;
    private TaskBrowser browser;
    private Stage stage;

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            ready.countDown();
        });
        assertTrue(ready.await(10, TimeUnit.SECONDS), "JavaFX did not start");
    }

    @BeforeEach
    void openBrowser() throws Exception {
        DatabaseManager database = new DatabaseManager(directory.resolve("tasks.db"));
        database.initialize();
        disk = new SQLiteTaskRepository(database);
        repository = new ControlledRepository(disk);
        showBrowser();
    }

    private void showBrowser() throws Exception {
        fx(() -> {
            browser = new TaskBrowser(new TaskService(repository, CLOCK), CLOCK);
            stage = new Stage();
            Scene scene = new Scene(browser, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/daymark/ui/daymark.css").toExternalForm());
            stage.setScene(scene);
            stage.setOnCloseRequest(event -> {
                if (!browser.requestClose()) event.consume();
            });
            stage.show();
            return null;
        });
        awaitIdle();
    }

    @AfterEach
    void closeWindows() throws Exception {
        repository.releaseSave.countDown();
        awaitIdle();
        fx(() -> {
            // Copy first: hiding a window changes JavaFX's live window list.
            List.copyOf(Window.getWindows()).forEach(Window::hide);
            browser.close();
            return null;
        });
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void createsEditsCompletesReopensAndReloadsFromDisk() throws Exception {
        click("new-task");
        fill("  Review chapter  ", "  Check joins  ", TODAY, Priority.HIGH);
        click("save-task");
        awaitIdle();
        Task original = disk.findAll().getFirst();
        assertEquals("Review chapter", original.title());
        assertEquals("Check joins", original.description());
        assertEquals(List.of(original), visibleTasks());

        click("edit-" + original.id());
        assertEquals("Review chapter", fx(() -> ((TextField) node("task-title")).getText()));
        assertEquals(TODAY, fx(() -> ((DatePicker) node("task-due")).getValue()));
        fill("Finish assignment", "Later this week", TODAY.plusDays(3), Priority.LOW);
        click("save-task");
        awaitIdle();
        assertTrue(visibleTasks().isEmpty());
        assertTrue(feedback().contains("outside this view"));

        click("view-all_tasks");
        Task edited = disk.findById(original.id()).orElseThrow();
        assertEquals(original.createdAt(), edited.createdAt());
        assertEquals(List.of(edited), visibleTasks());
        click("status-" + original.id());
        awaitIdle();
        click("view-completed");
        Task completed = disk.findById(original.id()).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, completed.status());
        assertNotNull(completed.completedAt());
        assertEquals(List.of(completed), visibleTasks());

        click("edit-" + original.id());
        fill("Finished assignment", "", TODAY, Priority.MEDIUM);
        click("clear-date");
        click("save-task");
        awaitIdle();
        Task cleared = disk.findById(original.id()).orElseThrow();
        assertNull(cleared.description());
        assertNull(cleared.dueDate());
        assertEquals(completed.completedAt(), cleared.completedAt());

        click("status-" + original.id());
        awaitIdle();
        assertTrue(visibleTasks().isEmpty());
        Task reopened = disk.findById(original.id()).orElseThrow();
        assertEquals(TaskStatus.ACTIVE, reopened.status());
        assertNull(reopened.completedAt());
        fx(() -> { browser.close(); stage.hide(); return null; });
        showBrowser();
        click("view-all_tasks");
        assertEquals(List.of(reopened), visibleTasks());
    }

    @Test
    void invalidInputAndCancelLeaveTheSavedTaskAlone() throws Exception {
        Task original = seed();
        click("edit-" + original.id());
        fill(" ", "Changed notes", TODAY, Priority.HIGH);
        click("save-task");
        awaitIdle();
        assertEquals("Title is required", error());
        assertEquals("Changed notes", fx(() -> ((TextArea) node("task-notes")).getText()));
        fill("a".repeat(101), "Notes", TODAY, Priority.LOW);
        click("save-task");
        awaitIdle();
        assertTrue(error().contains("100"));
        fill("Valid", "b".repeat(501), TODAY, Priority.LOW);
        click("save-task");
        awaitIdle();
        assertTrue(error().contains("500"));
        click("cancel-task");
        assertEquals(original, disk.findById(original.id()).orElseThrow());
        assertEquals(List.of(original), visibleTasks());
    }

    @Test
    void failedSaveKeepsTheDraftAndCanBeRetried() throws Exception {
        Task original = seed();
        click("edit-" + original.id());
        fill("Retry this title", "Keep my notes", TODAY, Priority.HIGH);
        repository.failWrites = true;
        click("save-task");
        awaitIdle();
        assertTrue(error().contains("Couldn't save"));
        assertEquals(original, disk.findById(original.id()).orElseThrow());
        assertEquals(List.of(original), visibleTasks());
        assertEquals("Retry this title", fx(() -> ((TextField) node("task-title")).getText()));
        repository.failWrites = false;
        click("save-task");
        awaitIdle();
        assertEquals("Retry this title", visibleTasks().getFirst().title());
    }

    @Test
    void invalidTypedDateDoesNotOverwriteTheExistingDueDate() throws Exception {
        Task original = seed();
        click("edit-" + original.id());
        fx(() -> {
            ((DatePicker) node("task-due")).getEditor().setText("not a date");
            return null;
        });
        click("save-task");
        awaitIdle();
        assertTrue(error().contains("valid due date"));
        assertEquals(original, disk.findById(original.id()).orElseThrow());
        click("clear-date");
        click("save-task");
        awaitIdle();
        assertNull(disk.findById(original.id()).orElseThrow().dueDate());
    }

    @Test
    void failedCompletionLeavesTheTaskActiveAndCanBeRetried() throws Exception {
        Task original = seed();
        repository.failWrites = true;
        click("status-" + original.id());
        awaitIdle();
        assertEquals(original, disk.findById(original.id()).orElseThrow());
        assertEquals(List.of(original), visibleTasks());
        fx(() -> {
            DialogPane alert = Window.getWindows().stream()
                    .map(window -> window.getScene().getRoot())
                    .filter(DialogPane.class::isInstance).map(DialogPane.class::cast)
                    .findFirst().orElseThrow();
            assertTrue(alert.getContentText().contains("Couldn't save"));
            ((Button) alert.lookupButton(ButtonType.OK)).fire();
            return null;
        });
        repository.failWrites = false;
        click("status-" + original.id());
        awaitIdle();
        assertTrue(visibleTasks().isEmpty());
        assertEquals(TaskStatus.COMPLETED, disk.findById(original.id()).orElseThrow().status());
    }

    @Test
    void missingTaskShowsAnErrorAndRefreshRemovesTheStaleRow() throws Exception {
        Task original = seed();
        click("edit-" + original.id());
        disk.deleteById(original.id());
        click("save-task");
        awaitIdle();
        assertTrue(error().contains("no longer exists"));
        click("cancel-task");
        click("refresh-tasks");
        awaitIdle();
        assertTrue(visibleTasks().isEmpty());
    }

    @Test
    void searchAndEditsUseTheLatestSavedContent() throws Exception {
        Task original = seed();
        setSearch("  JOINS  ");
        assertEquals(List.of(original), visibleTasks());
        click("edit-" + original.id());
        fill("Read chapter", "Practice grouping", TODAY, Priority.LOW);
        click("save-task");
        awaitIdle();
        assertTrue(visibleTasks().isEmpty());
        assertTrue(feedback().contains("outside this view"));
        setSearch("grouping");
        assertEquals(1, visibleTasks().size());
    }

    @Test
    void loadFailureIsNotPresentedAsAnEmptyListAndRefreshRecovers() throws Exception {
        fx(() -> { browser.close(); stage.hide(); return null; });
        repository.failReads = true;
        showBrowser();
        assertTrue(feedback().contains("Couldn't load"));
        assertTrue(fx(() -> ((ListView<?>) node("task-list")).getPlaceholder()
                .lookupAll(".label").stream().anyMatch(label -> ((Label) label).getText().equals("Tasks couldn't be loaded"))));
        repository.failReads = false;
        click("refresh-tasks");
        awaitIdle();
        assertEquals("", feedback());
    }

    @Test
    void pendingSaveBlocksDuplicateSubmissionAndWindowClosure() throws Exception {
        Task original = seed();
        click("edit-" + original.id());
        fill("One save", "Wait for storage", TODAY, Priority.HIGH);
        repository.holdWrites = true;
        click("save-task");
        assertTrue(repository.saveStarted.await(5, TimeUnit.SECONDS));
        fx(() -> {
            assertTrue(node("save-task").isDisabled());
            Window editor = node("save-task").getScene().getWindow();
            editor.fireEvent(new WindowEvent(editor, WindowEvent.WINDOW_CLOSE_REQUEST));
            assertTrue(editor.isShowing());
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            assertTrue(stage.isShowing());
            ((Button) node("save-task")).fire();
            return null;
        });
        repository.releaseSave.countDown();
        awaitIdle();
        assertEquals(1, repository.writeCount);
        assertEquals("One save", disk.findById(original.id()).orElseThrow().title());
    }

    private Task seed() throws Exception {
        Task task = new TaskService(disk, CLOCK).createTask("Read chapter", "Check joins", TODAY, Priority.LOW);
        click("refresh-tasks");
        awaitIdle();
        return task;
    }

    private void fill(String title, String notes, LocalDate due, Priority priority) throws Exception {
        fx(() -> {
            ((TextField) node("task-title")).setText(title);
            ((TextArea) node("task-notes")).setText(notes);
            ((DatePicker) node("task-due")).setValue(due);
            @SuppressWarnings("unchecked")
            ComboBox<Priority> choices = (ComboBox<Priority>) node("task-priority");
            choices.setValue(priority);
            return null;
        });
    }

    private void setSearch(String query) throws Exception {
        fx(() -> { ((TextField) node("task-search")).setText(query); return null; });
    }

    private void click(String id) throws Exception {
        fx(() -> { ((ButtonBase) node(id)).fire(); return null; });
    }

    private javafx.scene.Node node(String id) {
        for (Window window : Window.getWindows()) {
            if (window.getScene() == null) continue;
            window.getScene().getRoot().applyCss();
            window.getScene().getRoot().layout();
            var found = window.getScene().lookup("#" + id);
            if (found != null) return found;
        }
        throw new AssertionError("Control not found: " + id);
    }

    private String error() throws Exception {
        return fx(() -> ((Label) node("task-error")).getText());
    }

    private String feedback() throws Exception {
        return fx(() -> ((Label) node("browser-feedback")).getText());
    }

    private List<Task> visibleTasks() throws Exception {
        return fx(() -> {
            @SuppressWarnings("unchecked")
            ListView<Task> list = (ListView<Task>) node("task-list");
            return List.copyOf(list.getItems());
        });
    }

    private void awaitIdle() throws Exception {
        // Wait for the observable UI state, not a guessed amount of database time.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (fx(() -> !node("refresh-tasks").isDisabled())) return;
            Thread.sleep(20);
        }
        fail("Task operation did not finish within 10 seconds");
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> call = new FutureTask<>(action);
        Platform.runLater(call);
        return call.get(10, TimeUnit.SECONDS);
    }

    // Most calls still use real SQLite. These switches let us reproduce failures without breaking a real database.
    private static final class ControlledRepository implements TaskRepository {
        private final TaskRepository delegate;
        private volatile boolean failReads;
        private volatile boolean failWrites;
        private volatile boolean holdWrites;
        private volatile int writeCount;
        private final CountDownLatch saveStarted = new CountDownLatch(1);
        private final CountDownLatch releaseSave = new CountDownLatch(1);

        ControlledRepository(TaskRepository delegate) { this.delegate = delegate; }

        public Task insert(Task task) { beforeWrite(); return delegate.insert(task); }
        public Task update(Task task) { beforeWrite(); return delegate.update(task); }
        public Optional<Task> findById(UUID id) { return delegate.findById(id); }
        public void deleteById(UUID id) { delegate.deleteById(id); }
        public List<Task> findAll() {
            if (failReads) throw new PersistenceException("Simulated read failure");
            return delegate.findAll();
        }

        private void beforeWrite() {
            if (failWrites) throw new PersistenceException("Simulated save failure");
            if (holdWrites) {
                saveStarted.countDown();
                try {
                    if (!releaseSave.await(10, TimeUnit.SECONDS)) throw new PersistenceException("Test save timed out");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new PersistenceException("Test save interrupted", exception);
                }
            }
            writeCount++;
        }
    }
}
