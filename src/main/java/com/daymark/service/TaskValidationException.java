package com.daymark.service;

/** Input that the caller should correct before trying again. */
public final class TaskValidationException extends IllegalArgumentException {
    public TaskValidationException(String message) {
        super(message);
    }
}
