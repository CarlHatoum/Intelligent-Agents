package template;

import logist.topology.Topology.City;

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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((city == null) ? 0 : city.hashCode());
		result = prime * result + ((cityTask == null) ? 0 : cityTask.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		if (city == null) {
			if (other.city != null)
				return false;
		} else if (!city.equals(other.city))
			return false;
		if (cityTask == null) {
			if (other.cityTask != null)
				return false;
		} else if (!cityTask.equals(other.cityTask))
			return false;
		return true;
	}

	
    
}
