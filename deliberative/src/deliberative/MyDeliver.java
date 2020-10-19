package deliberative;

import logist.task.Task;

public class MyDeliver extends MyAction {
    private Task task;

    public MyDeliver(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    public String toString() {
        return "deliver " + task.id;
    }
}
