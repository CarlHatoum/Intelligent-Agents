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

        LinkedList<Node> Q = new LinkedList<>(); //queue of nodes to be processed
        HashMap<Node, Double> C = new HashMap<>();

        Q.add(initialNode);

        while (!Q.isEmpty()) {
        	//pop first node in queue
            Node n = Q.pop();

            if(n.isFinalState()) {
            	//found a plan
            	Plan plan = generatePlanFromLastNode(n);
                plans.put(plan, n.getGCost());
            }
            //check if node was visited
            if (isLowestCostForState(C,n, node -> node.getGCost())) {
            	C.put(n, n.getGCost());
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

        City currentCity = vehicle.getCurrentCity();

        Node initialNode = new Node(null, currentCity, vehicle.getCurrentTasks(), tasks, vehicle.capacity());

        PriorityQueue<Node> Q = new PriorityQueue<>(Comparator.comparingDouble(
                x -> x.getGCost() + h(x)
        )); //queue of nodes to be processed, sorted according to f value
        HashMap<Node, Double> C = new HashMap<>(); //processed nodes and their cost

        Q.add(initialNode);

        Plan plan = new Plan(currentCity);
        while (!Q.isEmpty()) {
            //pop first node in queue
            Node n = Q.poll();

            if (n.isFinalState()) {
                //found the solution
                //generate the action List to get to the state n
                plan = generatePlanFromLastNode(n);
                break;
            }

            if (isLowestCostForState(C, n, node -> node.getGCost() + h(node))) {
                C.put(n, n.getGCost() + h(n));
                //add all the children of n to the queue
                Q.addAll(n.generateChildren());
                //the queue is a PriorityQueue, thus it is always sorted
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
    private boolean isLowestCostForState(HashMap<Node, Double> list, Node n, ToDoubleFunction<Node> costFunction) {
        if(!list.containsKey(n)){
            return true;
        }
        else{
            Double currentBest = list.get(n);
            return costFunction.applyAsDouble(n) < currentBest;
        }
    }

    private double getTransitionCost(Node n1, Node n2){
        return n1.getCity().distanceTo(n2.getCity());
    }

    /**
     * heuristic: distance of the task with the longest required distance (distance to get to the pickup city + distance of task)
     * if no task remaining, distance left of a random task
     */
    private double h(Node n) {
        if (n.getRemainingTasks().isEmpty()) {
            if (n.getCarriedTasks().isEmpty()) {
                return 0.0;
            }
            else
                return n.getCity().distanceTo(n.getCarriedTasks().iterator().next().deliveryCity);
        }
        else{
            return n.getRemainingTasks().stream()
                    .mapToDouble(task ->n.getCity().distanceTo(task.pickupCity) + task.pathLength())
                    .max()
                    .orElseThrow(NoSuchElementException::new);
        }
    }


    private Plan generatePlanFromLastNode(Node endNode){
        //generate the list of traversed nodes from start to end
        ArrayList<Node> path = new ArrayList<>();
        Node currentNode = endNode;
        do {
            path.add(0, currentNode);
            currentNode = currentNode.getParent();
        } while (currentNode.getParent()!=null);
        path.add(0, currentNode);

        //convert the list of nodes to a plan
        City initialCity = path.get(0).getCity();
        Plan plan = new Plan(initialCity);

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
        if (!carriedTasks.isEmpty()) {
            //this is not needed as vehicle.getCurrentTasks() is used for the plan generation
        }
    }
}
