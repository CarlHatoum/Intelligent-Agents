package centralized;

import logist.task.Task;

import java.util.Objects;

public class MyAction {
    private boolean isPickup;
    private Task task;

    public MyAction(Task task) {
        this.task = task;
    }


    public boolean isPickup() {
        return isPickup;
    }

    public boolean isDelivery() {
        return !isPickup;
    }

    public Task getTask() {
        return task;
    }

    public int getId(){
        if(isPickup()){
            return getTask().id;
        }
        else return getTask().id + Variables.NUM_TASK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyAction action = (MyAction) o;
        return isPickup() == action.isPickup() &&
                Objects.equals(task, action.task);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isPickup(), task);
    }
}
