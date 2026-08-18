package com.daymark.repository;

import com.daymark.domain.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for task storage. */
public interface TaskRepository {
    Task insert(Task task);

    Task update(Task task);

    Optional<Task> findById(UUID id);

    List<Task> findAll();

    void deleteById(UUID id);
}
