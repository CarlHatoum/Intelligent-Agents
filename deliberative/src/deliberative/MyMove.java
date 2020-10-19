package deliberative;

import logist.topology.Topology.City;

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
