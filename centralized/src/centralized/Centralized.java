package centralized;

//the list of imports

import deliberative.Deliberative;
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
import logist.topology.Topology.City;

import java.io.File;
import java.util.*;

import centralized.MyAction;
import centralized.Solution;

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

        Solution.MAX_TIME = 100;//TODO comment determiner
        Solution.NUM_TASKS = agent.getTasks().size();
        Solution.NUM_VEHICLES = agent.vehicles().size();
        Solution.topology = topology;
        Solution.agent = agent;

        Solution A = selectInitialSolution(vehicles, tasks);
        System.out.println("initial cost:" + A.computeCost());
        Solution A_old;

        Solution best = A;
        double bestCost = A.computeCost();

        do {
            A_old = A;
            ArrayList<Solution> N = chooseNeighbours(A_old);
//            for(Solution n: N){
//                System.out.println(n.computeCost());
//            }

            A = localChoice(N, A_old);
//            A.printActions();

            if(A.computeCost() < bestCost){
                best = A;
                bestCost = A.computeCost();
            }


        } while (System.currentTimeMillis() - time_start < timeout_plan * 0.9);

        List<Plan> plans = best.convertToPlan();

        for (Task task : tasks) {
            System.out.println(task);
        }

        Vehicle bestVehicle = vehicles.stream().max(Comparator.comparingInt(Vehicle::capacity)).get();

//        System.out.println("best plan (A*):");
//        System.out.println(Deliberative.aStarPlan(bestVehicle, tasks));
        for (int i = 0; i < vehicles.size(); i++) {
            System.out.println(plans.get(i));
            System.out.println(plans.get(i).totalDistance() * vehicles.get(i).costPerKm());
        }

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");

        return plans;
    }


    private Solution selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
        //assign all the tasks to the vehicle with biggest capacity
        Solution solution = new Solution();
        Vehicle bestVehicle = vehicles.stream().max(Comparator.comparingInt(Vehicle::capacity)).get();
        MyAction previous = null;
        for (Task task : tasks) {
        	solution.setTaskVehicle(task, bestVehicle);
        	
        	MyAction pickup = new MyAction(task, true);
        	if (previous == null) solution.setNextAction(bestVehicle, pickup);
        	else solution.setNextAction(previous, pickup);
            solution.updateTime(bestVehicle);
            
            
            MyAction delivery = new MyAction(task, false);
            solution.setNextAction(pickup, delivery);
            solution.updateTime(bestVehicle);
            
            previous = delivery;
        }
        return solution;
    }

    //constraints
    private boolean checkCapacity(Solution solution) {
        for (Vehicle vehicle : agent.vehicles()) {
            double currentCapacity = 0;
            double maxCapacity = vehicle.capacity();

            MyAction a = solution.getNextAction(vehicle);
            while (a!=null){
                double weight = a.getTask().weight;

                if(a.isPickup()) currentCapacity += weight;
                else currentCapacity -= weight;

                if(currentCapacity > maxCapacity) return false;

                a = solution.getNextAction(a);
            }

        }
        return true;
    }


    //constraints
//    private boolean checkCapacity(Solution solution) {
//    	for (Vehicle vehicle : solution.getVehicle()) {
//    		for (int time : solution.getTime()) {
//    			if (solution.getCapacity(vehicle, time) > vehicle.capacity()) return false;
//    		}
//    	}
//    	return true;
//    }
    
    private boolean checkOrder(Solution solution) {
    	for (Vehicle vehicle : agent.vehicles()) {
    		List<Task> treated = new ArrayList<Task>();
    		MyAction myaction = solution.getNextAction(vehicle);
    		while (myaction!= null) {
    			if (!treated.contains(myaction.getTask())) {
    				if (myaction.isDelivery()) return false;
    				else treated.add(myaction.getTask());
    			}
    			myaction = solution.getNextAction(myaction);
    		}
    	}
		return true;
    }

    private ArrayList<Solution> chooseNeighbours(Solution A_old) {
        ArrayList<Solution> neighbours = new ArrayList<>();

        //select random vehicle with tasks
        List<Vehicle> randomVehicles = new ArrayList<>(agent.vehicles());
        Collections.shuffle(randomVehicles);
        Vehicle vi = randomVehicles.stream().filter(A_old::hasActions).findFirst().orElseThrow();

        //add all permutation of 2 actions as neighbour (if valid)
        int length = A_old.getNumberOfActions(vi);
        for(int tIdx1 = 1; tIdx1 < length;tIdx1++){
            for(int tIdx2 = tIdx1+1; tIdx2 < length + 1;tIdx2++){
                Solution n = new Solution(A_old);
                n.changingActionOrder(vi, tIdx1, tIdx2);
                if(checkOrder(n) && checkCapacity(n)){
//                    n.printActions();
                    neighbours.add(n);
                }
            }
        }


        return neighbours;
    }

    private Solution localChoice(ArrayList<Solution> N, Solution A_old) {
        Solution chosenNeighbour;
        double old_cost = A_old.computeCost();
        double bestCost = old_cost;
//        System.out.println(bestCost);

        ArrayList<Solution> bestSols = new ArrayList<>();
        for(Solution sol:N){
            double cost = sol.computeCost();
            if(cost < bestCost){
                bestCost = cost;
                bestSols = new ArrayList<>();
                bestSols.add(sol);
            }
            else if (cost == bestCost){
                bestSols.add(sol);
            }
        }

        if(bestSols.isEmpty()){
            System.out.println("no best neighbour");
            return A_old;
        }

        //return best with probability p
        if (Math.random() < p) return bestSols.get(new Random().nextInt(bestSols.size()));
        //return random with probability 1-p
        else return N.get(new Random().nextInt(N.size()));

//        if (Math.random() > p) return N.stream().min(Comparator.comparingDouble(Solution::computeCost)).orElseThrow();
//        else return N.get(new Random().nextInt(N.size()));
    }

}
