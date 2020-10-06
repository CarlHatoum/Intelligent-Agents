package rla;

import java.util.HashMap;
import java.util.Random;

import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;
import rla.PolicyGenerator.MyAction;
import rla.PolicyGenerator.MyMove;
import rla.PolicyGenerator.MyPickup;

/**
 * Reactive Agent using a fixed policy, optimized using the Value Iteration method
 */
public class ReactiveRLA implements ReactiveBehavior {

    private int numActions;
    private Agent myAgent;
    Double discount;

    private HashMap<State, MyAction> policy;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {

        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95
        discount = agent.readProperty("discount-factor", Double.class,
                0.95);

        this.numActions = 0;
        this.myAgent = agent;

        policy = new PolicyGenerator(topology, td, agent).generatePolicy(discount);
        //new PolicyGenerator(topology, td).displayT();
    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        Action action;
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
        	//System.out.println("Pickup the task, delivery to "+availableTask.deliveryCity);
        	action = new Action.Pickup(availableTask);
        } else {
        	//System.out.println("Move to next city : "+ ((MyMove) bestAction).getDestination());
            action = new Action.Move(((MyMove) bestAction).getDestination());
        }
        if (numActions >= 1) {
            //System.out.println("Reactive agent "+ discount+ ": The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
        }
        numActions++;
        return action;
    }
}
