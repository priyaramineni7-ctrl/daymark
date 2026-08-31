package com.daymark.persistence;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteTaskRepositoryTest {
    private static final Instant CREATED_AT = Instant.parse("2026-08-31T14:00:00Z");

    @TempDir
    Path temporaryDirectory;

    private SQLiteTaskRepository repository;

    @BeforeEach
    void setUp() {
        DatabaseManager databaseManager = new DatabaseManager(
                temporaryDirectory.resolve("daymark.db")
        );
        databaseManager.initialize();
        repository = new SQLiteTaskRepository(databaseManager);
    }

    @Test
    void insertsAndFindsTaskWithAllFields() {
        Task task = new Task(
                UUID.randomUUID(),
                "Submit project",
                "Attach the final screenshots",
                LocalDate.of(2026, 9, 3),
                Priority.HIGH,
                TaskStatus.COMPLETED,
                CREATED_AT,
                CREATED_AT.plusSeconds(600),
                CREATED_AT.plusSeconds(600)
        );

        assertEquals(task, repository.insert(task));
        assertEquals(task, repository.findById(task.id()).orElseThrow());
    }

    @Test
    void preservesNullableFields() {
        Task task = task("Plan the week", CREATED_AT);

        repository.insert(task);

        Task stored = repository.findById(task.id()).orElseThrow();
        assertEquals(task, stored);
        assertNull(stored.description());
        assertNull(stored.dueDate());
        assertNull(stored.completedAt());
    }

    @Test
    void findsAllTasksInCreationOrder() {
        Task second = task("Second task", CREATED_AT.plusSeconds(60));
        Task first = task("First task", CREATED_AT);

        repository.insert(second);
        repository.insert(first);

        List<Task> tasks = repository.findAll();
        assertEquals(List.of(first, second), tasks);
        assertThrows(UnsupportedOperationException.class, () -> tasks.add(first));
    }

    @Test
    void updatesExistingTask() {
        Task original = task("Original title", CREATED_AT);
        repository.insert(original);
        Task updated = new Task(
                original.id(),
                "Updated title",
                "Updated description",
                LocalDate.of(2026, 9, 10),
                Priority.MEDIUM,
                TaskStatus.ACTIVE,
                original.createdAt(),
                CREATED_AT.plusSeconds(120),
                null
        );

        assertEquals(updated, repository.update(updated));
        assertEquals(updated, repository.findById(original.id()).orElseThrow());
    }

    @Test
    void deletesTaskById() {
        Task task = task("Temporary task", CREATED_AT);
        repository.insert(task);

        repository.deleteById(task.id());

        assertFalse(repository.findById(task.id()).isPresent());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void reportsDuplicateInsertAndMissingUpdate() {
        Task task = task("Unique task", CREATED_AT);
        repository.insert(task);

        assertThrows(PersistenceException.class, () -> repository.insert(task));
        assertThrows(
                PersistenceException.class,
                () -> repository.update(task("Missing task", CREATED_AT))
        );
    }

    private Task task(String title, Instant createdAt) {
        return new Task(
                UUID.randomUUID(),
                title,
                null,
                null,
                Priority.LOW,
                TaskStatus.ACTIVE,
                createdAt,
                createdAt,
                null
        );
    }
}
