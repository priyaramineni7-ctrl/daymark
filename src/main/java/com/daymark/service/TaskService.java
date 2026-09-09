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

/** Applies task input rules and status changes before passing tasks to storage. */
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
        // A fixed clock makes timestamp behavior predictable in tests.
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Task createTask(String title, String description, LocalDate dueDate, Priority priority) {
        // Validate everything first so invalid input never reaches the database.
        String cleanTitle = validateTitle(title);
        String cleanDescription = validateDescription(description);
        validatePriority(priority);

        // Read the clock once so both timestamps start with exactly the same value.
        Instant now = clock.instant();
        Task task = new Task(UUID.randomUUID(), cleanTitle, cleanDescription, dueDate,
                priority, TaskStatus.ACTIVE, now, now, null);
        // Past dates are allowed: a newly entered task may already be overdue.
        return repository.insert(task);
    }

    /** Replaces the editable fields; null notes or a null due date clear those fields. */
    public Task updateTask(UUID id, String title, String description, LocalDate dueDate, Priority priority) {
        String cleanTitle = validateTitle(title);
        String cleanDescription = validateDescription(description);
        validatePriority(priority);
        Task existing = requireTask(id);

        // Editing the wording shouldn't change when the task was created or completed.
        Task updated = new Task(existing.id(), cleanTitle, cleanDescription, dueDate,
                priority, existing.status(), existing.createdAt(), clock.instant(), existing.completedAt());
        return repository.update(updated);
    }

    /** Marks a task completed, keeping the original completion time on repeated calls. */
    public Task completeTask(UUID id) {
        Task existing = requireTask(id);
        // A second click on Done isn't a new completion event.
        if (existing.status() == TaskStatus.COMPLETED) {
            return existing;
        }
        Instant now = clock.instant();
        Task completed = new Task(existing.id(), existing.title(), existing.description(), existing.dueDate(),
                existing.priority(), TaskStatus.COMPLETED, existing.createdAt(), now, now);
        return repository.update(completed);
    }

    /** Returns a completed task to active; an already-active task is left alone. */
    public Task reopenTask(UUID id) {
        Task existing = requireTask(id);
        if (existing.status() == TaskStatus.ACTIVE) {
            return existing;
        }
        // Clear the old completion time so it can't appear on an active task.
        Task reopened = new Task(existing.id(), existing.title(), existing.description(), existing.dueDate(),
                existing.priority(), TaskStatus.ACTIVE, existing.createdAt(), clock.instant(), null);
        return repository.update(reopened);
    }

    private Task requireTask(UUID id) {
        if (id == null) {
            throw new TaskValidationException("Task ID is required");
        }
        // Load the saved task instead of trusting an older copy held by a screen.
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private void validatePriority(Priority priority) {
        if (priority == null) {
            throw new TaskValidationException("Priority is required");
        }
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
