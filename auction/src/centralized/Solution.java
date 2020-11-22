package centralized;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class Solution {
    public static Topology topology;
    private List<Vehicle> agentVehicles;

    private HashMap<Vehicle, MyAction> nextActionsVehicle;
    private HashMap<MyAction, MyAction> nextActions;

    public Solution(List<Vehicle> agentVehicles) {
        this.agentVehicles = agentVehicles;
        nextActionsVehicle = new HashMap<>();
        nextActions = new HashMap<>();
    }

    public Solution(Solution original) {
        agentVehicles = original.agentVehicles;
        nextActionsVehicle = (HashMap<Vehicle, MyAction>) original.nextActionsVehicle.clone();
        nextActions = (HashMap<MyAction, MyAction>) original.nextActions.clone();
    }

    public void addNewTask(Task task) {
        //add new task at the end of first vehicle
        Vehicle vehicle = agentVehicles.get(0);

        MyAction a = getNextAction(vehicle);
        if (a == null) {
            setNextAction(vehicle, new MyAction(task, true));
            MyAction pickup = getNextAction(vehicle);
            setNextAction(pickup, new MyAction(task, false));
        } else {
            while (getNextAction(a) != null) {
                a = getNextAction(a);
            }

            setNextAction(a, new MyAction(task, true));
            MyAction pickup = getNextAction(a);
            setNextAction(pickup, new MyAction(task, false));
        }

    }

    /**
     * returns the cost of transport of the solution
     */
    public double computeCost() {
        double cost = 0;
        for (Vehicle v : agentVehicles) {
            cost += computeVehicleDistance(v) * v.costPerKm();
        }
        return cost;
    }

    /**
     * returns the distance travelled by the given vehicle
     */
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

    /**
     * swaps the order of two given actions of a vehicle
     */
    public void swapActionOrder(Vehicle vi, int tIdx1, int tIdx2) {
        //get the action and its neighbours at time tIdx1
        MyAction tPre1 = null;
        MyAction t1 = getNextAction(vi);
        int count = 1;
        while (count < tIdx1) {
            tPre1 = t1;
            t1 = getNextAction(t1);
            count++;
        }
        MyAction tPost1 = getNextAction(t1);

        //get the action and its neighbours at time tIdx2
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

    /**
     * moves an action from a vehicle to another
     */
    public void moveAction(MyAction t, Vehicle v1, Vehicle v2) {
        //remove MyAction from v1
        MyAction ti = getNextAction(v1);
        if (ti.equals(t)) setNextAction(v1, getNextAction(ti));
        else {
            while (getNextAction(ti) != null) {//TODO error??
                if (!getNextAction(ti).equals(t)) ti = getNextAction(ti);
                else {
                    setNextAction(ti, getNextAction(getNextAction(ti)));
                    break;
                }
            }
        }

        //Insert it in v2
        MyAction copy = getNextAction(v2);
        setNextAction(v2, t);
        setNextAction(t, copy);
    }

    /**
     * moves a task from a vehicle to another
     */
    public void moveTask(Task task, Vehicle v1, Vehicle v2) {
        MyAction pickup = new MyAction(task, true);
        MyAction deliver = new MyAction(task, false);
        moveAction(deliver, v1, v2);
        moveAction(pickup, v1, v2);
    }

    /**
     * converts the solution to a list of plans for each vehicle
     */
    public List<Plan> convertToPlans() {
        List<Plan> plans = new ArrayList<>();
        for (Vehicle v : agentVehicles) {
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

    /**
     * Returns whether the solution respects the capacity constraint
     */
    public boolean checkCapacity() {
        for (Vehicle vehicle : agentVehicles) {
            double currentCapacity = 0;
            double maxCapacity = vehicle.capacity();

            MyAction a = getNextAction(vehicle);
            while (a != null) {
                double weight = a.getTask().weight;

                if (a.isPickup()) currentCapacity += weight;
                else currentCapacity -= weight;

                if (currentCapacity > maxCapacity) return false;

                a = getNextAction(a);
            }

        }
        return true;
    }

    /**
     * Returns whether the solution has pickups before deliveries
     */
    public boolean checkOrder() {
        for (Vehicle vehicle : agentVehicles) {
            List<Task> treated = new ArrayList<Task>();
            MyAction myaction = getNextAction(vehicle);
            while (myaction != null) {
                if (!treated.contains(myaction.getTask())) {
                    if (myaction.isDelivery()) return false;
                    else treated.add(myaction.getTask());
                }
                myaction = getNextAction(myaction);
            }
        }
        return true;
    }

    /**
     * Returns the number of actions of given vehicle
     */
    public int getNumberOfActions(Vehicle v) {
        int length = 0;
        MyAction a = getNextAction(v);
        while (a != null) {
            length++;
            a = getNextAction(a);
        }
        return length;
    }

    /**
     * Returns the taks of given vehicle
     */
    public LinkedList<Task> getTasks(Vehicle v) {
        LinkedList<Task> tasks = new LinkedList<>();
        MyAction a = getNextAction(v);
        while (a != null) {
            tasks.add(a.getTask());
            a = getNextAction(a);
        }
        return tasks;
    }

    public MyAction getNextAction(Vehicle vehicle) {
        return nextActionsVehicle.get(vehicle);
    }

    public MyAction getNextAction(MyAction action) {
        return nextActions.get(action);
    }

    public void setNextAction(Vehicle vehicle, MyAction nextAction) {
        nextActionsVehicle.put(vehicle, nextAction);
    }

    public void setNextAction(MyAction action, MyAction nextAction) {
        nextActions.put(action, nextAction);
    }

    public boolean hasActions(Vehicle v) {
        return getNextAction(v) != null;
    }

    public void printActions() {
        for (Vehicle v : agentVehicles) {
            MyAction a = getNextAction(v);
            System.out.print("vehicle " + v.id() + ": ");
            while (a != null) {
                System.out.print(a + " ");
                a = getNextAction(a);
            }
            System.out.println();
        }
    }

    public List<Vehicle> getAgentVehicles() {
        return agentVehicles;
    }
}
