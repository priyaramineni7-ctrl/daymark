package com.daymark.ui;

import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;

import java.time.LocalDate;

/** The task collections exposed by the dashboard navigation. */
public enum TaskFilter {
    TODAY("Today", "Tasks that need your attention today"),
    UPCOMING("Upcoming", "Plan what is ahead"),
    ALL("All tasks", "Everything in one place"),
    COMPLETED("Completed", "A record of what you have finished");

    private final String displayName;
    private final String description;

    TaskFilter(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public boolean includes(Task task, LocalDate today) {
        return switch (this) {
            case TODAY -> task.status() == TaskStatus.ACTIVE
                    && task.dueDate() != null
                    && !task.dueDate().isAfter(today);
            case UPCOMING -> task.status() == TaskStatus.ACTIVE
                    && task.dueDate() != null
                    && task.dueDate().isAfter(today);
            case ALL -> task.status() == TaskStatus.ACTIVE;
            case COMPLETED -> task.status() == TaskStatus.COMPLETED;
        };
    }
}
