package dummy;

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
 */
@SuppressWarnings("unused")
public class DummyAgent implements AuctionBehavior {

    private Topology topology;
    private DefaultTaskDistribution distribution;
    private Agent agent;
    private Random random;
    private Vehicle vehicle;
    private City currentCity;
    private double p = .7;
    private long timeout_plan;
    private long timeout_bid;
    private double discount_factor = 0.4;

    private Solution currentSolution;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        this.topology = topology;
        this.distribution = (DefaultTaskDistribution) distribution;
        this.agent = agent;
        this.vehicle = agent.vehicles().get(0);
        this.currentCity = vehicle.homeCity();

        Solution.topology = topology;

        this.currentSolution = new Solution(agent.vehicles());

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
            currentSolution.addNewTask(previous);
            currentSolution = optimizeSolution(currentSolution, timeout_bid * 0.3);

            //TODO remove line
            currentCity = previous.deliveryCity;
        }
    }

    @Override
    public Long askPrice(Task task) {
        Solution newSolution = new Solution(currentSolution);
        newSolution.addNewTask(task);
        double marginalCost = Math.max(optimizeSolution(newSolution, timeout_bid * 0.5).computeCost() - currentSolution.computeCost(), 0);
        System.out.println("dummy marginal cost: " + marginalCost);

        double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
        double bid = ratio * marginalCost;
        return Math.round(bid);
    }

    public double costEstimation(Solution solution, Task additionalTask, double timeout) {
        Solution newSolution = new Solution(solution);
        newSolution.addNewTask(additionalTask);

        double marginalCost = Math.max(optimizeSolution(newSolution, timeout * 0.5).computeCost() - currentSolution.computeCost(), 0);

        double futureSavings1 = Math.min(futureSavingsIfTaskTaken(currentSolution, newSolution, 1, timeout * 0.5) - marginalCost, 0);
        double futureSavings2 = Math.min(futureSavingsIfTaskTaken(currentSolution, newSolution, 2, timeout * 0.5) - marginalCost, 0);

        double cost = marginalCost + discount_factor * futureSavings1 + discount_factor * discount_factor * futureSavings2;

        return cost;
    }

    public double futureSavingsIfTaskTaken(Solution sol, Solution solIfTaskTaken, int numberOfFutureTasks, double timeout) {
        int numberOfRuns = 30;

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

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        Solution best = optimizeSolution(currentSolution, timeout_plan*0.7);
        List<Plan> plans = best.convertToPlans();
        return plans;
    }

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

    private ArrayList<Solution> chooseNeighbours(Solution A_old) {
        ArrayList<Solution> neighbours = new ArrayList<>();
        List<Vehicle> randomVehicles = new ArrayList<>(A_old.getAgentVehicles());
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
