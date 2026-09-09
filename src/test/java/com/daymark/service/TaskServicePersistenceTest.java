package com.daymark.service;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.persistence.DatabaseManager;
import com.daymark.persistence.SQLiteTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class TaskServicePersistenceTest {
    @TempDir Path directory;

    @Test
    void editsAndStatusChangesSurviveOpeningTheDatabaseAgain() {
        Path path = directory.resolve("tasks.db");
        DatabaseManager database = new DatabaseManager(path);
        database.initialize();
        SQLiteTaskRepository repository = new SQLiteTaskRepository(database);
        Instant createdAt = Instant.parse("2026-09-05T12:00:00Z");
        Task created = new TaskService(repository, Clock.fixed(createdAt, ZoneOffset.UTC))
                .createTask("Read chapter", "Take notes", LocalDate.of(2026, 9, 6), Priority.LOW);
        TaskService service = new TaskService(repository,
                Clock.fixed(createdAt.plusSeconds(60), ZoneOffset.UTC));

        Task edited = service.updateTask(created.id(), " Review chapter ", null, null, Priority.HIGH);
        assertEquals(edited, readFromDisk(path, created));
        Task completed = service.completeTask(created.id());
        assertEquals(completed, readFromDisk(path, created));
        Task reopened = service.reopenTask(created.id());
        assertEquals(reopened, readFromDisk(path, created));
        assertEquals(TaskStatus.ACTIVE, reopened.status());
        assertNull(reopened.completedAt());
        assertEquals(createdAt, reopened.createdAt());
    }

    private Task readFromDisk(Path path, Task task) {
        // A fresh repository catches changes that were only kept in memory.
        return new SQLiteTaskRepository(new DatabaseManager(path)).findById(task.id()).orElseThrow();
    }
}
