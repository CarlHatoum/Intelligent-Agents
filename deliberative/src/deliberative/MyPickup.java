package deliberative;

import logist.task.Task;

public class MyPickup extends MyAction {
    private Task task;

    public MyPickup(Task task) {
        this.task = task;
    }

    public String toString() {
        return "pickup " + task.id;
    }

    public Task getTask() {
        return task;
    }
}
