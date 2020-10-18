package deliberative;

/* import table */

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import rla.PolicyGenerator.MyAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class Deliberative implements DeliberativeBehavior {
	
	public abstract class MyAction {
    }

    public class MyMove extends MyAction {
        private final City destination;

        public MyMove(City destination) {
            this.destination = destination;
        }

        public City getDestination() {
            return destination;
        }

        public String toString() {
            return "moveTo" + destination;
        }
    }
    
    public class MyDeliver extends MyAction {
        public String toString() {
            return "deliver";
        }
    }

    enum Algorithm {BFS, ASTAR}

    /* Environment */
    Topology topology;
    TaskDistribution td;

    /* the properties of the agent */
    Agent agent;
    int capacity;

    /* the planning class */
    Algorithm algorithm;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {
        this.topology = topology;
        this.td = td;
        this.agent = agent;

        // initialize the planner
        int capacity = agent.vehicles().get(0).capacity();
        String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

        // Throws IllegalArgumentException if algorithm is unknown
        algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

        // ...
    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet tasks) {
        Plan plan;

        // Compute the plan with the selected algorithm.
        switch (algorithm) {
            case ASTAR:
                plan = aStarPlan(vehicle, tasks);
                break;
            case BFS:
                plan = bfsPlan(vehicle, tasks);
                break;
            default:
                throw new AssertionError("Should not happen.");
        }
        return plan;
    }

    private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {
        //TODO
        return null;
    }

    private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        Node initialNode = new Node(null, current);

        ArrayList<Node> Q = new ArrayList<>(); //queue of nodes to be processed
        ArrayList<Node> C = new ArrayList<>(); //processed nodes

        HashMap<Node, Double> gCost = new HashMap<>();

        Q.add(initialNode);

        while (!Q.isEmpty()) {
            Node n = Q.get(0);
            if (goalReached(n)) {
                Node currentNode = n;
                do {
                    //TODO add action to plan
                    currentNode = currentNode.getParent();
                } while (currentNode != initialNode);
                break;
            }

            if (!C.contains(n)) {
                C.add(n);
                ArrayList<Node> children = n.generateChildren();

                children.sort(Comparator.comparingDouble(
                        x -> x.getGCost() + h(x)
                ));

                Q.addAll(children);
            }
        }
        return plan;
    }

    private double h(Node n) {
        //heuristic
        //TODO
        return 0.0;
    }

    public boolean goalReached(Node currentNode) {
        //TODO
        return false;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity))
                plan.appendMove(city);

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path())
                plan.appendMove(city);

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {

        if (!carriedTasks.isEmpty()) {
            // This cannot happen for this simple agent, but typically
            // you will need to consider the carriedTasks when the next
            // plan is computed.
        }
    }
}
