package auction;

//the list of imports

import centralized.MyAction;
import centralized.Solution;
import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
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
 */
@SuppressWarnings("unused")
public class AuctionMain implements AuctionBehavior {

    private Topology topology;
    private DefaultTaskDistribution distribution;
    private Agent agent;
    private Random random;
    private double p = .7;
    private long timeout_plan;
    private long timeout_bid;
    private double discount_factor = 0.5;

    private Solution currentSolution;
    private Solution opponentSolution;
    private List<Vehicle> opponentVehicles;
    private double uncertainty_factor = 1;
    private double previousOpponentCostPrediction;
    private List<City> possibleOpponentCities;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        this.topology = topology;
        this.distribution = (DefaultTaskDistribution) distribution;
        this.agent = agent;

        Solution.topology = topology;

        OpponentVehicle.costPerKm = agent.vehicles().stream().mapToInt(Vehicle::costPerKm).max().orElseThrow();
        OpponentVehicle.capacity = agent.vehicles().stream().mapToInt(Vehicle::capacity).max().orElseThrow();
        opponentVehicles = new ArrayList<>();
        for (int i = 0; i < agent.vehicles().size(); i++) {
            opponentVehicles.add(new OpponentVehicle());
        }
        this.possibleOpponentCities = topology.cities();

        this.currentSolution = new Solution(agent.vehicles());
        this.opponentSolution = new Solution(opponentVehicles);

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);

        long seed = -9019554669489983951L * agent.id();
        this.random = new Random(seed);
    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        long opponentBid = 0;
        for (int i = 0; i < bids.length; i++) {
            if (i != agent.id()) {
                opponentBid = bids[i];
                System.out.println("opponent bid: " + opponentBid);
                updatePossibleCities(previous, opponentBid, timeout_bid * 0.1 / bids.length);
            }
        }

        if (winner == agent.id()) {
            System.out.println("task " + previous.id + " received");
            // add the new task to the solution and optimize
            currentSolution.addNewTask(previous);
            currentSolution = optimizeSolution(currentSolution, timeout_bid * 0.2);

            //reduce discount factor by 20%
            discount_factor *= 0.5;
        } else {
            // add the new task to the solution and optimize
            opponentSolution.addNewTask(previous);
            opponentSolution = optimizeSolution(opponentSolution, timeout_bid * 0.2);
            System.out.println("opponent received task " + previous.id);
        }
        System.out.println("our plan:");
        currentSolution.printActions();
        System.out.println("opponent plan:");
        opponentSolution.printActions();

        improveUncertaintyFactor(opponentBid);

        System.out.println();
    }

    /**
     * learn the value of the uncertainty factor with measurement r
     */
    public void improveUncertaintyFactor(long opponentBid) {
        double r = opponentBid / previousOpponentCostPrediction;
        if (previousOpponentCostPrediction == 0) return;
        double alpha = 0.2;
        uncertainty_factor = alpha * r + (1 - alpha) * uncertainty_factor;
        System.out.println("new factor:" + uncertainty_factor);
    }

    @Override
    public Long askPrice(Task task) {
        System.out.println("Bid for " + task + ":");
        double ownCost = costEstimation(currentSolution, task, timeout_bid * 0.2);
        System.out.println("our cost estimate: " + ownCost);

        double opponentCost = opponentCostEstimation(task, timeout_bid * 0.2);
        System.out.println("opponent estimate: " + opponentCost);

        //save the value for the uncertainty factor update
        previousOpponentCostPrediction = opponentCost;

        long opponentBid = Math.round(Math.max(uncertainty_factor * opponentCost, 0));
        System.out.println("opponent bid prediction: " + opponentBid);
        long ourBid = bid(ownCost, opponentBid, 0.9);
        System.out.println("our bid: " + ourBid);
        return ourBid;
    }

    /**
     * choose the value to bid, depending on costs
     */
    public Long bid(double ownCost, double opponentBid, double alpha) {
        double bid;
        // the higher the alpha, the more confident we are and consequently take risk
        if (opponentBid < ownCost) bid = ownCost;
        else bid = ownCost + alpha * (opponentBid - ownCost);
        return Math.round(Math.max(bid, 0));
    }

    /**
     * estimate the cost of taking a task, considering the future
     */
    public double costEstimation(Solution solution, Task additionalTask, double timeout) {
        Solution newSolution = new Solution(solution);
        newSolution.addNewTask(additionalTask);

        double marginalCost = Math.max(optimizeSolution(newSolution, timeout * 0.3).computeCost() - solution.computeCost(), 0);

        double futureSavings1 = Math.min(futureSavingsIfTaskTaken(solution, newSolution, 2, timeout * 0.3) - marginalCost, 0);
        double futureSavings2 = Math.min(futureSavingsIfTaskTaken(solution, newSolution, 4, timeout * 0.3) - marginalCost, 0);

//		System.out.println("marginal cost:"+marginalCost);
//		System.out.println("savings in 2 round:"+futureSavings1);
//		System.out.println("savings in 4 round:"+futureSavings2);

        double cost = marginalCost + discount_factor * futureSavings1 + discount_factor * discount_factor * futureSavings2;

        return Math.max(0, cost);
    }

    /**
     * estimated the future savings of taking a task, by averaging over 30 runs, with a given number of new tasks
     */
    public double futureSavingsIfTaskTaken(Solution sol, Solution solIfTaskTaken, int numberOfFutureTasks, double timeout) {
        int numberOfRuns = 20;
        double futureSavings = 0;
        for (int i = 0; i < numberOfRuns; i++) {
            Solution newSol = new Solution(sol);
            Solution newSoIfTaskTaken = new Solution(solIfTaskTaken);
            for (int j = 0; j < numberOfFutureTasks; j++) {
                Task newTask = distribution.createTask();
                newSol.addNewTask(newTask);
                newSoIfTaskTaken.addNewTask(newTask);
            }

            double futureCost = optimizeSolution(newSol, timeout / numberOfRuns / 2).computeCost();
            double futureCostIfTaskTaken = optimizeSolution(newSoIfTaskTaken, timeout / numberOfRuns / 2).computeCost();
            futureSavings += (futureCostIfTaskTaken - futureCost) / numberOfRuns;
        }
        return futureSavings;
    }

    /**
     * update the list of possible starting cities of the opponent, considering their bid
     */
    public void updatePossibleCities(Task task, Long bid, double timeout) {
        //the marginal cost is lower or equal to the bid
        long start = System.currentTimeMillis();
        do {
            List<City> newCities = new ArrayList<City>();
            for (City city : possibleOpponentCities) {
                long totalDistance = city.distanceUnitsTo(task.pickupCity) + task.pickupCity.distanceUnitsTo(task.deliveryCity);
                long marginalCost = totalDistance * OpponentVehicle.costPerKm;
                // if the bid is smaller than the marginal cost of the city, it cannot be considered as candidate
                if (marginalCost > bid) newCities.add(city);
            }
            possibleOpponentCities = newCities;
        } while ((System.currentTimeMillis() - start) < timeout);
    }

    /**
     * estimate the cost of taking an action for the opponent, by averaging over all his possible starting cities
     */
    public double opponentCostEstimation(Task additionalTask, double timeout) {
        int n = possibleOpponentCities.size();
        double average = 0;
        for (City startingCity : possibleOpponentCities) {
            OpponentVehicle.startingCity = startingCity;
            double estimation = costEstimation(opponentSolution, additionalTask, timeout / n/2);
            average += estimation / n;
        }
        return average;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        System.out.println("Initial:");

        Solution initialSolution = selectInitialSolution(vehicles, tasks);
        initialSolution.printActions();
        Solution best = optimizeSolution(initialSolution, timeout_plan * 0.9);

        System.out.println("Solution:");
        List<Plan> plans = best.convertToPlans();
        best.printActions();

        double cost = best.computeCost();
        System.out.println("cost: " + cost);

        double reward = 0;
        for (Task task:tasks){
            reward += task.reward;
        }
        System.out.println("reward: " + reward);
        System.out.println("net gain: " + (reward - cost));

        return plans;
    }

    /**
     * optimize the solution using the stochastic local search algorithm
     */
    public Solution optimizeSolution(Solution initialSolution, double timeout) {
        long time_start = System.currentTimeMillis();

        Solution best = initialSolution;
        double bestCost = initialSolution.computeCost();
        Solution A = initialSolution;
        Solution A_old;
        do {
            A_old = A;
            ArrayList<Solution> N = chooseNeighbours(A_old);
            A = localChoice(N, A_old);

            if (A.computeCost() < bestCost) {
                best = A;
                bestCost = A.computeCost();
            }
        } while ((System.currentTimeMillis() - time_start) < 0.8 * timeout);

        return best;
    }

    /**
     * returns similar solutions where a task was moved from a vehicle
     * and similar solutions where two actions have been swapped
     */
    private ArrayList<Solution> chooseNeighbours(Solution A_old) {
        ArrayList<Solution> neighbours = new ArrayList<>();
        List<Vehicle> randomVehicles = new ArrayList<>(A_old.getAgentVehicles());
        Collections.shuffle(randomVehicles);

        Vehicle vi = randomVehicles.stream().filter(A_old::hasActions).findFirst().orElseThrow();

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

    /**
     * Returns the initial solution where the tasks are distributed evenly among the vehicles
     * if a task is too big for a vehicle, it is given to the biggest one instead
     */
    private Solution selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
        Solution solution = new Solution(vehicles);
        Vehicle biggestVehicle = vehicles.stream().max(Comparator.comparingInt(Vehicle::capacity)).get();

        for (Vehicle potentialVehicle : vehicles) {
            MyAction previous = null;
            //filter the tasks evenly
            Iterator<Task> vehicleTasks = tasks.stream().filter(task -> task.id % vehicles.size() == potentialVehicle.id()).iterator();
            while (vehicleTasks.hasNext()) {
                Task task = vehicleTasks.next();
                Vehicle v;
                if (task.weight <= potentialVehicle.capacity()) v = potentialVehicle;
                else v = biggestVehicle;

                MyAction pickup = new MyAction(task, true);
                if (previous == null) solution.setNextAction(v, pickup);
                else solution.setNextAction(previous, pickup);

                MyAction delivery = new MyAction(task, false);
                solution.setNextAction(pickup, delivery);

                previous = delivery;
            }
        }

        return solution;
    }
}
