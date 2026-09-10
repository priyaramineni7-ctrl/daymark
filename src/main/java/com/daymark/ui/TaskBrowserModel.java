package com.daymark.ui;

import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** List rules kept separate from JavaFX so they can be tested without opening a window. */
public final class TaskBrowserModel {
    private TaskBrowserModel() { }

    public static List<Task> filter(List<Task> tasks, TaskView view, String search, LocalDate today) {
        String query = search.strip().toLowerCase(Locale.ROOT);
        Comparator<Task> order = Comparator
                .comparing((Task task) -> task.status() == TaskStatus.COMPLETED)
                .thenComparing(Task::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::priority, Comparator.reverseOrder())
                .thenComparing(Task::createdAt)
                .thenComparing(Task::id);
        if (view == TaskView.COMPLETED) {
            order = Comparator.comparing(Task::completedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Task::id);
        }
        return tasks.stream().filter(task -> matches(task, view, today))
                .filter(task -> task.title().toLowerCase(Locale.ROOT).contains(query)
                        || (task.description() != null
                        && task.description().toLowerCase(Locale.ROOT).contains(query)))
                .sorted(order).toList();
    }

    private static boolean matches(Task task, TaskView view, LocalDate today) {
        return switch (view) {
            // Overdue work stays in Today until it's finished, rather than disappearing at midnight.
            case TODAY -> task.status() == TaskStatus.ACTIVE && task.dueDate() != null
                    && !task.dueDate().isAfter(today);
            case ALL_TASKS -> true;
            case COMPLETED -> task.status() == TaskStatus.COMPLETED;
        };
    }
}
