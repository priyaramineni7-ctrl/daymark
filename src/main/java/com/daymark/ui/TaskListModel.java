package com.daymark.ui;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Pure presentation logic for filtering and ordering task cards. */
public final class TaskListModel {
    private static final Comparator<Task> DISPLAY_ORDER = Comparator
            .comparing(Task::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::priority, TaskListModel::comparePriority)
            .thenComparing(Task::createdAt)
            .thenComparing(Task::id);

    private TaskListModel() {
    }

    public static List<Task> select(
            List<Task> tasks,
            TaskFilter filter,
            String searchText,
            LocalDate today
    ) {
        Objects.requireNonNull(tasks, "tasks must not be null");
        Objects.requireNonNull(filter, "filter must not be null");
        Objects.requireNonNull(today, "today must not be null");
        String query = searchText == null ? "" : searchText.strip().toLowerCase(Locale.ROOT);

        return tasks.stream()
                .filter(task -> filter.includes(task, today))
                .filter(task -> matchesSearch(task, query))
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    private static boolean matchesSearch(Task task, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return task.title().toLowerCase(Locale.ROOT).contains(query)
                || task.description() != null
                && task.description().toLowerCase(Locale.ROOT).contains(query);
    }

    private static int comparePriority(Priority left, Priority right) {
        return Integer.compare(rank(right), rank(left));
    }

    private static int rank(Priority priority) {
        return switch (priority) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }
}
