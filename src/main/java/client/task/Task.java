package client.task;

import client.option.OptionPool;

import java.util.ArrayList;
import java.util.List;

public class Task implements Runnable {
    final OptionPool options;
    private final List<Task> subTasks = new ArrayList<>();

    Task(OptionPool options) {
        this.options = options;
    }

    Task(OptionPool options, Task subTask) {
        this(options);
        subTasks.add(subTask);
    }

    @Override
    public void run() {
        subTasks.forEach(Task::run);
    }
}
