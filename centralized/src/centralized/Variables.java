package centralized;

import logist.simulation.Vehicle;
import logist.task.Task;

public class Variables {
    public static int NUM_TASK;
    public static int NUM_VEHICLE;
    public static int MAX_TIME;

    private MyAction[] nextActions;
    private int[] time;
    private int[] vehicle;
    private int[][] capacities;

    public Variables(){
        nextActions = new MyAction[NUM_TASK*2];
        time = new int[MAX_TIME];
        vehicle = new int[NUM_VEHICLE];
        capacities = new int[NUM_VEHICLE][MAX_TIME];
    }

    public MyAction getNextAction(MyAction action){
        return nextActions[action.getId()];
    }

    public void setNextAction(MyAction action, MyAction nextAction){
        nextActions[action.getId()] = nextAction;
    }

    public int getCapacity(Vehicle vehicle, int time) {
        return capacities[vehicle.id()][time];
    }

    public void setCapacity(Vehicle vehicle, int time, int capacity) {
         capacities[vehicle.id()][time] = capacity;
    }

    public int getVehicleId(Task task){
        return vehicle[task.id];
    }

    public void setVehicle(Task task, Vehicle v){
        vehicle[task.id] = v.id();
    }

    public int getActionTime(MyAction action){
        return time[action.getId()];
    }

    public void setActionTime(MyAction action, int t){
        time[action.getId()] = t;
    }

}
