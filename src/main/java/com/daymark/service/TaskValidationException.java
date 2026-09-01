package com.daymark.service;

public final class TaskValidationException extends IllegalArgumentException {
    public TaskValidationException(String message) {
        super(message);
    }
}
