# Task Manager Prototype

This repository contains the original console prototype that later became **Daymark**, a Java desktop task-planning application.

The prototype was built to practice core Java fundamentals: object-oriented design, collections, file input/output, user input, and basic application control flow. It runs entirely in the terminal and stores tasks in a local text file.

## Features

- Add a task with a title, description, due date, and priority.
- View all saved tasks in a numbered list.
- Mark a task as completed.
- Delete a task by its list number.
- Save tasks to `tasks.txt` and reload them the next time the program runs.

## Run the prototype

From the repository root:

```shell
cd TaskManager
javac *.java
java Main
```

Choose an option from the numbered menu and follow the terminal prompts. Use **Save and Exit** to persist changes.

## Project structure

- `Main.java` provides the terminal menu and reads user input.
- `Task.java` represents an individual task.
- `TaskManager.java` manages the in-memory task list.
- `FileHandler.java` loads and saves task data.

## Prototype limitations

This is intentionally the original learning-stage version. Dates and priorities are stored as text, input validation is minimal, and comma-separated persistence does not safely handle commas inside task content. These limitations provide the starting point for the application's later architectural and user-interface improvements.
