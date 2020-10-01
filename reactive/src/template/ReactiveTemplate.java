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

public class ReactiveTemplate implements ReactiveBehavior {

    private Random random;
    private double pPickup;
    private int numActions;
    private Agent myAgent;

    private HashMap<State, Action> policy;

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

        policy = new PolicyGenerator(topology, td).generatePolicy(discount);
        //new PolicyGenerator(topology, td).displayT();
    }

    @Override
    public logist.plan.Action act(Vehicle vehicle, Task availableTask) {
        logist.plan.Action action;

        if (availableTask == null || random.nextDouble() > pPickup) {
            City currentCity = vehicle.getCurrentCity();
            action = new logist.plan.Action.Move(currentCity.randomNeighbor(random));
        } else {
            action = new logist.plan.Action.Pickup(availableTask);
        }

        if (numActions >= 1) {
            System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
        }
        numActions++;

        return action;
    }
}
