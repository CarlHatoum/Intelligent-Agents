package template;

import logist.plan.ActionHandler;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class Move extends Action{
    private final City destination;

    public Move(City destination) {
        this.destination = destination;
    }

    public City getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return "moveTo"+destination;
    }
}
