# Daymark

Daymark is a desktop task manager designed to make daily planning feel simple and focused. It is built with JavaFX and stores data locally with SQLite, keeping the application fast, private, and usable without an internet connection.

The project began as a terminal-based Java program and is now being rebuilt as a full desktop application. The original console version remains in [`legacy-console`](legacy-console) as a record of where the project started.

## Current capabilities

The application currently includes:

- A polished JavaFX dashboard designed around daily planning
- Today, upcoming, active, and completed task views
- Search, task counts, clear empty states, and keyboard shortcuts
- Create, edit, complete, restore, and delete workflows
- An immutable task domain model
- Priority and task-status types
- A repository interface that separates application logic from storage
- Complete SQLite persistence for creating, reading, updating, and deleting tasks
- A validated application service for task creation, editing, completion, restoration, and deletion
- Automatic SQLite database and schema initialization
- Local database safety settings, including foreign keys and a busy timeout
- Automated tests across the domain, service, presentation, repository, and database layers

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
com.daymark.service      Validation and task workflows
com.daymark.ui           JavaFX views and presentation logic
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

## Ideas for future development

- User-selectable themes and accessibility preferences
- Recurring tasks and lightweight tags
- Custom sorting and saved filters
- Import and export tools
