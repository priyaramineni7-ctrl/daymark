package com.daymark.persistence;

import com.daymark.domain.Priority;
import com.daymark.domain.Task;
import com.daymark.domain.TaskStatus;
import com.daymark.repository.TaskRepository;

import java.sql.Connection;
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

/** SQLite-backed implementation of the task persistence contract. */
public final class SQLiteTaskRepository implements TaskRepository {
    private static final String INSERT_TASK = """
            INSERT INTO tasks (
                id, title, description, due_date, priority, status,
                created_at, updated_at, completed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_TASK = """
            UPDATE tasks
            SET title = ?, description = ?, due_date = ?, priority = ?, status = ?,
                created_at = ?, updated_at = ?, completed_at = ?
            WHERE id = ?
            """;

    private static final String FIND_BY_ID = "SELECT * FROM tasks WHERE id = ?";
    private static final String FIND_ALL = """
            SELECT * FROM tasks
            ORDER BY created_at ASC, id ASC
            """;
    private static final String DELETE_BY_ID = "DELETE FROM tasks WHERE id = ?";

    private final DatabaseManager databaseManager;

    public SQLiteTaskRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager,
                "databaseManager must not be null"
        );
    }

    @Override
    public Task insert(Task task) {
        Objects.requireNonNull(task, "task must not be null");

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_TASK)) {
            bindTaskForInsert(statement, task);
            statement.executeUpdate();
            return task;
        } catch (SQLException exception) {
            throw new PersistenceException("Could not insert task " + task.id(), exception);
        }
    }

    @Override
    public Task update(Task task) {
        Objects.requireNonNull(task, "task must not be null");

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_TASK)) {
            bindTaskForUpdate(statement, task);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new PersistenceException("Cannot update missing task " + task.id());
            }
            return task;
        } catch (SQLException exception) {
            throw new PersistenceException("Could not update task " + task.id(), exception);
        }
    }

    @Override
    public Optional<Task> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapTask(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not find task " + id, exception);
        }
    }

    @Override
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tasks.add(mapTask(resultSet));
            }
            return List.copyOf(tasks);
        } catch (SQLException exception) {
            throw new PersistenceException("Could not load tasks", exception);
        }
    }

    @Override
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Could not delete task " + id, exception);
        }
    }

    private void bindTaskForInsert(PreparedStatement statement, Task task) throws SQLException {
        statement.setString(1, task.id().toString());
        statement.setString(2, task.title());
        setNullableString(statement, 3, task.description());
        setNullableString(statement, 4, toText(task.dueDate()));
        statement.setString(5, task.priority().name());
        statement.setString(6, task.status().name());
        statement.setString(7, task.createdAt().toString());
        statement.setString(8, task.updatedAt().toString());
        setNullableString(statement, 9, toText(task.completedAt()));
    }

    private void bindTaskForUpdate(PreparedStatement statement, Task task) throws SQLException {
        statement.setString(1, task.title());
        setNullableString(statement, 2, task.description());
        setNullableString(statement, 3, toText(task.dueDate()));
        statement.setString(4, task.priority().name());
        statement.setString(5, task.status().name());
        statement.setString(6, task.createdAt().toString());
        statement.setString(7, task.updatedAt().toString());
        setNullableString(statement, 8, toText(task.completedAt()));
        statement.setString(9, task.id().toString());
    }

    private Task mapTask(ResultSet resultSet) throws SQLException {
        try {
            String dueDate = resultSet.getString("due_date");
            String completedAt = resultSet.getString("completed_at");

            return new Task(
                    UUID.fromString(resultSet.getString("id")),
                    resultSet.getString("title"),
                    resultSet.getString("description"),
                    dueDate == null ? null : LocalDate.parse(dueDate),
                    Priority.valueOf(resultSet.getString("priority")),
                    TaskStatus.valueOf(resultSet.getString("status")),
                    Instant.parse(resultSet.getString("created_at")),
                    Instant.parse(resultSet.getString("updated_at")),
                    completedAt == null ? null : Instant.parse(completedAt)
            );
        } catch (RuntimeException exception) {
            throw new PersistenceException("Stored task data is invalid", exception);
        }
    }

    private void setNullableString(
            PreparedStatement statement,
            int parameterIndex,
            String value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.VARCHAR);
        } else {
            statement.setString(parameterIndex, value);
        }
    }

    private String toText(Object value) {
        return value == null ? null : value.toString();
    }
}
