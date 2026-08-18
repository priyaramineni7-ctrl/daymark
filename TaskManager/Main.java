import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        TaskManager manager = new TaskManager();

        manager.setTasks(FileHandler.loadTasks());

        while (true) {

            System.out.println("\nTask Manager");
            System.out.println("1. Add Task");
            System.out.println("2. View Tasks");
            System.out.println("3. Complete Task");
            System.out.println("4. Delete Task");
            System.out.println("5. Save and Exit");

            System.out.print("Choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 1) {

                System.out.print("Title: ");
                String title = scanner.nextLine();

                System.out.print("Description: ");
                String description = scanner.nextLine();

                System.out.print("Due Date: ");
                String dueDate = scanner.nextLine();

                System.out.print("Priority (Low/Medium/High): ");
                String priority = scanner.nextLine();

                Task task = new Task(title, description, dueDate, priority, false);
                manager.addTask(task);

            } else if (choice == 2) {

                manager.displayTasks();

            } else if (choice == 3) {

                manager.displayTasks();
                System.out.print("Enter task number: ");
                int index = scanner.nextInt();

                manager.markTaskComplete(index);

            } else if (choice == 4) {

                manager.displayTasks();
                System.out.print("Enter task number: ");
                int index = scanner.nextInt();

                manager.deleteTask(index);

            } else if (choice == 5) {

                FileHandler.saveTasks(manager.getTasks());
                System.out.println("Tasks saved. Goodbye!");
                break;
            }
        }

        scanner.close();
    }
}
