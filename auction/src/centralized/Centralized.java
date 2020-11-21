package centralized;

//the list of imports

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.io.File;
import java.util.*;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 */
@SuppressWarnings("unused")
public class Centralized implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private double p = .7;
    private Random rand = new Random();

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;

    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();

        Solution best = null;
        double bestCost = Double.POSITIVE_INFINITY;

        Solution A = selectInitialSolution(vehicles, tasks);
        System.out.println("initial cost:" + A.computeCost());
        Solution A_old;
        A.printActions();
        do {
            A_old = A;
            ArrayList<Solution> N = chooseNeighbours(A_old);
            A = localChoice(N, A_old);
//            A.printActions();

            if (A.computeCost() < bestCost) {
                best = A;
                bestCost = A.computeCost();
            }
        } while ((System.currentTimeMillis() - time_start) < 0.9 * timeout_plan);

        System.out.println("Solution:");
        List<Plan> plans = best.convertToPlans();
        best.printActions();
        System.out.println(best.computeCost());

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");

        return plans;
    }


//    private Solution selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
//        //assign all the tasks to the vehicle with biggest capacity
//        Solution solution = new Solution();
//        Vehicle bestVehicle = vehicles.stream().max(Comparator.comparingInt(Vehicle::capacity)).get();
//
//        MyAction previous = null;
//        for (Task task : tasks) {
//        	solution.setTaskVehicle(task, bestVehicle);
//
//        	MyAction pickup = new MyAction(task, true);
//        	if (previous == null) solution.setNextAction(bestVehicle, pickup);
//        	else solution.setNextAction(previous, pickup);
//            solution.updateTime(bestVehicle);
//
//
//            MyAction delivery = new MyAction(task, false);
//            solution.setNextAction(pickup, delivery);
//            solution.updateTime(bestVehicle);
//
//            previous = delivery;
//        }
//        return solution;
//    }

    /**
     * Returns the initial solution where the tasks are distributed evenly among the vehicles
     * if a task is too big for a vehicle, it is given to the biggest one instead
     */
    private Solution selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
        Solution solution = new Solution();
        Vehicle biggestVehicle = vehicles.stream().max(Comparator.comparingInt(Vehicle::capacity)).get();

        for (Vehicle potentialVehicle : vehicles) {
            MyAction previous = null;
            //filter the tasks evenly
            Iterator<Task> vehicleTasks = tasks.stream().filter(task -> task.id % vehicles.size() == potentialVehicle.id()).iterator();
            while (vehicleTasks.hasNext()) {
                Task task = vehicleTasks.next();
                Vehicle v;
                if (task.weight <= potentialVehicle.capacity()) v = potentialVehicle;
                else v = biggestVehicle;

                MyAction pickup = new MyAction(task, true);
                if (previous == null) solution.setNextAction(v, pickup);
                else solution.setNextAction(previous, pickup);

                MyAction delivery = new MyAction(task, false);
                solution.setNextAction(pickup, delivery);

                previous = delivery;
            }
        }

        return solution;
    }

    /**
     * returns similar solutions where a task was moved from a vehicle
     * and similar solutions where two actions have been swapped
     */
    private ArrayList<Solution> chooseNeighbours(Solution A_old) {
        ArrayList<Solution> neighbours = new ArrayList<>();
        List<Vehicle> randomVehicles = new ArrayList<>(agent.vehicles());
        Collections.shuffle(randomVehicles);

        Vehicle vi = randomVehicles.stream().filter(A_old::hasActions).findFirst().orElseThrow();

        randomVehicles.remove(vi);
        for (Task t : A_old.getTasks(vi)) {
            for (Vehicle vj : randomVehicles) {
                Solution n = new Solution(A_old);
                n.moveTask(t, vi, vj);
                if (n.checkCapacity() && n.checkOrder()) {
                    neighbours.add(n);
                }
                if (t.weight <= vj.capacity()) {
                    int length = A_old.getNumberOfActions(vj);
                    for (int tIdx1 = 1; tIdx1 < length; tIdx1++) {
                        for (int tIdx2 = tIdx1 + 1; tIdx2 < length + 1; tIdx2++) {
                            Solution n2 = new Solution(n);
                            n2.swapActionOrder(vj, 1, tIdx1);
                            n2.swapActionOrder(vj, 2, tIdx2);
                            if (n2.checkCapacity() && n2.checkOrder()) {
                                neighbours.add(n);
                            }
                        }
                    }
                }
            }
        }
        //add all permutation of 2 actions as neighbour (if valid)
        int length = A_old.getNumberOfActions(vi);
        for (int tIdx1 = 1; tIdx1 < length; tIdx1++) {
            for (int tIdx2 = tIdx1 + 1; tIdx2 < length + 1; tIdx2++) {
                Solution n = new Solution(A_old);
                n.swapActionOrder(vi, tIdx1, tIdx2);
                if (n.checkCapacity() && n.checkOrder()) {
                    neighbours.add(n);
                }
            }
        }
        return neighbours;
    }

    /**
     * returns a random lowest cost element with probability p,
     * and a random element with probability 1-p
     */
    private Solution localChoice(ArrayList<Solution> N, Solution A_old) {
        if (N.isEmpty()) return A_old;

        Solution chosenNeighbour;
        double bestCost = A_old.computeCost();

        ArrayList<Solution> bestSols = new ArrayList<>();
        for (Solution sol : N) {
            double cost = sol.computeCost();
            if (cost < bestCost) {
                bestCost = cost;
                bestSols = new ArrayList<>();
                bestSols.add(sol);
            } else if (cost == bestCost) {
                bestSols.add(sol);
            }
        }


        //return best with probability p
        if (Math.random() < p) {
            if (bestSols.isEmpty()) return A_old;
            return bestSols.get(rand.nextInt(bestSols.size()));
        }
        //return random with probability 1-p
        else return N.get(rand.nextInt(N.size()));
    }

}
