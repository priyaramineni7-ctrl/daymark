package com.daymark.service;

/** Indicates that task input does not meet Daymark's domain rules. */
public final class TaskValidationException extends IllegalArgumentException {
    public TaskValidationException(String message) {
        super(message);
    }
}
