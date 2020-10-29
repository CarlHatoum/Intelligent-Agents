package centralized;

import logist.simulation.Vehicle;
import logist.task.Task;

public class Variables {
    public static int NUM_TASKS;
    public static int NUM_VEHICLES;
    public static int MAX_TIME;

    private MyAction[] nextActions;
    private int[] time;
    private int[] vehicle;
    private int[][] capacities;

    public Variables() {
        nextActions = new MyAction[NUM_VEHICLES + NUM_TASKS * 2];
        time = new int[MAX_TIME];
        vehicle = new int[NUM_VEHICLES];
        capacities = new int[NUM_VEHICLES][MAX_TIME];
    }

    public void updateTime(Vehicle v) {
        MyAction ti = getNextAction(v);
        if (ti != null) {
            setActionTime(ti, 1);
            MyAction tj;
            while (true) {
                tj = getNextAction(ti);
                if (tj != null) {
                    setActionTime(tj, getActionTime(ti) + 1);
                    ti = tj;
                }
                else break;
            }
        }
    }

    public MyAction getNextAction(Vehicle vehicle) {
        return nextActions[vehicle.id()];
    }

    public MyAction getNextAction(MyAction action) {
        return nextActions[NUM_VEHICLES + action.getId()];
    }

    public void setNextAction(Vehicle vehicle, MyAction nextAction) {
        nextActions[vehicle.id()] = nextAction;
    }

    public void setNextAction(MyAction action, MyAction nextAction) {
        nextActions[NUM_VEHICLES + action.getId()] = nextAction;
    }


    public int getCapacity(Vehicle vehicle, int time) {
        return capacities[vehicle.id()][time];
    }

    public void setCapacity(Vehicle vehicle, int time, int capacity) {
        capacities[vehicle.id()][time] = capacity;
    }

    public int getVehicleId(Task task) {
        return vehicle[task.id];
    }

    public void setTaskVehicle(Task task, Vehicle v) {
        vehicle[task.id] = v.id();
    }

    public int getActionTime(MyAction action) {
        return time[action.getId()];
    }

    public void setActionTime(MyAction action, int t) {
        time[action.getId()] = t;
    }

}
