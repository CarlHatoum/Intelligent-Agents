package deliberative;

import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;

import deliberative.Deliberative.MyAction;
import deliberative.Deliberative.MyDeliver;
import deliberative.Deliberative.MyMove;
import deliberative.Deliberative.MyPickup;

public class Node {
	
    private City city;
    private Node parent;
    private double gCost;
    private TaskSet carriedTasks;
    private TaskSet remainingTasks;

    public Node(Node parent, City city, TaskSet carriedTasks, TaskSet remainingTasks) {
        this.parent = parent;
        this.city = city;
        this.carriedTasks = carriedTasks;
        this.remainingTasks = remainingTasks;
        gCost = parent.getGCost() + getTransitionCost(parent, this);
    }

    public City getCity() {
        return city;
    }

    public Node getParent() {
        return parent;
    }

    public ArrayList<Node> generateChildren(MyAction action) {
        ArrayList<Node> children = new ArrayList<>();
            
        if (action instanceof MyDeliver) {
        	for (Task task : carriedTasks) {
        		if (task.deliveryCity == this.getCity()) {
            		this.carriedTasks.remove(task);
            	}
        		Node child = new Node(this, this.city, this.getCarriedTasks(), this.getRemainingTasks());
            	children.add(child);
        	}
        
        }
        if (action instanceof MyPickup) {
        	for (Task task : remainingTasks) {
        		if (task.pickupCity == this.getCity()) {
        			this.remainingTasks.remove(task);
            		this.carriedTasks.add(task);
            	}
        		Node child = new Node(this, this.city, this.getCarriedTasks(), this.getRemainingTasks());
            	children.add(child);
        	}
        }
        if (action instanceof MyMove) {
        	for (City neighbor : city.neighbors()) {
        		Node child = new Node(this, neighbor, this.getCarriedTasks(), this.getRemainingTasks());
        		children.add(child);
        	}
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

	public void setRemainingTasks(TaskSet remainingTasks) {
		this.remainingTasks = remainingTasks;
	}
	
	public boolean isGoalState() {
		return remainingTasks.isEmpty() && carriedTasks.isEmpty();
		
	}

	public TaskSet getCarriedTasks() {
		return carriedTasks;
	}

	public void setCarriedTasks(TaskSet carriedTasks) {
		this.carriedTasks = carriedTasks;
	}


}
