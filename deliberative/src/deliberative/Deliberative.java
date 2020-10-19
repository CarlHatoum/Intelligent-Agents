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

import java.util.*;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class Deliberative implements DeliberativeBehavior {

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
        System.out.println("generating A* plan");
        long startTime = System.nanoTime();

        City currentCity = vehicle.getCurrentCity();

        Node initialNode = new Node(null, currentCity, vehicle.getCurrentTasks(), tasks, vehicle.capacity());

        ArrayList<Node> Q = new ArrayList<>(); //queue of nodes to be processed
        ArrayList<Node> C = new ArrayList<>(); //processed nodes

        Q.add(initialNode);

        Plan plan = new Plan(currentCity);
        while (!Q.isEmpty()) {
            //pop first node in queue
            Node n = Q.remove(0);

            if (n.isGoalState()) {
                //found the solution
                //generate the action List to get to the state n
                plan = generatePlan(currentCity, n);
                break;
            }

            if (!C.contains(n) || (C.contains(n) && n.getGCost() + h(n) < getBestFCost(C, n))) {
                C.add(n);

                //add all the children of n to the queue
                Q.addAll(n.generateChildren());

                //sort according to the estimated total cost
                Q.sort(Comparator.comparingDouble(
                        x -> x.getGCost() + h(x)
                ));
            }
        }
        System.out.println("finished A*");
        System.out.println(plan);
        System.out.println("time to compute: " + (System.nanoTime() - startTime) / 1e6 + "ms");
        System.out.println("visited nodes:" + C.size());
        return plan;
    }

    /**
     * return node in list with lowest fcost
     */
    private double getBestFCost(ArrayList<Node> list, Node node) {
        //TODO simplify this function
        ArrayList<Node> copies = new ArrayList<>();
        for (Node n : list) {
            if (n.equals(node)) {
                copies.add(n);
            }
        }
        return copies.stream()
                .mapToDouble(node2 -> node2.getGCost() + h(node2))
                .min()
                .orElseThrow(NoSuchElementException::new);
    }

    /**
     * heuristic: the distance of the longest task if there are any left
     */
    private double h(Node n) {
        if (n.getRemainingTasks().isEmpty()) {
            if (n.getCarriedTasks().isEmpty()) {
                return 0.0;
            }
            //return the distance from the current city to the city of the furthest carried task
            else
                return n.getCarriedTasks().stream().mapToDouble(task -> n.getCity().distanceTo(task.deliveryCity)).max().orElseThrow();
        }
        //return the distance of the longest remaining task
        else
            return n.getRemainingTasks().stream().mapToDouble(task -> task.pickupCity.distanceTo(task.deliveryCity)).max().orElseThrow();
    }


    private Plan generatePlan(City initialCity, Node endNode){
        Plan plan = new Plan(initialCity);
        ArrayList<Node> path = new ArrayList<>();
        Node currentNode = endNode;
        do {
            path.add(0, currentNode);
            currentNode = currentNode.getParent();
        } while (currentNode.getParent()!=null);
        path.add(0, currentNode);

        for(int i = 0;i<path.size()-1; i++){
            Node node = path.get(i);
            Node nextNode = path.get(i+1);

            TaskSet carriedTasks = node.getCarriedTasks().clone();
            TaskSet carriedTasksNext = nextNode.getCarriedTasks().clone();

            if(!node.getCity().equals(nextNode.getCity())){
                //if the city changed -> move
                plan.appendMove(nextNode.getCity());
            }
            else if(carriedTasksNext.size() < carriedTasks.size()){
                //lost a task -> delivery
                carriedTasks.removeAll(carriedTasksNext);
                Task task = carriedTasks.iterator().next();
                plan.appendDelivery(task);
            }
            else if(node.getCarriedTasks().size() < nextNode.getCarriedTasks().size()){
                //gained a task -> pickup
                carriedTasksNext.removeAll(carriedTasks);
                Task task = carriedTasksNext.iterator().next();
                plan.appendPickup(task);
            }
        }
        return plan;
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
        System.out.println("plan cancelled");
        if (!carriedTasks.isEmpty()) {
            // This cannot happen for this simple agent, but typically
            // you will need to consider the carriedTasks when the next
            // plan is computed.
        }
    }
}
