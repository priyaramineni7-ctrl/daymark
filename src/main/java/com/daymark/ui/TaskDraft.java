package com.daymark.ui;

import com.daymark.domain.Priority;

import java.time.LocalDate;

/** User-entered task values returned by the editor dialog. */
public record TaskDraft(
        String title,
        String description,
        LocalDate dueDate,
        Priority priority
) {
}
