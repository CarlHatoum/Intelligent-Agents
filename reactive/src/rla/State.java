package rla;

import logist.topology.Topology.City;

import java.util.Objects;

public class State {
    private City city;
    private MyTask cityTask;

    public State(City city, MyTask cityTask) {
        this.city = city;
        this.cityTask = cityTask;
    }

    public City getCity() {
        return city;
    }

    public MyTask getCityTask() {
        return cityTask;
    }

    @Override
    public String toString() {
        if (cityTask == null) return "state (" + city + ", /)";
        else return "state (" + city + ", " + cityTask + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return getCity().equals(state.getCity()) &&
                Objects.equals(getCityTask(), state.getCityTask());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCity(), getCityTask());
    }

}
