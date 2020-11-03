package centralized;

import logist.agent.Agent;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Solution {
    public static int NUM_TASKS;
    public static int NUM_VEHICLES;
    public static int MAX_TIME;
    public static Topology topology;
    public static Agent agent;

    private MyAction[] nextActions;
    private int[] time;
    private Vehicle[] vehicle;
    private int[][] capacities;

    public Solution() {
        nextActions = new MyAction[NUM_VEHICLES + NUM_TASKS * 2];
        time = new int[MAX_TIME];
        vehicle = new Vehicle[NUM_TASKS];
        capacities = new int[NUM_VEHICLES][MAX_TIME];
    }

    public Solution(Solution original) {
        nextActions = original.nextActions.clone();
        time = original.time.clone();
        vehicle = original.vehicle.clone();
        capacities = original.capacities.clone();
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
                } else break;
            }
        }
    }

    public double computeCost() {
        double cost = 0;
        for (Vehicle v : agent.vehicles()) {
            cost += computeVehicleDistance(v) * v.costPerKm();
        }
        return cost;
    }

    private double computeVehicleDistance(Vehicle v) {
        double distance = 0;
        MyAction ti = getNextAction(v);
        if (ti != null) {
            distance += v.getCurrentCity().distanceTo(ti.getActionCity());
            MyAction tj;
            while (true) {
                tj = getNextAction(ti);
                if (tj != null) {
                    distance += ti.getActionCity().distanceTo(tj.getActionCity());
                    ti = tj;
                } else break;
            }
        }
        return distance;
    }

    public void changingActionOrder(Vehicle vi, int tIdx1, int tIdx2) {
        MyAction tPre1 = null;
        MyAction t1 = getNextAction(vi);
        int count = 1;
        while (count < tIdx1) {
            tPre1 = t1;
            t1 = getNextAction(t1);
            count++;
        }
        MyAction tPost1 = getNextAction(t1);

        MyAction tPre2 = t1;
        MyAction t2 = getNextAction(tPre2);
        count++;
        while (count < tIdx2) {
            tPre2 = t2;
            t2 = getNextAction(t2);
            count++;
        }
        MyAction tPost2 = getNextAction(t2);

        if (tPost1 == t2) {
            if (tPre1 == null) setNextAction(vi, t2);
            else setNextAction(tPre1, t2);
            setNextAction(t2, t1);
            setNextAction(t1, tPost2);
        } else {
            if (tPre1 == null) setNextAction(vi, t2);
            else setNextAction(tPre1, t2);
            if (tPre2 == null) setNextAction(vi, t1);
            else setNextAction(tPre2, t1);
            setNextAction(t2, tPost1);
            setNextAction(t1, tPost2);
        }
    }
    
    public void moveAction(MyAction t, Vehicle v1, Vehicle v2) {
    	//remove MyAction from v1
    	MyAction ti = getNextAction(v1);
    	if (ti.equals(t)) setNextAction(v1, getNextAction(ti));
    	else {
            while (ti!=null) {
                if (!getNextAction(ti).equals(t)) ti = getNextAction(ti);
                else {
                    setNextAction(ti, getNextAction(getNextAction(ti)));
                    break;
                }
            }
        }

    	//Insert it in v2
    	setTaskVehicle(t.getTask(), v2);
    	MyAction copy = getNextAction(v2);
    	setNextAction(v2, t);
    	setNextAction(t, copy);
    	updateTime(v2);
    }

    public void moveTask(Task task, Vehicle v1, Vehicle v2){
        MyAction pickup = new MyAction(task, true);
        MyAction deliver = new MyAction(task, false);
        moveAction(deliver, v1, v2);
        moveAction(pickup, v1, v2);
    }


    public int getNumberOfActions(Vehicle v){
        int length = 0;
        MyAction a = getNextAction(v);
        while (a!=null){
            length++;
            a = getNextAction(a);
        }
        return length;
    }


    public int[] getTime() {
		return time;
	}

	public void setTime(int[] time) {
		this.time = time;
	}

	public MyAction getNextAction(Vehicle vehicle) {
        return nextActions[vehicle.id()];
    }

    public MyAction getNextAction(MyAction action) {
        return nextActions[action.getId()];
    }

    public void setNextAction(Vehicle vehicle, MyAction nextAction) {
        nextActions[vehicle.id()] = nextAction;
    }

    public void setNextAction(MyAction action, MyAction nextAction) {
        nextActions[action.getId()] = nextAction;
    }

    public int getCapacity(Vehicle vehicle, int time) {
        return capacities[vehicle.id()][time];
    }

    public void setCapacity(Vehicle vehicle, int time, int capacity) {
        capacities[vehicle.id()][time] = capacity;
    }

    public Vehicle getResponsibleVehicle(Task task) {
        return vehicle[task.id];
    }

    public void setTaskVehicle(Task task, Vehicle v) {
        vehicle[task.id] = v;
    }

    public int getActionTime(MyAction action) {
        return time[action.getId()];
    }

    public void setActionTime(MyAction action, int t) {
        time[action.getId()] = t;
    }

    public boolean hasActions(Vehicle v){
        return getNextAction(v) != null;
    }

    public void printActions(){
        for(Vehicle v: agent.vehicles()){
            MyAction a = getNextAction(v);
            System.out.print("vehicle "+v.id()+ ": ");
            while (a!=null){
                System.out.print(a+" ");
                a = getNextAction(a);
            }
            System.out.println();
        }
    }

    public List<Plan> convertToPlan() {
        List<Plan> plans = new ArrayList<Plan>();
        for (Vehicle v : agent.vehicles()) {
            City previousCity = v.getCurrentCity();
            Plan plan = new Plan(previousCity);
            MyAction a = getNextAction(v);
            City currentCity;
            while (a != null) {
                currentCity = a.getActionCity();

                if (!currentCity.equals(previousCity)) {
                    for (City step : previousCity.pathTo(currentCity)) {
                        plan.appendMove(step);
                    }
                }

                if (a.isPickup()) {
                    plan.appendPickup(a.getTask());
                } else {
                    plan.appendDelivery(a.getTask());
                }

                previousCity = currentCity;
                a = getNextAction(a);
            }

            plans.add(plan);
        }
        return plans;
    }
}
