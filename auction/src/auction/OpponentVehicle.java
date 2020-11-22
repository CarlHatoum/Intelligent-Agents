package auction;

import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.awt.*;

public class OpponentVehicle implements Vehicle {
    public static int costPerKm;
    public static Topology.City startingCity;
    public static int capacity;

    @Override
    public int id() {
        return 0;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public Topology.City homeCity() {
        return startingCity;
    }

    @Override
    public double speed() {
        return 0;
    }

    @Override
    public int costPerKm() {
        return costPerKm;
    }

    @Override
    public Topology.City getCurrentCity() {
        return null;
    }

    @Override
    public TaskSet getCurrentTasks() {
        return null;
    }

    @Override
    public long getReward() {
        return 0;
    }

    @Override
    public long getDistanceUnits() {
        return 0;
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public Color color() {
        return null;
    }
}
