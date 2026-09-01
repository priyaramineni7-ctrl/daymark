package com.daymark.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskTest {
    @Test
    void constructsTaskWithExpectedValues() {
        UUID id = UUID.randomUUID();
        LocalDate dueDate = LocalDate.of(2026, 8, 20);
        Instant createdAt = Instant.parse("2026-08-18T15:00:00Z");

        Task task = new Task(
                id,
                "Prepare portfolio demo",
                "Walk through the architecture",
                dueDate,
                Priority.HIGH,
                TaskStatus.ACTIVE,
                createdAt,
                createdAt,
                null
        );

        assertEquals(id, task.id());
        assertEquals("Prepare portfolio demo", task.title());
        assertEquals(dueDate, task.dueDate());
        assertEquals(Priority.HIGH, task.priority());
        assertEquals(TaskStatus.ACTIVE, task.status());
        assertNull(task.completedAt());
    }

    @Test
    void requiredFieldsRejectNull() {
        Instant now = Instant.parse("2026-08-18T15:00:00Z");

        assertThrows(NullPointerException.class, () -> new Task(
                UUID.randomUUID(),
                "Task",
                null,
                null,
                null,
                TaskStatus.ACTIVE,
                now,
                now,
                null
        ));
    }
}
