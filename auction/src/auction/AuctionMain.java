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
import logist.simulation.VehicleImpl;
import logist.task.DefaultTaskDistribution;
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
	private DefaultTaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private double p = .7;
	private long timeout_plan;
	private long timeout_bid;
	private double discount_factor = 0.7;

	private ArrayList<Task> assignedTasks;
	private Solution currentSolution;
	private Solution opponentSolution;
	private ArrayList<OpponentVehicle> opponentVehicles;
	private double uncertainty_factor = 1;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = (DefaultTaskDistribution) distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		Solution.topology = topology;
		Solution.agent = agent;

		this.assignedTasks = new ArrayList<>();
		this.currentSolution = new Solution();
		this.opponentSolution = new Solution();

		OpponentVehicle.costPerKm = vehicle.costPerKm();
		OpponentVehicle.capacity = vehicle.capacity();
		opponentVehicles = new ArrayList<>();
		for(int i = 0; i<agent.vehicles().size();i++){
			opponentVehicles.add(new OpponentVehicle());
		}

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
		for(int i = 0; i<bids.length; i++){
			if(i!=agent.id())System.out.println("opponent bid: " + bids[i]);
		}

		if (winner == agent.id()) {
			System.out.println("task "+previous.id + " received");
			assignedTasks.add(previous);
			currentSolution.addNewTask(previous);
			currentSolution = optimizeSolution(currentSolution, timeout_bid*0.1);

			//TODO remove line
			currentCity = previous.deliveryCity;
		}
		else{
			opponentSolution.addNewTask(previous);
			opponentSolution = optimizeSolution(opponentSolution, timeout_bid*0.1);
			System.out.println("opponent received task " + previous.id);
		}
		System.out.println("our plan:");
		currentSolution.printActions();
		System.out.println("opponent plan:");
		opponentSolution.printActions();

		System.out.println();
	}

	@Override
	public Long askPrice(Task task) {
		System.out.println("Bid for " + task + ":");
		double ownCost = costEstimation(currentSolution, task, timeout_bid*0.2);
		System.out.println("our cost estimate: " + ownCost);

		double opponentCost = opponentCostEstimation(task, timeout_bid*0.3, topology.cities());
		System.out.println("opponent estimate: " + opponentCost);

		double opponentBid = Math.max(uncertainty_factor * opponentCost, 0);
		System.out.println("opponent bid prediction: " + opponentBid);
		long ourBid = bid(ownCost, opponentBid, 0.5);
		System.out.println("our bid: " + ourBid);
		return ourBid;
	}


	public long bid(double ownCost, double opponentBid, double alpha) {
		double bid;
		if (opponentBid < ownCost) bid = ownCost;
			// the higher the alpha, the more we take risk
		else bid = ownCost + alpha*(opponentBid - ownCost);
		return Math.round(Math.max(0, bid));
	}

	public double costEstimation(Solution solution, Task additionalTask, double timeout){
		Solution newSolution = new Solution(solution);
		newSolution.addNewTask(additionalTask);

		double marginalCost = Math.max(optimizeSolution(newSolution, timeout*0.5).computeCost() - solution.computeCost(), 0);

		double futureSavings1 = Math.min(futureSavingsIfTaskTaken(solution, newSolution, 3, timeout*0.5) - marginalCost, 0);
		double futureSavings2 = Math.min(futureSavingsIfTaskTaken(solution, newSolution, 4, timeout*0.5) - marginalCost, 0);

//		System.out.println("marginal cost:"+marginalCost);
//		System.out.println("savings in 1 round:"+futureSavings1);
//		System.out.println("savings in 2 round:"+futureSavings2);

		double cost = marginalCost + discount_factor*futureSavings1 + discount_factor*discount_factor* futureSavings2;

		return cost;
	}

	public double futureSavingsIfTaskTaken(Solution sol, Solution solIfTaskTaken, int numberOfFutureTasks, double timeout){
		int numberOfRuns = 30;

		double futureSavings = 0;
		for(int i = 0; i<numberOfRuns; i++){
			Solution newSol = new Solution(sol);
			Solution newSoIfTaskTaken = new Solution(solIfTaskTaken);
			for (int j = 0; j<numberOfFutureTasks; j++){
				Task newTask = distribution.createTask();
				newSol.addNewTask(newTask);
				newSoIfTaskTaken.addNewTask(newTask);
			}

			double futureCost = optimizeSolution(newSol, timeout/numberOfRuns/2).computeCost();
			double futureCostIfTaskTaken = optimizeSolution(newSoIfTaskTaken, timeout/numberOfRuns/2).computeCost() ;
//			solIfTaskTaken.printActions();
//			System.out.println(solIfTaskTaken.computeCost());
//			newSoIfTaskTaken.printActions();
//			System.out.println(newSoIfTaskTaken.computeCost());
//
//			System.out.println("if not taken "+futureCost);
//			System.out.println("if taken "+futureCostIfTaskTaken);
//			System.out.println("diff "+futureCostIfTaskTaken);

			futureSavings += (futureCostIfTaskTaken - futureCost)/numberOfRuns;
		}
		return futureSavings;
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

	public double opponentCostEstimation(Task additionalTask, double timeout, List<City> possibleStartingCities){
		int n = possibleStartingCities.size();
		double average = 0;
		for(City startingCity: possibleStartingCities){
			OpponentVehicle.startingCity = startingCity;
			double estimation = costEstimation(opponentSolution, additionalTask, timeout/n);
			average += estimation/n;
		}
		return average;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		System.out.println("Initial:");
		currentSolution.printActions();
		Solution best = optimizeSolution(currentSolution, timeout_plan);

		System.out.println("Solution:");
		List<Plan> plans = best.convertToPlans();
		best.printActions();
		System.out.println(best.computeCost());

		return plans;
	}

	public Solution optimizeSolution(Solution initialSolution, double timeout) {
		long time_start = System.currentTimeMillis();

		Solution best = initialSolution;
		double bestCost = initialSolution.computeCost();
//		System.out.println("before "+bestCost);
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

//		System.out.println("after "+bestCost);
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

		Vehicle vi = randomVehicles.stream().filter(A_old::hasActions).findFirst().orElse(null);
		if(vi == null) return neighbours;

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
