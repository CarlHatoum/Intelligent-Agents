package centralized;

import logist.task.Task;
import logist.topology.Topology.City;

import java.util.Objects;

public class MyAction {
    private boolean isPickup;
    private Task task;

    public MyAction(Task task, boolean isPickup) {
        this.task = task;
        this.isPickup = isPickup;
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

    public City getActionCity() {
        if (isPickup) {
            return task.pickupCity;
        } else return task.deliveryCity;
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

    @Override
    public String toString() {
        if (isPickup) {
            return "Pickup " + task.id;
        } else {
            return "Deliver " + task.id;
        }
    }
}
