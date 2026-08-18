package com.daymark.persistence;

/** Indicates that local persistence could not complete an operation. */
public final class PersistenceException extends RuntimeException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
