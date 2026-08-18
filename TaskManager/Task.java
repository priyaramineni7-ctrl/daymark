public class Task {

    private String title;
    private String description;
    private String dueDate;
    private String priority;
    private boolean completed;

    public Task(String title, String description, String dueDate, String priority, boolean completed) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.completed = completed;
    }

    public void markComplete() {
        completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String toFileString() {
        return title + "," + description + "," + dueDate + "," + priority + "," + completed;
    }

    public static Task fromFileString(String line) {
        String[] parts = line.split(",");
        return new Task(parts[0], parts[1], parts[2], parts[3], Boolean.parseBoolean(parts[4]));
    }

    public String toString() {
        return title + " | " + description + " | Due: " + dueDate +
               " | Priority: " + priority + " | Completed: " + completed;
    }
}
