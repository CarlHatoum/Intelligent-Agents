package rla;

import logist.topology.Topology.City;

import java.util.Objects;

public class MyTask {
    private City destination;

    public MyTask(City destination) {
        this.destination = destination;
    }

    public City getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return "->" + destination;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDestination());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyTask myTask = (MyTask) o;
        return getDestination().equals(myTask.getDestination());
    }
}
