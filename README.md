# Daymark

Daymark is a desktop task manager designed to make daily planning feel simple and focused. It is built with JavaFX and stores data locally with SQLite, keeping the application fast, private, and usable without an internet connection.

The project began as a terminal-based Java program and is now being rebuilt as a full desktop application. The original console version remains in [`legacy-console`](legacy-console) as a record of where the project started.

## Current capabilities

The application currently includes the core technical foundation:

- A launchable JavaFX desktop shell
- An immutable task domain model
- Priority and task-status types
- A repository interface that separates application logic from storage
- Automatic SQLite database and schema initialization
- Local database safety settings, including foreign keys and a busy timeout
- Automated tests for the domain model and database setup

Task-management screens and workflows are still under development.

## Technology

- **Java 21** — application language and runtime
- **JavaFX 21** — desktop user interface
- **SQLite** — local data storage
- **Maven** — dependency management and build tooling
- **JUnit** — automated testing

## Getting started

### Requirements

- JDK 21
- Maven 3.6.3 or newer

### Run the application

```shell
mvn javafx:run
```

### Run the tests

```shell
mvn clean test
```

## Architecture

Daymark keeps its domain, persistence, and interface concerns separate:

```text
JavaFX interface
       |
Application services
       |
TaskRepository
       |
SQLite persistence
```

The domain model has no dependency on JavaFX or JDBC. Higher-level application code works through the `TaskRepository` contract instead of depending directly on SQLite, which keeps the business logic easier to test and allows the storage implementation to evolve independently.

The main source packages are organized by responsibility:

```text
com.daymark.app          Application entry point
com.daymark.domain       Task model and domain types
com.daymark.repository   Persistence contracts
com.daymark.persistence  SQLite configuration and schema setup
```

## Local data

On Windows, Daymark stores its database at:

```text
%LOCALAPPDATA%\Daymark\daymark.db
```

If `LOCALAPPDATA` is unavailable, the application falls back to:

```text
~/.daymark/daymark.db
```

The database is kept outside the repository so personal task data cannot be committed accidentally.

## Planned features

- Create and edit tasks with due dates and priorities
- A focused view for overdue tasks and tasks due today
- Search, filtering, and sorting
- Complete, restore, and delete workflows
- Clear empty, validation, and error states
- Keyboard-friendly navigation
