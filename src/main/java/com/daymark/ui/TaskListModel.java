package com.daymark.ui;

import com.daymark.domain.Task;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// Static and JavaFX-free on purpose. Everything that decides which tasks appear and in
// what order lives here, so TaskListModelTest can cover it without starting a toolkit -
// the dashboard itself has no test, and this is the part worth having one for.
public final class TaskListModel {
    // Undated tasks sink to the bottom; within a day, HIGH comes first. Priority is
    // declared LOW..HIGH so reverseOrder gives us the descending order we want.
    private static final Comparator<Task> DISPLAY_ORDER = Comparator
            .comparing(Task::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::priority, Comparator.reverseOrder())
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
}
