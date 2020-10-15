package deliberative;

import logist.topology.Topology.City;

import java.util.ArrayList;

public class Node {
    private City city;

    private Node parent;
    private double gCost;

    public Node(Node parent, City city) {
        this.parent = parent;
        this.city = city;

        gCost = parent.getGCost() + getTransitionCost(parent, this);
    }

    public City getCity() {
        return city;
    }

    public Node getParent() {
        return parent;
    }

    public ArrayList<Node> generateChildren() {
        ArrayList<Node> children = new ArrayList<>();
        //TODO
        return children;
    }


    public double getGCost() {
        return gCost;
    }

    static public double getTransitionCost(Node startNode, Node endNode) {
        //TODO
        return 0.0;
    }


}
