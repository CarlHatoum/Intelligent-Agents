package deliberative;

import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Objects;

public class Node {

    private City city;
    private Node parent;
    private double gCost;
    private MyAction action; //action that led to this node
    private TaskSet carriedTasks;
    private TaskSet remainingTasks; //tasks left that are not currently being carried
    private final double maxCapacity;

    public Node(Node parent, City city, MyAction action, TaskSet carriedTasks, TaskSet remainingTasks, double maxCapacity) {
        this.parent = parent;
        this.city = city;
        this.action = action;
        this.carriedTasks = carriedTasks;
        this.remainingTasks = remainingTasks;
        if (parent != null) {
            gCost = parent.getGCost() + getTransitionCost(parent, this);
        } else gCost = 0;

        this.maxCapacity = maxCapacity;
    }

    public City getCity() {
        return city;
    }

    public Node getParent() {
        return parent;
    }

    public ArrayList<Node> generateChildren() {
        ArrayList<Node> children = new ArrayList<>();

        for (Task task : remainingTasks) {
            if (task.pickupCity.equals(this.getCity())) {
                if (getCurrentWeight() + task.weight <= maxCapacity) {
                    MyAction action = new MyPickup(task);

                    TaskSet remainingTasks = this.getRemainingTasks().clone();
                    remainingTasks.remove(task);
                    TaskSet carriedTasks = this.getCarriedTasks().clone();
                    carriedTasks.add(task);

                    Node child = new Node(this, this.city, action, carriedTasks, remainingTasks, maxCapacity);
                    children.add(child);
                }
            }
        }

        for (Task task : carriedTasks) {
            if (task.deliveryCity.equals(this.getCity())) {
                MyAction action = new MyDeliver(task);

                TaskSet remainingTasks = this.getRemainingTasks().clone();
                TaskSet carriedTasks = this.getCarriedTasks().clone();
                carriedTasks.remove(task);

                Node child = new Node(this, this.city, action, carriedTasks, remainingTasks, maxCapacity);
                children.add(child);
            }
        }

        for (City neighbor : city.neighbors()) {
            MyAction action = new MyMove(neighbor);

            Node child = new Node(this, neighbor, action, this.getCarriedTasks(), this.getRemainingTasks(), maxCapacity);
            children.add(child);
        }

        return children;
    }

    public double getGCost() {
        return gCost;
    }

    static public double getTransitionCost(Node startNode, Node endNode) {
        return startNode.city.distanceTo(endNode.city);
    }

    public TaskSet getRemainingTasks() {
        return remainingTasks;
    }

    public boolean isGoalState() {
        return remainingTasks.isEmpty() && carriedTasks.isEmpty();
    }

    public TaskSet getCarriedTasks() {
        return carriedTasks;
    }

    public double getCurrentWeight() {
        double sum = 0;
        for (Task task : carriedTasks) {
            sum += task.weight;
        }
        return sum;
    }

    public MyAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "Node{" +
                "city=" + city +
                ", parent=" + ((parent == null) ? "" : parent.getCity()) +
                ", gCost=" + gCost +
                ", action=" + action +
                ", carriedTasks=" + carriedTasks +
                ", remainingTasks=" + remainingTasks +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return getCity().equals(node.getCity()) &&
                getCarriedTasks().equals(node.getCarriedTasks()) &&
                getRemainingTasks().equals(node.getRemainingTasks());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCity(), getCarriedTasks(), getRemainingTasks());
    }
}
