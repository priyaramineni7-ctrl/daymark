package com.daymark.service;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-31T18:00:00Z");

    private InMemoryTaskRepository repository;
    private TaskService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTaskRepository();
        service = new TaskService(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsNormalizedActiveTask() {
        LocalDate dueDate = LocalDate.of(2026, 9, 5);

        Task task = service.createTask(
                "  Prepare demo  ",
                "  Walk through the architecture  ",
                dueDate,
                Priority.HIGH
        );

        assertEquals("Prepare demo", task.title());
        assertEquals("Walk through the architecture", task.description());
        assertEquals(dueDate, task.dueDate());
        assertEquals(Priority.HIGH, task.priority());
        assertEquals(TaskStatus.ACTIVE, task.status());
        assertEquals(NOW, task.createdAt());
        assertEquals(NOW, task.updatedAt());
        assertNull(task.completedAt());
        assertEquals(task, repository.findById(task.id()).orElseThrow());
    }

    @Test
    void treatsBlankDescriptionAsMissing() {
        Task task = service.createTask("Task", "   ", null, Priority.MEDIUM);

        assertNull(task.description());
    }

    @Test
    void rejectsInvalidInput() {
        assertAll(
                () -> assertThrows(
                        TaskValidationException.class,
                        () -> service.createTask(null, null, null, Priority.LOW)
                ),
                () -> assertThrows(
                        TaskValidationException.class,
                        () -> service.createTask("   ", null, null, Priority.LOW)
                ),
                () -> assertThrows(
                        TaskValidationException.class,
                        () -> service.createTask(
                                "x".repeat(TaskService.MAX_TITLE_LENGTH + 1),
                                null,
                                null,
                                Priority.LOW
                        )
                ),
                () -> assertThrows(
                        TaskValidationException.class,
                        () -> service.createTask(
                                "Task",
                                "x".repeat(TaskService.MAX_DESCRIPTION_LENGTH + 1),
                                null,
                                Priority.LOW
                        )
                ),
                () -> assertThrows(
                        TaskValidationException.class,
                        () -> service.createTask("Task", null, null, null)
                )
        );
    }

    @Test
    void updatesEditableFieldsAndPreservesLifecycleData() {
        Instant createdAt = NOW.minusSeconds(3_600);
        Task existing = task(TaskStatus.ACTIVE, createdAt, createdAt, null);
        repository.insert(existing);

        Task updated = service.updateTask(
                existing.id(),
                "  Revised title ",
                " Revised description ",
                LocalDate.of(2026, 9, 8),
                Priority.HIGH
        );

        assertEquals("Revised title", updated.title());
        assertEquals("Revised description", updated.description());
        assertEquals(existing.createdAt(), updated.createdAt());
        assertEquals(existing.status(), updated.status());
        assertEquals(NOW, updated.updatedAt());
    }

    @Test
    void completesAndRestoresTask() {
        Task existing = task(
                TaskStatus.ACTIVE,
                NOW.minusSeconds(3_600),
                NOW.minusSeconds(3_600),
                null
        );
        repository.insert(existing);

        Task completed = service.completeTask(existing.id());
        assertEquals(TaskStatus.COMPLETED, completed.status());
        assertEquals(NOW, completed.completedAt());
        assertEquals(NOW, completed.updatedAt());
        assertSame(completed, service.completeTask(existing.id()));

        Task restored = service.restoreTask(existing.id());
        assertEquals(TaskStatus.ACTIVE, restored.status());
        assertNull(restored.completedAt());
        assertEquals(NOW, restored.updatedAt());
        assertSame(restored, service.restoreTask(existing.id()));
    }

    @Test
    void findsAndDeletesTask() {
        Task created = service.createTask("Remove me", null, null, Priority.LOW);

        assertEquals(Optional.of(created), service.findTask(created.id()));
        assertEquals(List.of(created), service.findAllTasks());

        service.deleteTask(created.id());

        assertFalse(service.findTask(created.id()).isPresent());
    }

    @Test
    void missingTaskOperationsFailClearly() {
        UUID missingId = UUID.randomUUID();

        assertAll(
                () -> assertThrows(
                        TaskNotFoundException.class,
                        () -> service.updateTask(
                                missingId,
                                "Task",
                                null,
                                null,
                                Priority.LOW
                        )
                ),
                () -> assertThrows(
                        TaskNotFoundException.class,
                        () -> service.completeTask(missingId)
                ),
                () -> assertThrows(
                        TaskNotFoundException.class,
                        () -> service.restoreTask(missingId)
                ),
                () -> assertThrows(
                        TaskNotFoundException.class,
                        () -> service.deleteTask(missingId)
                )
        );
    }

    private Task task(
            TaskStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt
    ) {
        return new Task(
                UUID.randomUUID(),
                "Existing task",
                null,
                null,
                Priority.MEDIUM,
                status,
                createdAt,
                updatedAt,
                completedAt
        );
    }

    private static final class InMemoryTaskRepository implements TaskRepository {
        private final Map<UUID, Task> tasks = new LinkedHashMap<>();

        @Override
        public Task insert(Task task) {
            tasks.put(task.id(), task);
            return task;
        }

        @Override
        public Task update(Task task) {
            tasks.put(task.id(), task);
            return task;
        }

        @Override
        public Optional<Task> findById(UUID id) {
            return Optional.ofNullable(tasks.get(id));
        }

        @Override
        public List<Task> findAll() {
            return List.copyOf(tasks.values());
        }

        @Override
        public void deleteById(UUID id) {
            tasks.remove(id);
        }
    }
}
