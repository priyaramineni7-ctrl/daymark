# Checking the task workflow

Run the regular tests with Java 21 and Maven:

```shell
mvn clean test
```

On a machine with a desktop, include the JavaFX workflow tests:

```shell
mvn clean test "-Ddaymark.uiTests=true"
```

The second command opens and closes test windows. It drives JavaFX controls on
the application thread and waits for storage operations to finish. Each test uses
its own temporary SQLite database, so it doesn't change your personal tasks.
The desktop tests are skipped by default so the regular build can run without a display.

The workflow checks cover:

- Creating a task and editing its title, notes, due date, and priority.
- Clearing optional fields and cancelling an edit without saving.
- Keeping validation errors and failed saves in the form, with the draft intact.
- Completing and reopening tasks, including editing a completed task.
- Updating search results and view membership after an edit.
- Reopening the browser and loading the saved task from SQLite.
- Recovering from a missing task, failed load, or failed status change.
- Blocking duplicate saves and window closure while a write is pending.

Some tests deliberately simulate storage errors. Their error logs are expected;
use the Maven test summary to determine whether the checks passed.

## Visual check

Launch the application with `mvn javafx:run`. This uses your normal task database.
For a separate test database in PowerShell, run these commands in a fresh terminal:

```powershell
$env:LOCALAPPDATA = Join-Path ([System.IO.Path]::GetTempPath()) ('daymark-check-' + [guid]::NewGuid())
mvn javafx:run
```

Close that terminal afterward to discard its environment override.

Check that long titles and notes wrap at the minimum window size, that all form
buttons remain readable, and that calendar selection and keyboard focus work.
The automated workflow tests exercise control events, not physical mouse and
keyboard input, so this visual pass still matters.

Today includes overdue active tasks. All Tasks includes future and undated tasks
as well as completed tasks. Refresh reloads the database and recalculates Today
using the current local date. Saving an edit keeps the current view and search;
the feedback explains if the saved task no longer matches them.
