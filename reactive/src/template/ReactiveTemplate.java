package template;

import java.util.HashMap;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;
import template.PolicyGenerator.MyAction;
import template.PolicyGenerator.MyMove;
import template.PolicyGenerator.MyPickup;

public class ReactiveTemplate implements ReactiveBehavior {

    private Random random;
    private double pPickup;
    private int numActions;
    private Agent myAgent;

    private HashMap<State, PolicyGenerator.MyAction> policy;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {

        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95
        Double discount = agent.readProperty("discount-factor", Double.class,
                0.95);

        this.random = new Random();
        this.pPickup = discount;
        this.numActions = 0;
        this.myAgent = agent;

        policy = new PolicyGenerator(topology, td, agent).generatePolicy(discount);
        //new PolicyGenerator(topology, td).displayT();
    }

    @Override
    public logist.plan.Action act(Vehicle vehicle, Task availableTask) {
        logist.plan.Action action;
        City dest;
        City currentCity = vehicle.getCurrentCity();
        MyTask currentTask;
		if (availableTask != null ) {
        	 dest = availableTask.deliveryCity;
        	 currentTask = new MyTask(dest);
        } else {
        	 currentTask = null;
        }
		
        State currentState = new State(currentCity, currentTask);
        MyAction bestAction = policy.get(currentState);
        if (bestAction instanceof MyPickup) {
        	System.out.println("Pickup the task, delivery to "+availableTask.deliveryCity);
        	action = new logist.plan.Action.Pickup(availableTask);
        } else {
        	System.out.println("Move to next city : "+ ((MyMove) bestAction).getDestination());
            action = new logist.plan.Action.Move(((MyMove) bestAction).getDestination());
        }
        if (numActions >= 1) {
            System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
        }
        numActions++;
        return action;
    }
}
