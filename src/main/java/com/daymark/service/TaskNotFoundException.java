package com.daymark.service;

import java.util.UUID;

/** Indicates that an operation targeted a task that does not exist. */
public final class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(UUID id) {
        super("Task not found: " + id);
    }
}
