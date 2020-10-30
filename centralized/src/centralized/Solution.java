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

        MyAction tPre2 = null;
        MyAction t2 = getNextAction(vi);
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
