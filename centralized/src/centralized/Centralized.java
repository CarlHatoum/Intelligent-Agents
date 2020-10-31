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
import logist.topology.Topology.City;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    private double p = 0.4;

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
        System.out.println(A.convertToPlan());
        System.out.println("initial cost:" + A.computeCost());
        Solution A_old;

        do {
            A_old = A;
            ArrayList<Solution> N = chooseNeighbours(A_old);
            Solution bestNeighbour = localChoice(N);

            if (Math.random() > p) A = bestNeighbour;
            else A = A_old;

        } while (!terminationConditionMet());

        List<Plan> plans = A.convertToPlan();

        for (Task task : tasks) {
            System.out.println(task);
        }
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
        Solution solution = new Solution();
        Vehicle bestVehicle = vehicles.stream().max(Comparator.comparingInt(Vehicle::capacity)).get();

        //assign all the tasks to the vehicle with biggest capacity
        for (Task task : tasks) {
            MyAction pickup = new MyAction(task, true);
            solution.setNextAction(bestVehicle, pickup);
            solution.updateTime(bestVehicle);

            MyAction delivery = new MyAction(task, false);
            solution.setNextAction(bestVehicle, delivery);
            solution.updateTime(bestVehicle);

            solution.setNextAction(pickup, delivery);

            solution.setTaskVehicle(task, bestVehicle);
        }

        return solution;
    }
    
    //constraints
    private boolean checkCapacity(Solution solution) {
    	for (Vehicle vehicle : solution.getVehicle()) {
    		for (int time : solution.getTime()) {
    			if (solution.getCapacity(vehicle, time) > vehicle.capacity()) return false;
    		}
    	}
    	return true;
    }
    
    private boolean checkOrder(Solution solution) {
    	for (Vehicle vehicle : solution.getVehicle()) {
    		List<MyAction> actions = new ArrayList<MyAction>();
    		List<MyAction> treatedActions = new ArrayList<MyAction>();
    		MyAction myaction = solution.getNextAction(vehicle);
    		while (solution.getNextAction(myaction)!= null) {
    			actions.add(myaction);
    			myaction = solution.getNextAction(myaction);
    		}
    		for (MyAction action : actions) {
    			if (!treatedActions.contains(action)) {
    				if (action.isDelivery()) return false;
    				else treatedActions.add(action);
    			}
    		}
    	}
		return true;
    }

    private ArrayList<Solution> chooseNeighbours(Solution A_old) {
        //TODO
        return new ArrayList<Solution>(){{add(A_old);}};
    }

    private Solution localChoice(ArrayList<Solution> N) {
        return N.stream().max(Comparator.comparingDouble(Solution::computeCost)).get();
    }

    private boolean terminationConditionMet() {
        //TODO
        return true;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
}
