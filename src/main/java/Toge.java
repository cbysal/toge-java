import client.TaskParser;
import client.task.Task;

public class Toge {
    private Toge() {
    }

    public static void main(String[] args) {
        TaskParser taskParser = new TaskParser(args);
        Task task = taskParser.getTask();
        task.run();
    }
}
