package auction;

//the list of imports

import centralized.MyAction;
import centralized.Solution;
import logist.LogistSettings;
import logist.Measures;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.io.File;
import java.util.*;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionMain implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private double p = .7;
	private long timeout_plan;
	private long timeout_bid;

	private ArrayList<Task> assignedTasks;
	private Solution currentSolution;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		Solution.topology = topology;
		Solution.agent = agent;

		this.assignedTasks = new ArrayList<>();
		this.currentSolution = new Solution();

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			System.out.println("task "+previous.id + " received");
			assignedTasks.add(previous);
			currentSolution.addNewTask(previous);
			currentSolution = optimizeSolution(currentSolution, timeout_bid*0.2);
			currentSolution.printActions();
			System.out.println();
			currentCity = previous.deliveryCity;
		}
	}

	@Override
	public Long askPrice(Task task) {
		System.out.println("Bid for " + task + ":");
		double ownCost = ownCostEstimation(task, timeout_bid*0.4);
		System.out.println("Our cost estimate:" + ownCost);

		double opponentCost = opponentCostEstimation(task, timeout_bid*0.4);

		//TODO
		return dummyAskPrice(task);
	}

	public Long dummyAskPrice(Task task) {
		if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;

		System.out.println("main agent bid " + Math.round(bid));
		return (long) Math.round(bid);
	}

	public double ownCostEstimation(Task additionalTask, double timeout){
		ArrayList<Task> newTaskList = new ArrayList<>(assignedTasks);
		newTaskList.add(additionalTask);
		Solution newSolution = new Solution(currentSolution);
		newSolution.addNewTask(additionalTask);
		double newCost = optimizeSolution(newSolution, timeout*0.5).computeCost();
		return newCost - currentSolution.computeCost();
	}
	
	public List<City> possibleCities(List<City> possibleCities, Task task, Long bid) {
		//the marginal cost is lower or equal to the bid
		for (City city : possibleCities) {
			long totalDistance = city.distanceUnitsTo(task.pickupCity) + task.pickupCity.distanceUnitsTo(task.deliveryCity);
			long marginalCost = totalDistance * 5;
			// if the bid is smaller than the marginal cost of the city, it cannot be considered as candidate
			if (bid < marginalCost) {
				possibleCities.remove(city);
			}
		}
		return possibleCities;
	}

	public double opponentCostEstimation(Task additionalTask, double timeout){
		//TODO
		return 0.0;
	}
	
	public double bid(double ownCost, double oponentCost, double alpha) {
		double bid;
		if (oponentCost < ownCost) bid = 99999;
		// the higher the alpha, the more we take risk
		else bid = ownCost + alpha*(oponentCost - ownCost);
		return bid;
		
	}


	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		System.out.println("Initial:");
		Solution best = optimizeSolution(currentSolution, timeout_plan);

		System.out.println("Solution:");
		List<Plan> plans = best.convertToPlans();
		best.printActions();
		System.out.println(best.computeCost());

		return plans;
	}

	public Solution optimizeSolution(Solution initialSolution, double timeout) {
		long time_start = System.currentTimeMillis();

		Solution best = null;
		double bestCost = Double.POSITIVE_INFINITY;

		Solution A = initialSolution;
		Solution A_old;
		do {
			A_old = A;
			ArrayList<Solution> N = chooseNeighbours(A_old);
			A = localChoice(N, A_old);
//            A.printActions();

			if (A.computeCost() < bestCost) {
				best = A;
				bestCost = A.computeCost();
			}
		} while ((System.currentTimeMillis() - time_start) < 0.9 * timeout);

		return best;
	}

//	private Solution selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
//		Solution solution = new Solution();
//		Vehicle biggestVehicle = vehicles.stream().max(Comparator.comparingInt(Vehicle::capacity)).get();
//
//		for (Vehicle potentialVehicle : vehicles) {
//			MyAction previous = null;
//			//filter the tasks evenly
//			Iterator<Task> vehicleTasks = tasks.stream().filter(task -> task.id % vehicles.size() == potentialVehicle.id()).iterator();
//			while (vehicleTasks.hasNext()) {
//				Task task = vehicleTasks.next();
//				Vehicle v;
//				if (task.weight <= potentialVehicle.capacity()) v = potentialVehicle;
//				else v = biggestVehicle;
//
//				MyAction pickup = new MyAction(task, true);
//				if (previous == null) solution.setNextAction(v, pickup);
//				else solution.setNextAction(previous, pickup);
//
//				MyAction delivery = new MyAction(task, false);
//				solution.setNextAction(pickup, delivery);
//
//				previous = delivery;
//			}
//		}
//
//		return solution;
//	}

	private ArrayList<Solution> chooseNeighbours(Solution A_old) {
		ArrayList<Solution> neighbours = new ArrayList<>();
		List<Vehicle> randomVehicles = new ArrayList<>(agent.vehicles());
		Collections.shuffle(randomVehicles);

		Vehicle vi = randomVehicles.stream().filter(A_old::hasActions).findFirst().orElseThrow(null);

		randomVehicles.remove(vi);
		for (Task t : A_old.getTasks(vi)) {
			for (Vehicle vj : randomVehicles) {
				Solution n = new Solution(A_old);
				n.moveTask(t, vi, vj);
				if (n.checkCapacity() && n.checkOrder()) {
					neighbours.add(n);
				}
				if (t.weight <= vj.capacity()) {
					int length = A_old.getNumberOfActions(vj);
					for (int tIdx1 = 1; tIdx1 < length; tIdx1++) {
						for (int tIdx2 = tIdx1 + 1; tIdx2 < length + 1; tIdx2++) {
							Solution n2 = new Solution(n);
							n2.swapActionOrder(vj, 1, tIdx1);
							n2.swapActionOrder(vj, 2, tIdx2);
							if (n2.checkCapacity() && n2.checkOrder()) {
								neighbours.add(n);
							}
						}
					}
				}
			}
		}
		//add all permutation of 2 actions as neighbour (if valid)
		int length = A_old.getNumberOfActions(vi);
		for (int tIdx1 = 1; tIdx1 < length; tIdx1++) {
			for (int tIdx2 = tIdx1 + 1; tIdx2 < length + 1; tIdx2++) {
				Solution n = new Solution(A_old);
				n.swapActionOrder(vi, tIdx1, tIdx2);
				if (n.checkCapacity() && n.checkOrder()) {
					neighbours.add(n);
				}
			}
		}
		return neighbours;
	}

	/**
	 * returns a random lowest cost element with probability p,
	 * and a random element with probability 1-p
	 */
	private Solution localChoice(ArrayList<Solution> N, Solution A_old) {
		if (N.isEmpty()) return A_old;

		Solution chosenNeighbour;
		double bestCost = A_old.computeCost();

		ArrayList<Solution> bestSols = new ArrayList<>();
		for (Solution sol : N) {
			double cost = sol.computeCost();
			if (cost < bestCost) {
				bestCost = cost;
				bestSols = new ArrayList<>();
				bestSols.add(sol);
			} else if (cost == bestCost) {
				bestSols.add(sol);
			}
		}


		//return best with probability p
		if (Math.random() < p) {
			if (bestSols.isEmpty()) return A_old;
			return bestSols.get(random.nextInt(bestSols.size()));
		}
		//return random with probability 1-p
		else return N.get(random.nextInt(N.size()));
	}
}
