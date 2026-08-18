package com.daymark.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** Creates configured SQLite connections and owns schema initialization. */
public final class DatabaseManager {
    private static final int BUSY_TIMEOUT_MILLIS = 5_000;

    private static final String CREATE_TASKS_TABLE = """
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL CHECK (length(trim(title)) BETWEEN 1 AND 100),
                description TEXT CHECK (description IS NULL OR length(description) <= 500),
                due_date TEXT,
                priority TEXT NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
                status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED')),
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                completed_at TEXT
            )
            """;

    private static final String CREATE_STATUS_DUE_DATE_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_tasks_status_due_date
            ON tasks (status, due_date)
            """;

    private final Path databasePath;

    public DatabaseManager(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath must not be null")
                .toAbsolutePath()
                .normalize();
    }

    public static DatabaseManager forDefaultLocation() {
        return new DatabaseManager(defaultDatabasePath());
    }

    public static Path defaultDatabasePath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "Daymark", "daymark.db");
        }

        return Path.of(System.getProperty("user.home"), ".daymark", "daymark.db");
    }

    public Path databasePath() {
        return databasePath;
    }

    public void initialize() {
        try {
            createParentDirectory();
            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute(CREATE_TASKS_TABLE);
                statement.execute(CREATE_STATUS_DUE_DATE_INDEX);
            }
        } catch (IOException | SQLException exception) {
            throw new PersistenceException(
                    "Could not initialize the Daymark database at " + databasePath,
                    exception
            );
        }
    }

    public Connection openConnection() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MILLIS);
            } catch (SQLException configurationFailure) {
                connection.close();
                throw configurationFailure;
            }
            return connection;
        } catch (SQLException exception) {
            throw new PersistenceException(
                    "Could not connect to the Daymark database at " + databasePath,
                    exception
            );
        }
    }

    private void createParentDirectory() throws IOException {
        Path parent = databasePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
