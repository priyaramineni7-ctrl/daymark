package com.daymark.service;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.repository.TaskRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Applies task input rules before passing tasks to storage. */
public final class TaskService {
    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    private final TaskRepository repository;
    private final Clock clock;

    public TaskService(TaskRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public TaskService(TaskRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        // Tests can supply a fixed clock without changing how production creates tasks.
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Task createTask(String title, String description, LocalDate dueDate, Priority priority) {
        // Validate everything first so invalid input never reaches the database.
        String cleanTitle = validateTitle(title);
        String cleanDescription = validateDescription(description);
        if (priority == null) {
            throw new TaskValidationException("Priority is required");
        }

        // Read the clock once so both timestamps start with exactly the same value.
        Instant now = clock.instant();
        Task task = new Task(UUID.randomUUID(), cleanTitle, cleanDescription, dueDate,
                priority, TaskStatus.ACTIVE, now, now, null);
        // Past dates are allowed: a newly entered task may already be overdue.
        return repository.insert(task);
    }

    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new TaskValidationException("Title is required");
        }
        String cleanTitle = title.strip();
        if (cleanTitle.length() > MAX_TITLE_LENGTH) {
            throw new TaskValidationException("Title must be 100 characters or fewer");
        }
        return cleanTitle;
    }

    private String validateDescription(String description) {
        // Keep one representation of missing notes instead of storing empty strings too.
        if (description == null || description.isBlank()) {
            return null;
        }
        String cleanDescription = description.strip();
        if (cleanDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new TaskValidationException("Description must be 500 characters or fewer");
        }
        return cleanDescription;
    }
}
