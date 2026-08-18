package com.daymark.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain representation of a task.
 *
 * <p>Description, due date, and completion time may be {@code null}. Workflow
 * validation belongs to the application service introduced in the next milestone.</p>
 */
public record Task(
        UUID id,
        String title,
        String description,
        LocalDate dueDate,
        Priority priority,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
    public Task {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
