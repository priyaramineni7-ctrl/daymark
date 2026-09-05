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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private final RecordingRepository repository = new RecordingRepository();
    private final TaskService service = new TaskService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsAnActiveTaskWithNormalizedInput() {
        Task task = service.createTask("  Read chapter two  ", "  Take notes  ", null, Priority.MEDIUM);
        assertEquals("Read chapter two", task.title());
        assertEquals("Take notes", task.description());
        assertEquals(Priority.MEDIUM, task.priority());
        assertEquals(TaskStatus.ACTIVE, task.status());
        assertEquals(NOW, task.createdAt());
        assertEquals(NOW, task.updatedAt());
        assertNull(task.completedAt());
        assertNull(task.dueDate());
        assertNotNull(task.id());
        assertEquals(List.of(task), repository.saved);
    }

    @Test
    void normalizesMissingNotesAndGeneratesSeparateIds() {
        Task first = service.createTask("First", null, null, Priority.LOW);
        Task second = service.createTask("Second", " \t\n", null, Priority.LOW);
        assertNull(first.description());
        assertNull(second.description());
        assertNotEquals(first.id(), second.id());
    }

    @Test
    void acceptsLengthLimitsAfterTrimming() {
        // Padding is removed before checking the limits on what actually gets stored.
        Task task = service.createTask(" " + "a".repeat(100) + " ",
                " " + "b".repeat(500) + " ", null, Priority.HIGH);
        assertEquals(100, task.title().length());
        assertEquals(500, task.description().length());
    }

    @Test
    void rejectsInvalidTitlesWithoutSavingAnything() {
        for (String title : new String[]{null, "", " \t\n", "a".repeat(101)}) {
            assertThrows(TaskValidationException.class,
                    () -> service.createTask(title, null, null, Priority.LOW));
        }
        assertTrue(repository.saved.isEmpty());
    }

    @Test
    void rejectsLongNotesAndMissingPriorityWithoutSavingAnything() {
        assertThrows(TaskValidationException.class,
                () -> service.createTask("Title", "b".repeat(501), null, Priority.LOW));
        assertThrows(TaskValidationException.class,
                () -> service.createTask("Title", null, null, null));
        assertTrue(repository.saved.isEmpty());
    }

    @Test
    void preservesPastPresentAndFutureDueDates() {
        for (LocalDate due : List.of(LocalDate.of(2020, 1, 1),
                LocalDate.of(2026, 9, 5), LocalDate.of(2030, 1, 1))) {
            assertEquals(due, service.createTask("Title", null, due, Priority.LOW).dueDate());
        }
    }

    @Test
    void letsStorageFailuresReachTheCaller() {
        PersistenceException failure = new PersistenceException("Disk unavailable");
        repository.failure = failure;
        assertSame(failure, assertThrows(PersistenceException.class,
                () -> service.createTask("Title", null, null, Priority.LOW)));
    }

    // Only insert is expected during creation. Unexpected repository calls fail the test.
    private static final class RecordingRepository implements TaskRepository {
        private final List<Task> saved = new ArrayList<>();
        private PersistenceException failure;

        public Task insert(Task task) {
            if (failure != null) {
                throw failure;
            }
            saved.add(task);
            return task;
        }

        public Task update(Task task) { throw new AssertionError("Unexpected update"); }
        public Optional<Task> findById(UUID id) { throw new AssertionError("Unexpected lookup"); }
        public List<Task> findAll() { throw new AssertionError("Unexpected list"); }
        public void deleteById(UUID id) { throw new AssertionError("Unexpected delete"); }
    }
}
