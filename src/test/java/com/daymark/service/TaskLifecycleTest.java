package com.daymark.service;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.persistence.PersistenceException;
import com.daymark.repository.TaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class TaskLifecycleTest {
    private static final Instant CREATED = Instant.parse("2026-09-05T12:00:00Z");
    private static final Instant NOW = CREATED.plusSeconds(3600);
    private final RecordingRepository repository = new RecordingRepository();
    private final TaskService service = serviceAt(NOW);

    @Test
    void editsContentWithoutChangingIdentityOrCreationTime() {
        Task original = repository.stored;
        LocalDate due = LocalDate.of(2020, 1, 1);
        Task edited = service.updateTask(original.id(), "  Review notes  ", "  Chapter two  ", due, Priority.HIGH);

        assertEquals(new Task(original.id(), "Review notes", "Chapter two", due, Priority.HIGH,
                TaskStatus.ACTIVE, CREATED, NOW, null), edited);
        assertSame(edited, repository.stored);
        assertEquals(1, repository.updates);
        assertEquals("Read chapter two", original.title());
    }

    @Test
    void clearsOptionalFieldsWhenEditing() {
        Task edited = service.updateTask(repository.stored.id(), "Title", " \t\n", null, Priority.LOW);
        assertNull(edited.description());
        assertNull(edited.dueDate());
        assertNull(service.updateTask(edited.id(), "Title", null, null, Priority.LOW).description());
    }

    @Test
    void acceptsEditLengthLimitsAfterTrimming() {
        Task edited = service.updateTask(repository.stored.id(), " " + "a".repeat(100) + " ",
                " " + "b".repeat(500) + " ", null, Priority.MEDIUM);
        assertEquals("a".repeat(100), edited.title());
        assertEquals("b".repeat(500), edited.description());
    }

    @Test
    void rejectsInvalidEditsWithoutChangingTheSavedTask() {
        Task original = repository.stored;
        for (String title : new String[]{null, "", " \t\n", "a".repeat(101)}) {
            assertThrows(TaskValidationException.class,
                    () -> service.updateTask(original.id(), title, null, null, Priority.LOW));
        }
        assertThrows(TaskValidationException.class,
                () -> service.updateTask(original.id(), "Title", "b".repeat(501), null, Priority.LOW));
        assertThrows(TaskValidationException.class,
                () -> service.updateTask(original.id(), "Title", null, null, null));
        assertSame(original, repository.stored);
        assertEquals(0, repository.updates);
    }

    @Test
    void completionPreservesContentAndUsesOneTimestamp() {
        Task original = repository.stored;
        Task completed = service.completeTask(original.id());
        assertEquals(new Task(original.id(), original.title(), original.description(), original.dueDate(),
                original.priority(), TaskStatus.COMPLETED, CREATED, NOW, NOW), completed);
        assertSame(completed, repository.stored);
    }

    @Test
    void editingACompletedTaskKeepsItsCompletionTime() {
        Task completed = service.completeTask(repository.stored.id());
        Instant later = NOW.plusSeconds(60);
        Task edited = serviceAt(later).updateTask(completed.id(), "Reworded", null, null, Priority.LOW);
        assertEquals(TaskStatus.COMPLETED, edited.status());
        assertEquals(NOW, edited.completedAt());
        assertEquals(later, edited.updatedAt());
        assertEquals(CREATED, edited.createdAt());
        assertEquals(completed.id(), edited.id());
    }

    @Test
    void reopeningClearsCompletionAndAllowsANewCompletionTime() {
        Task completed = service.completeTask(repository.stored.id());
        Instant later = NOW.plusSeconds(60);
        Task reopened = serviceAt(later).reopenTask(completed.id());
        assertEquals(new Task(completed.id(), completed.title(), completed.description(), completed.dueDate(),
                completed.priority(), TaskStatus.ACTIVE, CREATED, later, null), reopened);
        Instant finishedAgain = later.plusSeconds(60);
        Task recompleted = serviceAt(finishedAgain).completeTask(reopened.id());
        assertEquals(TaskStatus.COMPLETED, recompleted.status());
        assertEquals(finishedAgain, recompleted.completedAt());
        assertEquals(finishedAgain, recompleted.updatedAt());
    }

    @Test
    void repeatedStatusActionsDoNotWriteOrMoveTimestamps() {
        Task active = repository.stored;
        assertSame(active, service.reopenTask(active.id()));
        assertEquals(0, repository.updates);
        Task completed = service.completeTask(active.id());
        // Use a later clock so an accidental timestamp reset would be visible.
        assertSame(completed, serviceAt(NOW.plusSeconds(60)).completeTask(active.id()));
        assertEquals(1, repository.updates);
    }

    @Test
    void rejectsMissingTasksAndNullIdsForEveryOperation() {
        Task original = repository.stored;
        for (Consumer<UUID> operation : operations()) {
            assertThrows(TaskNotFoundException.class, () -> operation.accept(UUID.randomUUID()));
            assertThrows(TaskValidationException.class, () -> operation.accept(null));
        }
        assertSame(original, repository.stored);
        assertEquals(0, repository.updates);
    }

    @Test
    void propagatesLookupFailuresForEveryOperation() {
        repository.lookupFailure = new PersistenceException("Cannot read tasks");
        for (Consumer<UUID> operation : operations()) {
            assertSame(repository.lookupFailure, assertThrows(PersistenceException.class,
                    () -> operation.accept(repository.stored.id())));
        }
        assertEquals(0, repository.updates);
    }

    @Test
    void propagatesUpdateFailuresWithoutChangingTheStoredTask() {
        Task active = repository.stored;
        repository.updateFailure = new PersistenceException("Cannot save task");
        assertSame(repository.updateFailure, assertThrows(PersistenceException.class,
                () -> service.updateTask(active.id(), "New title", null, null, Priority.LOW)));
        assertSame(repository.updateFailure, assertThrows(PersistenceException.class,
                () -> service.completeTask(active.id())));
        assertSame(active, repository.stored);

        repository.updateFailure = null;
        Task completed = service.completeTask(active.id());
        repository.updateFailure = new PersistenceException("Cannot reopen task");
        assertSame(repository.updateFailure, assertThrows(PersistenceException.class,
                () -> service.reopenTask(completed.id())));
        assertSame(completed, repository.stored);
    }

    private TaskService serviceAt(Instant instant) {
        return new TaskService(repository, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private List<Consumer<UUID>> operations() {
        return List.of(id -> service.updateTask(id, "Title", null, null, Priority.LOW),
                service::completeTask, service::reopenTask);
    }

    // Keep storage small here; SQLite persistence is checked separately with a real database.
    private static final class RecordingRepository implements TaskRepository {
        private Task stored = new Task(UUID.randomUUID(), "Read chapter two", "Take notes",
                LocalDate.of(2026, 9, 6), Priority.MEDIUM, TaskStatus.ACTIVE, CREATED, CREATED, null);
        private int updates;
        private PersistenceException lookupFailure;
        private PersistenceException updateFailure;

        public Optional<Task> findById(UUID id) {
            if (lookupFailure != null) {
                throw lookupFailure;
            }
            return stored.id().equals(id) ? Optional.of(stored) : Optional.empty();
        }

        public Task update(Task task) {
            if (updateFailure != null) {
                throw updateFailure;
            }
            assertEquals(stored.id(), task.id());
            stored = task;
            updates++;
            return task;
        }

        public Task insert(Task task) { throw new AssertionError("Unexpected insert"); }
        public List<Task> findAll() { throw new AssertionError("Unexpected list"); }
        public void deleteById(UUID id) { throw new AssertionError("Unexpected delete"); }
    }
}
