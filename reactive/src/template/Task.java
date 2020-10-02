package template;

import logist.topology.Topology.City;

public class Task {
    private City destination;

    public Task(City destination) {
        this.destination = destination;
    }

    public City getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return "->" + destination;
    }
}
