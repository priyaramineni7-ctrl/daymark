package com.daymark.ui;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskListModelTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 31);

    @Test
    void todayIncludesDueTodayAndOverdueActiveTasks() {
        Task overdue = task("Overdue", TODAY.minusDays(2), Priority.LOW, TaskStatus.ACTIVE, 1);
        Task today = task("Today", TODAY, Priority.MEDIUM, TaskStatus.ACTIVE, 2);
        Task future = task("Future", TODAY.plusDays(1), Priority.HIGH, TaskStatus.ACTIVE, 3);
        Task completed = task("Done", TODAY, Priority.HIGH, TaskStatus.COMPLETED, 4);

        List<Task> selected = TaskListModel.select(
                List.of(future, completed, today, overdue),
                TaskFilter.TODAY,
                "",
                TODAY
        );

        assertEquals(List.of(overdue, today), selected);
    }

    @Test
    void eachNavigationViewHasASeparateMeaning() {
        Task unscheduled = task("Inbox", null, Priority.LOW, TaskStatus.ACTIVE, 1);
        Task upcoming = task("Plan", TODAY.plusDays(3), Priority.MEDIUM, TaskStatus.ACTIVE, 2);
        Task completed = task("Done", TODAY.minusDays(1), Priority.HIGH, TaskStatus.COMPLETED, 3);
        List<Task> tasks = List.of(unscheduled, upcoming, completed);

        assertEquals(List.of(upcoming), TaskListModel.select(tasks, TaskFilter.UPCOMING, "", TODAY));
        assertEquals(List.of(upcoming, unscheduled), TaskListModel.select(tasks, TaskFilter.ALL, "", TODAY));
        assertEquals(List.of(completed), TaskListModel.select(tasks, TaskFilter.COMPLETED, "", TODAY));
    }

    @Test
    void searchMatchesTitlesAndDescriptionsIgnoringCase() {
        Task titleMatch = task("Write REPORT", null, Priority.MEDIUM, TaskStatus.ACTIVE, 1);
        Task descriptionMatch = task(
                "Prepare slides",
                "Use the report findings",
                null,
                Priority.LOW,
                TaskStatus.ACTIVE,
                2
        );
        Task noMatch = task("Buy groceries", null, Priority.HIGH, TaskStatus.ACTIVE, 3);

        List<Task> selected = TaskListModel.select(
                List.of(noMatch, descriptionMatch, titleMatch),
                TaskFilter.ALL,
                " report ",
                TODAY
        );

        assertEquals(List.of(titleMatch, descriptionMatch), selected);
    }

    @Test
    void tasksSortByDueDateThenHighPriorityFirst() {
        Task later = task("Later", TODAY.plusDays(2), Priority.HIGH, TaskStatus.ACTIVE, 1);
        Task low = task("Low", TODAY.plusDays(1), Priority.LOW, TaskStatus.ACTIVE, 2);
        Task high = task("High", TODAY.plusDays(1), Priority.HIGH, TaskStatus.ACTIVE, 3);

        assertEquals(
                List.of(high, low, later),
                TaskListModel.select(List.of(later, low, high), TaskFilter.ALL, "", TODAY)
        );
    }

    @Test
    void selectionResultIsUnmodifiable() {
        List<Task> selected = TaskListModel.select(
                List.of(task("Task", null, Priority.LOW, TaskStatus.ACTIVE, 1)),
                TaskFilter.ALL,
                "",
                TODAY
        );

        assertThrows(UnsupportedOperationException.class, () -> selected.add(selected.getFirst()));
    }

    private Task task(
            String title,
            LocalDate dueDate,
            Priority priority,
            TaskStatus status,
            int seconds
    ) {
        return task(title, null, dueDate, priority, status, seconds);
    }

    private Task task(
            String title,
            String description,
            LocalDate dueDate,
            Priority priority,
            TaskStatus status,
            int seconds
    ) {
        Instant timestamp = Instant.parse("2026-08-31T12:00:00Z").plusSeconds(seconds);
        return new Task(
                new UUID(0, seconds),
                title,
                description,
                dueDate,
                priority,
                status,
                timestamp,
                timestamp,
                status == TaskStatus.COMPLETED ? timestamp : null
        );
    }
}
