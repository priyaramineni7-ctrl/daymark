package com.daymark.service;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.repository.TaskRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Coordinates task workflows and applies input and lifecycle rules. */
public final class TaskService {
    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    private final TaskRepository taskRepository;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository) {
        this(taskRepository, Clock.systemUTC());
    }

    public TaskService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = Objects.requireNonNull(
                taskRepository,
                "taskRepository must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Task createTask(
            String title,
            String description,
            LocalDate dueDate,
            Priority priority
    ) {
        String normalizedTitle = validateTitle(title);
        String normalizedDescription = validateDescription(description);
        Priority validatedPriority = requirePriority(priority);
        Instant now = clock.instant();

        Task task = new Task(
                UUID.randomUUID(),
                normalizedTitle,
                normalizedDescription,
                dueDate,
                validatedPriority,
                TaskStatus.ACTIVE,
                now,
                now,
                null
        );
        return taskRepository.insert(task);
    }

    public Task updateTask(
            UUID id,
            String title,
            String description,
            LocalDate dueDate,
            Priority priority
    ) {
        Task existing = requireTask(id);
        Task updated = new Task(
                existing.id(),
                validateTitle(title),
                validateDescription(description),
                dueDate,
                requirePriority(priority),
                existing.status(),
                existing.createdAt(),
                clock.instant(),
                existing.completedAt()
        );
        return taskRepository.update(updated);
    }

    public Task completeTask(UUID id) {
        Task existing = requireTask(id);
        if (existing.status() == TaskStatus.COMPLETED) {
            return existing;
        }

        Instant now = clock.instant();
        Task completed = new Task(
                existing.id(),
                existing.title(),
                existing.description(),
                existing.dueDate(),
                existing.priority(),
                TaskStatus.COMPLETED,
                existing.createdAt(),
                now,
                now
        );
        return taskRepository.update(completed);
    }

    public Task restoreTask(UUID id) {
        Task existing = requireTask(id);
        if (existing.status() == TaskStatus.ACTIVE) {
            return existing;
        }

        Task restored = new Task(
                existing.id(),
                existing.title(),
                existing.description(),
                existing.dueDate(),
                existing.priority(),
                TaskStatus.ACTIVE,
                existing.createdAt(),
                clock.instant(),
                null
        );
        return taskRepository.update(restored);
    }

    public Optional<Task> findTask(UUID id) {
        return taskRepository.findById(requireId(id));
    }

    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }

    public void deleteTask(UUID id) {
        Task existing = requireTask(id);
        taskRepository.deleteById(existing.id());
    }

    private Task requireTask(UUID id) {
        UUID validatedId = requireId(id);
        return taskRepository.findById(validatedId)
                .orElseThrow(() -> new TaskNotFoundException(validatedId));
    }

    private UUID requireId(UUID id) {
        if (id == null) {
            throw new TaskValidationException("Task ID is required");
        }
        return id;
    }

    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new TaskValidationException("Title is required");
        }

        String normalized = title.strip();
        if (normalized.length() > MAX_TITLE_LENGTH) {
            throw new TaskValidationException(
                    "Title must be " + MAX_TITLE_LENGTH + " characters or fewer"
            );
        }
        return normalized;
    }

    private String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalized = description.strip();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new TaskValidationException(
                    "Description must be " + MAX_DESCRIPTION_LENGTH + " characters or fewer"
            );
        }
        return normalized;
    }

    private Priority requirePriority(Priority priority) {
        if (priority == null) {
            throw new TaskValidationException("Priority is required");
        }
        return priority;
    }
}
