package com.daymark.service;

import java.util.UUID;

/** The requested task is no longer available, rather than a failure to access storage. */
public final class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(UUID id) {
        super("Task no longer exists: " + id);
    }
}
