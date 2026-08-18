import java.io.*;
import java.util.ArrayList;

public class FileHandler {

    private static final String FILE_NAME = "tasks.txt";

    public static void saveTasks(ArrayList<Task> tasks) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME));

            for (Task task : tasks) {
                writer.println(task.toFileString());
            }

            writer.close();

        } catch (IOException e) {
            System.out.println("Error saving tasks.");
        }
    }

    public static ArrayList<Task> loadTasks() {
        ArrayList<Task> tasks = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME));
            String line;

            while ((line = reader.readLine()) != null) {
                tasks.add(Task.fromFileString(line));
            }

            reader.close();

        } catch (IOException e) {
            System.out.println("No previous tasks found.");
        }

        return tasks;
    }
}
