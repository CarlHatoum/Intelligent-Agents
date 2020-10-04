package template;

import logist.topology.Topology.City;

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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((destination == null) ? 0 : destination.hashCode());
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
		MyTask other = (MyTask) obj;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		return true;
	}
}
