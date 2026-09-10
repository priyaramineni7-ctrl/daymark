package com.daymark.ui;

public enum TaskView {
    TODAY("Today", "A little focus for the day ahead.", "You're all caught up", "No active tasks are due today or overdue."),
    ALL_TASKS("All Tasks", "Everything you're working on, in one place.", "Start with one task", "Add something you'd like to get done."),
    COMPLETED("Completed", "A record of what you've finished.", "Room for your progress", "Tasks you finish will appear here.");

    private final String title;
    private final String description;
    private final String emptyTitle;
    private final String emptyDescription;

    TaskView(String title, String description, String emptyTitle, String emptyDescription) {
        this.title = title;
        this.description = description;
        this.emptyTitle = emptyTitle;
        this.emptyDescription = emptyDescription;
    }

    public String title() { return title; }
    public String description() { return description; }
    public String emptyTitle() { return emptyTitle; }
    public String emptyDescription() { return emptyDescription; }
}
