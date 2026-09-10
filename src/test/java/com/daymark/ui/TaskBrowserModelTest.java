package com.daymark.ui;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskBrowserModelTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 9);
    private static final Instant CREATED = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void todayIncludesOverdueButExcludesFutureUndatedAndCompletedTasks() {
        Task overdue = task(1, TODAY.minusDays(1), TaskStatus.ACTIVE, Priority.LOW);
        Task today = task(2, TODAY, TaskStatus.ACTIVE, Priority.MEDIUM);
        List<Task> tasks = List.of(today, task(3, TODAY.plusDays(1), TaskStatus.ACTIVE, Priority.HIGH),
                task(4, null, TaskStatus.ACTIVE, Priority.HIGH),
                task(5, TODAY, TaskStatus.COMPLETED, Priority.HIGH), overdue);
        assertEquals(List.of(overdue, today), filter(tasks, TaskView.TODAY, ""));
    }

    @Test
    void allTasksKeepsFutureUndatedAndCompletedWork() {
        Task future = task(1, TODAY.plusDays(2), TaskStatus.ACTIVE, Priority.LOW);
        Task undated = task(2, null, TaskStatus.ACTIVE, Priority.LOW);
        Task completed = task(3, TODAY, TaskStatus.COMPLETED, Priority.HIGH);
        assertEquals(List.of(future, undated, completed),
                filter(List.of(completed, undated, future), TaskView.ALL_TASKS, ""));
    }

    @Test
    void completedShowsMostRecentCompletionFirst() {
        Task first = task(1, TODAY, TaskStatus.COMPLETED, Priority.LOW);
        Task second = task(2, TODAY, TaskStatus.COMPLETED, Priority.LOW);
        assertEquals(List.of(second, first), filter(List.of(first, second,
                task(3, TODAY, TaskStatus.ACTIVE, Priority.LOW)), TaskView.COMPLETED, ""));
    }

    @Test
    void searchMatchesTitleAndNotesIgnoringCaseAndSurroundingSpaces() {
        Task titleMatch = task(1, TODAY, TaskStatus.ACTIVE, Priority.LOW);
        Task notesMatch = new Task(new UUID(0, 2), "Read chapter", "Discuss PROJECT in class", null,
                Priority.LOW, TaskStatus.ACTIVE, CREATED, CREATED, null);
        List<Task> tasks = List.of(titleMatch, notesMatch);
        assertEquals(tasks, filter(tasks, TaskView.ALL_TASKS, "  project  "));
        assertTrue(filter(tasks, TaskView.ALL_TASKS, "nothing like this").isEmpty());
        assertEquals(List.of(titleMatch), filter(tasks, TaskView.TODAY, "project"));
    }

    @Test
    void sortsByDueDateThenPriorityAndBreaksTiesConsistently() {
        Task low = task(1, TODAY, TaskStatus.ACTIVE, Priority.LOW);
        Task high = task(2, TODAY, TaskStatus.ACTIVE, Priority.HIGH);
        Task highAgain = task(3, TODAY, TaskStatus.ACTIVE, Priority.HIGH);
        // Feed the ties in reverse order so the test doesn't pass just by keeping input order.
        assertEquals(List.of(high, highAgain, low), filter(List.of(highAgain, low, high), TaskView.TODAY, ""));
    }

    @Test
    void todayMovesForwardUsingTheSuppliedLocalDate() {
        Task tomorrow = task(1, TODAY.plusDays(1), TaskStatus.ACTIVE, Priority.LOW);
        assertTrue(filter(List.of(tomorrow), TaskView.TODAY, "").isEmpty());
        assertEquals(List.of(tomorrow), TaskBrowserModel.filter(List.of(tomorrow), TaskView.TODAY, "", TODAY.plusDays(1)));
    }

    @Test
    void supportsEmptyListsAndDoesNotModifyTheOriginalList() {
        assertTrue(filter(List.of(), TaskView.TODAY, "").isEmpty());
        Task task = task(1, null, TaskStatus.ACTIVE, Priority.LOW);
        List<Task> original = List.of(task);
        List<Task> result = filter(original, TaskView.ALL_TASKS, "");
        assertThrows(UnsupportedOperationException.class, result::clear);
        assertEquals(List.of(task), original);
    }

    private List<Task> filter(List<Task> tasks, TaskView view, String query) {
        return TaskBrowserModel.filter(tasks, view, query, TODAY);
    }

    private Task task(int id, LocalDate due, TaskStatus status, Priority priority) {
        return new Task(new UUID(0, id), "Project " + id, null, due, priority, status,
                CREATED, CREATED.plusSeconds(id), status == TaskStatus.COMPLETED ? CREATED.plusSeconds(id) : null);
    }
}
