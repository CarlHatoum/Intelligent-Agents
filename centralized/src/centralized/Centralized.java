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
import centralized.Variables;

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

        Variables.MAX_TIME = 100;//TODO comment determiner
        Variables.NUM_TASKS = agent.getTasks().size();
        Variables.NUM_VEHICLES = agent.vehicles().size();
        Variables.topology = topology;
        Variables.agent = agent;

        Variables A = selectInitialSolution(vehicles, tasks);
        Variables A_old;

        do {
            A_old = A;
            ArrayList<Variables> N = chooseNeighbours(A_old);
            A = localChoice(N);
        } while (!terminationConditionMet());

        List<Plan> plans = convertSolutionToPlan(A, vehicles, tasks);

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");

        return plans;
    }

    private Variables selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
    	Variables variables = new Variables();
    	Vehicle bestVehicle = vehicles.stream().max(Comparator.comparingInt(Vehicle::capacity)).get();
    	
    	//assign all the tasks to the vehicle with biggest capacity
    	for (Task task : tasks) {
    		MyAction pickup = new MyAction(task, true);
    		variables.setNextAction(bestVehicle, pickup);
    		variables.updateTime(bestVehicle);
    		
    		MyAction delivery = new MyAction(task, false);
    		variables.setNextAction(bestVehicle, delivery);
    		variables.updateTime(bestVehicle);
    		
    		variables.setNextAction(pickup, delivery);
    			
    		variables.setTaskVehicle(task, bestVehicle);    		
    	}
    	
        return variables;
    }

    private ArrayList<Variables> chooseNeighbours(Variables A_old) {
        //TODO
        return null;
    }

    private Variables localChoice(ArrayList<Variables> N) {
        //TODO
        return null;
    }

    private boolean terminationConditionMet() {
        //TODO
        return true;
    }

    private List<Plan> convertSolutionToPlan(Variables solution, List<Vehicle> vehicles, TaskSet tasks) {
        List<Plan> plans = new ArrayList<Plan>();
        //TODO
        Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);
        plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }
        return plans;
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
