package com.daymark.persistence;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.repository.TaskRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Stores tasks using the schema owned by DatabaseManager. */
public final class SQLiteTaskRepository implements TaskRepository {
    private final DatabaseManager database;

    public SQLiteTaskRepository(DatabaseManager database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public Task insert(Task task) {
        return save(task, """
                INSERT INTO tasks (title, description, due_date, priority, status,
                    created_at, updated_at, completed_at, id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """);
    }

    @Override
    public Task update(Task task) {
        return save(task, """
                UPDATE tasks SET title = ?, description = ?, due_date = ?, priority = ?,
                    status = ?, created_at = ?, updated_at = ?, completed_at = ? WHERE id = ?
                """);
    }

    private Task save(Task task, String sql) {
        Objects.requireNonNull(task);
        try (var connection = database.openConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, task.title());
            nullableText(statement, 2, task.description());
            nullableText(statement, 3, task.dueDate());
            statement.setString(4, task.priority().name());
            statement.setString(5, task.status().name());
            statement.setString(6, task.createdAt().toString());
            statement.setString(7, task.updatedAt().toString());
            nullableText(statement, 8, task.completedAt());
            statement.setString(9, task.id().toString());
            // A missing update must not look like a successful save to the caller.
            if (statement.executeUpdate() != 1) {
                throw new PersistenceException("Task no longer exists: " + task.id());
            }
            return task;
        } catch (SQLException exception) {
            throw new PersistenceException("Could not save task " + task.id(), exception);
        }
    }

    @Override
    public Optional<Task> findById(UUID id) {
        Objects.requireNonNull(id);
        try (var connection = database.openConnection();
             var statement = connection.prepareStatement("SELECT * FROM tasks WHERE id = ?")) {
            statement.setString(1, id.toString());
            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(readTask(rows)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not load task " + id, exception);
        }
    }

    @Override
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        // The ID breaks ties when tasks share a creation timestamp.
        try (var connection = database.openConnection();
             var statement = connection.prepareStatement("SELECT * FROM tasks ORDER BY created_at, id");
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                tasks.add(readTask(rows));
            }
            return List.copyOf(tasks);
        } catch (SQLException exception) {
            throw new PersistenceException("Could not load tasks", exception);
        }
    }

    @Override
    public void deleteById(UUID id) {
        Objects.requireNonNull(id);
        try (var connection = database.openConnection();
             var statement = connection.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Could not delete task " + id, exception);
        }
    }

    private Task readTask(ResultSet row) throws SQLException {
        try {
            String due = row.getString("due_date");
            String completed = row.getString("completed_at");
            return new Task(
                    UUID.fromString(row.getString("id")), row.getString("title"),
                    row.getString("description"), due == null ? null : LocalDate.parse(due),
                    Priority.valueOf(row.getString("priority")),
                    TaskStatus.valueOf(row.getString("status")),
                    Instant.parse(row.getString("created_at")),
                    Instant.parse(row.getString("updated_at")),
                    completed == null ? null : Instant.parse(completed));
        } catch (IllegalArgumentException | java.time.DateTimeException | NullPointerException exception) {
            throw new PersistenceException("Stored task data is invalid", exception);
        }
    }

    private void nullableText(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.toString());
        }
    }
}
