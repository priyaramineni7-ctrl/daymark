# Daymark

Daymark is a calm, today-focused desktop task planner built with Java 21, JavaFX, Maven, and SQLite. The project is being developed as a one-week portfolio application with an emphasis on maintainable architecture and thoughtful interaction design.

## Day 1 foundation

The current milestone provides:

- an immutable task domain model;
- a repository abstraction that keeps persistence out of the UI and domain;
- automatic, idempotent SQLite schema initialization;
- a minimal launchable JavaFX shell; and
- automated domain and database foundation tests.

The original terminal implementation is preserved in [`legacy-console`](legacy-console) for comparison. Its compiled files and local `tasks.txt` data are intentionally not part of the new application.

## Requirements

- JDK 21
- Maven 3.6.3 or newer

## Build and test

```shell
mvn clean test
```

## Run

```shell
mvn javafx:run
```

On Windows, Daymark stores its database at `%LOCALAPPDATA%\Daymark\daymark.db`. If `LOCALAPPDATA` is unavailable, it falls back to `~/.daymark/daymark.db`. No database is written inside the repository.

## Architecture

```text
JavaFX application
       |
TaskRepository contract
       |
SQLite infrastructure
       |
Local Daymark database
```

The domain package does not depend on JavaFX or JDBC. Application code will depend on the `TaskRepository` interface so the persistence implementation remains replaceable and testable.

## Task storage

`SQLiteTaskRepository` implements saving, loading, updating, and deleting tasks.
Queries use prepared statements, and storage failures are reported through
`PersistenceException`. Updating a missing task fails; deleting a missing task
is harmless. Task lists use creation time and ID for a consistent order.

Repository tests use a temporary database, so running them does not change your
personal tasks. Run the full suite with `mvn clean test`.

## Planned next steps

Next comes the application service for validation and task status changes, followed
by the Today, All Tasks, and Completed screens.
