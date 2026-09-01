package com.daymark.ui;

import com.daymark.domain.Priority;

import java.time.LocalDate;

public record TaskDraft(
        String title,
        String description,
        LocalDate dueDate,
        Priority priority
) {
}
