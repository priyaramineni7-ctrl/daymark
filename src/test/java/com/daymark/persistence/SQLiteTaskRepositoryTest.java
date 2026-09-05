package com.daymark.persistence;

import com.daymark.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteTaskRepositoryTest {
    @TempDir Path directory;
    private DatabaseManager database;
    private SQLiteTaskRepository repository;

    @BeforeEach
    void setUp() {
        database = new DatabaseManager(directory.resolve("tasks.db"));
        database.initialize();
        repository = new SQLiteTaskRepository(database);
    }

    @Test
    void savesEveryFieldAndLoadsThroughANewRepository() {
        Task task = task(1, true);
        assertEquals(task, repository.insert(task));
        var reopened = new SQLiteTaskRepository(database);
        assertEquals(task, reopened.findById(task.id()).orElseThrow());
    }

    @Test
    void preservesNullFieldsAndHandlesUnknownIds() {
        Task task = task(1, false);
        repository.insert(task);
        assertEquals(task, repository.findById(task.id()).orElseThrow());
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void listsInStableOrderAndReturnsAnUnmodifiableList() {
        assertTrue(repository.findAll().isEmpty());
        Task first = task(1, false);
        Task second = task(2, false);
        repository.insert(second);
        repository.insert(first);
        assertEquals(List.of(first, second), repository.findAll());
        assertThrows(UnsupportedOperationException.class, () -> repository.findAll().clear());
    }

    @Test
    void updatesAllFieldsIncludingClearingOptionalValues() {
        repository.insert(task(1, true));
        Task updated = task(1, false);
        assertEquals(updated, repository.update(updated));
        assertEquals(List.of(updated), repository.findAll());
    }

    @Test
    void rejectsDuplicateInsertsAndMissingUpdates() {
        Task task = task(1, false);
        assertThrows(PersistenceException.class, () -> repository.update(task));
        repository.insert(task);
        assertThrows(PersistenceException.class, () -> repository.insert(task));
        assertEquals(List.of(task), repository.findAll());
    }

    @Test
    void deletesOnlyTheRequestedTaskAndAllowsRepeatedDeletion() {
        Task first = task(1, false);
        Task second = task(2, false);
        repository.insert(first);
        repository.insert(second);
        repository.deleteById(first.id());
        repository.deleteById(first.id());
        assertEquals(List.of(second), repository.findAll());
    }

    @Test
    void wrapsMalformedStoredDates() throws Exception {
        repository.insert(task(1, false));
        try (var connection = database.openConnection();
             var statement = connection.prepareStatement("UPDATE tasks SET due_date = ? WHERE id = ?")) {
            statement.setString(1, "invalid-date");
            statement.setString(2, new UUID(0, 1).toString());
            statement.executeUpdate();
        }
        assertThrows(PersistenceException.class, repository::findAll);
        assertThrows(PersistenceException.class, () -> repository.findById(new UUID(0, 1)));
    }

    @Test
    void wrapsConnectionFailures() {
        var unavailable = new SQLiteTaskRepository(new DatabaseManager(directory.resolve("missing/tasks.db")));
        assertThrows(PersistenceException.class, unavailable::findAll);
    }

    private Task task(int id, boolean completed) {
        Instant created = Instant.parse("2026-09-05T12:00:00Z");
        return new Task(new UUID(0, id), completed ? "Finish SQL assignment" : "Read chapter 'two'",
                completed ? "Check joins and indexes" : null,
                completed ? LocalDate.of(2026, 9, 6) : null,
                completed ? Priority.HIGH : Priority.LOW,
                completed ? TaskStatus.COMPLETED : TaskStatus.ACTIVE,
                created, completed ? created.plusSeconds(60) : created,
                completed ? created.plusSeconds(60) : null);
    }
}
