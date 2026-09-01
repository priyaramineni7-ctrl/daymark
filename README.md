# Daymark

A desktop task planner built with Java 21, JavaFX and SQLite. Tasks live in a local database
file — there is no account, no sync, and nothing leaves the machine.

It started as the terminal program still kept in [`legacy-console`](legacy-console). That
version stored tasks as comma-separated lines, so any title containing a comma came back
mangled on the next load. Fixing that properly is what turned it into this.

## Requirements

- JDK 21
- Maven 3.6.3 or newer

## Run

```shell
mvn javafx:run
```

## Test

```shell
mvn clean test
```

## What it does

Three views in the sidebar — Today (overdue plus due today), Active, and Completed. You can
create, edit, complete, restore and delete tasks, each with an optional description, due date
and priority. The search box filters the current view by title and description.

## Where the data lives

On Windows: `%LOCALAPPDATA%\Daymark\daymark.db`. If `LOCALAPPDATA` isn't set it falls back to
`~/.daymark/daymark.db`. Nothing is written inside the repo, so task data can't be committed
by accident. The database runs in WAL mode, so you'll see `daymark.db-wal` and `daymark.db-shm`
alongside it while the app is open.

## Known rough edges

- **Everything loads at once.** `findAll()` reads every task and the filtering, search and
  sorting all happen in memory in `TaskListModel`. That's fine for a personal task list and
  would need real SQL queries somewhere in the low thousands.
- **Delete is permanent.** There's a confirmation dialog and that's it — no undo, no trash.
- **The dashboard has no tests.** The presentation logic is pulled out into `TaskListModel`
  precisely so it can be tested without a JavaFX toolkit, but the wiring in `TaskDashboard`
  itself is only ever checked by hand.
- **One theme.** The palette is hard-coded in `daymark.css`; there's no dark mode.

## Layout

```text
com.daymark.app          Application entry point
com.daymark.domain       Task model and domain types
com.daymark.repository   Persistence contract
com.daymark.persistence  SQLite schema, connections, repository implementation
com.daymark.service      Validation and task workflows
com.daymark.ui           JavaFX views and presentation logic
```

The domain package doesn't depend on JavaFX or JDBC, and everything above the repository works
through the `TaskRepository` interface rather than talking to SQLite directly.
