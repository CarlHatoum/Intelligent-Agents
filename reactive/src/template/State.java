package template;

import logist.topology.Topology.City;

public class State {
    private City city;
    private Task cityTask;

    public State(City city, Task cityTask) {
        this.city = city;
        this.cityTask = cityTask;
    }

    public City getCity() {
        return city;
    }

    public Task getCityTask() {
        return cityTask;
    }

    @Override
    public String toString() {
        if (cityTask == null) return "state (" + city + ", /)";
        else return "state (" + city + ", " + cityTask + ")";
    }
}
