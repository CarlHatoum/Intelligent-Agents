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
import java.util.function.ToDoubleFunction;

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

        long startTime = System.nanoTime();
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

        System.out.println(plan);
        System.out.println(plan.totalDistance());
        System.out.println("time to compute: " + (System.nanoTime() - startTime) / 1e6 + "ms");

        return plan;
    }

    private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {
    	System.out.println("generating BFS plan");

    	Map<Plan, Double> plans = new HashMap<>(); //map of plans and their costs
        City currentCity = vehicle.getCurrentCity();

        Node initialNode = new Node(null, currentCity, vehicle.getCurrentTasks(), tasks, vehicle.capacity());
        
        ArrayList<Node> Q = new ArrayList<>(); //queue of nodes to be processed
        ArrayList<Node> C = new ArrayList<>(); //processed nodes

        Q.add(initialNode);

        while (!Q.isEmpty()) {
        	//pop first node in queue
            Node n = Q.remove(0);
            
            if(n.isGoalState()) {
            	//found a plan
            	Plan plan = generatePlan(currentCity, n);
                plans.put(plan, n.getGCost());
            }
            //check if node was visited
            if (isLowestCostForState(C,n, node -> node.getGCost())) {
            	C.add(n);
            	//add all the children of n to the queue
                Q.addAll(n.generateChildren());
            }
             
        }
        if (plans.isEmpty()) {
            System.out.println("Error: no path found");
        }
        // get the optimal plan
        Plan plan = getOptimalPlan(plans);
        System.out.println("finished BFS");
        System.out.println("visited nodes:" + C.size());
        return plan;
    }

    //return the plan with lowest cost
    private Plan getOptimalPlan(Map<Plan, Double> plans){
    	Map.Entry<Plan, Double> min = null;   	
    	for (Map.Entry<Plan, Double> entry : plans.entrySet()) {
    		if (min == null || entry.getValue() < min.getValue()) {
    	        min = entry;
    	    }
        }
		return min.getKey();
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

            if (isLowestCostForState(C, n, node -> node.getGCost() + h(node))) {
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
        System.out.println("visited nodes:" + C.size());
        return plan;
    }

    /**
     * return true if the list doesn't contain the node or if it does contain it but with a higher cost.
     * In the latter case, remove the higher cost node from the list
     */
    private boolean isLowestCostForState(ArrayList<Node> list, Node n, ToDoubleFunction<Node> costFunction) {
        if(!list.contains(n)){
            return true;
        }
        else{
            Node currentBest = list.get(list.indexOf(n));
            if(costFunction.applyAsDouble(n) < costFunction.applyAsDouble(currentBest)){
                list.remove(currentBest);
                return true;
            }
            else{
                return false;
            }
        }
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
