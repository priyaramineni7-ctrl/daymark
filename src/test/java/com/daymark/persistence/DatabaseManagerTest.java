package com.daymark.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsDatabaseSchemaAndIndex() throws SQLException {
        Path databasePath = temporaryDirectory.resolve("nested/daymark.db");
        DatabaseManager databaseManager = new DatabaseManager(databasePath);

        databaseManager.initialize();

        assertTrue(Files.isRegularFile(databasePath));
        assertEquals(1, countSchemaObjects(databaseManager, "table", "tasks"));
        assertEquals(1, countSchemaObjects(databaseManager, "index", "idx_tasks_status_due_date"));
    }

    @Test
    void initializationIsIdempotent() throws SQLException {
        DatabaseManager databaseManager = new DatabaseManager(
                temporaryDirectory.resolve("daymark.db")
        );

        databaseManager.initialize();
        databaseManager.initialize();

        assertEquals(1, countSchemaObjects(databaseManager, "table", "tasks"));
        assertEquals(1, countSchemaObjects(databaseManager, "index", "idx_tasks_status_due_date"));
    }

    @Test
    void invalidParentLocationThrowsPersistenceException() throws IOException {
        Path regularFile = Files.createFile(temporaryDirectory.resolve("not-a-directory"));
        DatabaseManager databaseManager = new DatabaseManager(
                regularFile.resolve("daymark.db")
        );

        assertThrows(PersistenceException.class, databaseManager::initialize);
    }

    @Test
    void connectionsUseWalAndABusyTimeout() throws SQLException {
        DatabaseManager databaseManager = new DatabaseManager(
                temporaryDirectory.resolve("daymark.db")
        );
        databaseManager.initialize();

        try (Connection connection = databaseManager.openConnection();
             Statement statement = connection.createStatement()) {
            assertEquals("wal", stringPragma(statement, "journal_mode"));
            assertEquals(5_000, pragmaValue(statement, "busy_timeout"));
        }
    }

    private int countSchemaObjects(
            DatabaseManager databaseManager,
            String type,
            String name
    ) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sqlite_master WHERE type = '%s' AND name = '%s'"
                .formatted(type, name);
        try (Connection connection = databaseManager.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int pragmaValue(Statement statement, String pragma) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("PRAGMA " + pragma)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String stringPragma(Statement statement, String pragma) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("PRAGMA " + pragma)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
