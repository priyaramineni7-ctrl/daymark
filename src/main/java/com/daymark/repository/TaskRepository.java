package com.daymark.repository;

import com.daymark.domain.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for task storage. */
public interface TaskRepository {
    /**
     * Adds a new task to storage.
     *
     * @throws RuntimeException if the task cannot be saved
     */
    Task insert(Task task);

    /**
     * Updates an existing task in storage.
     *
     * @throws RuntimeException if the task does not exist or cannot be saved
     */
    Task update(Task task);

    /**
     * Finds a task by its identifier.
     *
     * @return the task, or an empty result when no task has that identifier
     */
    Optional<Task> findById(UUID id);

    /**
     * Returns all stored tasks in the repository's stable ordering.
     */
    List<Task> findAll();

    /**
     * Deletes a task when it exists; deleting an unknown identifier is harmless.
     */
    void deleteById(UUID id);
}
