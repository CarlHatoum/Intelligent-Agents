package template;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class PolicyGenerator {
    private Topology topology;
    private TaskDistribution td;
    private Agent agent;
    private final ArrayList<State> possibleStates;

    public abstract class Action {
    }

    public class Move extends Action {
        private final City destination;

        public Move(City destination) {
            this.destination = destination;
        }

        public City getDestination() {
            return destination;
        }

        public String toString() {
            return "moveTo" + destination;
        }
    }

    public class Pickup extends Action {
        public String toString() {
            return "pickup";
        }
    }

    public PolicyGenerator(Topology topology, TaskDistribution td, Agent agent) {
        this.topology = topology;
        this.td = td;
        this.agent = agent;
        possibleStates = generateAllPossibleState();
    }

    public final ArrayList<State> generateAllPossibleState() {
        ArrayList<State> res = new ArrayList<>();
        for (City city : topology.cities()) {
            for (City taskDestination : topology.cities()) {
                if (taskDestination != city) {
                    res.add(new State(city, new Task(taskDestination)));
                }
            }
            res.add(new State(city, null));
        }
        return res;
    }

    public final ArrayList<Action> getActionsFromState(State s) {
        City city = s.getCity();
        ArrayList<Action> res = new ArrayList<>();
        for (City neighbour : city.neighbors()) {
            res.add(new Move(neighbour));
        }
        if (s.getCityTask() != null) {
            res.add(new Pickup());
        }
        return res;
    }

    public double T(State state, Action action, State nextState) {
        //current state
        City city = state.getCity();
        Task cityTask = state.getCityTask();

        //next state
        City nextCity = nextState.getCity();
        Task nextCityTask = nextState.getCityTask();

        if (action instanceof Pickup) {
            if (cityTask != null) {
                if (cityTask.getDestination().equals(nextCity)) {
                    if (nextCityTask != null) {
                        return td.probability(cityTask.getDestination(), nextCityTask.getDestination());
                    } else {
                        //probability that there is no task in next city
                        //1-sum(P())
                        double res = 1;
                        for (City possibleCityTask : topology.cities()) {
                            res -= td.probability(cityTask.getDestination(), possibleCityTask);
                        }
                        return res;
                    }
                }
            }

        } else if (action instanceof Move) {
            City destination = ((Move) action).getDestination();
            if (nextCity.equals(city)) {
                //cannot move to the same city
                return 0;

            } else if (!nextCity.equals(destination)) {
                //zero probability
                return 0;

            } else {
                if (nextCityTask != null) {
                    return td.probability(nextCity, nextCityTask.getDestination());
                } else {
                    //probability that there is no task in next city
                    //1-sum(P())
                    double res = 1;
                    for (City possibleCityTask : topology.cities()) {
                        res -= td.probability(nextCity, possibleCityTask);
                    }
                    return res;
                }
            }
        }
        return 0;
    }


    public HashMap<State, Action> generatePolicy(double discount) {
        //init V(s) arbitrarily
        HashMap<State, Double> V = new HashMap<>();
        for (State s : possibleStates) {
            V.put(s, 0.0);
        }

        //optimize V by Value iteration
        HashMap<State, Double> lastV;
        double epsilon = 1e-14;
        do {
            //copy V
            lastV = new HashMap<>(V);
            for (State s : possibleStates) {
                //find highest Q for all possible actions
                double maxQ = getActionsFromState(s).stream()
                        .mapToDouble(a -> Q(s, a, V, discount))
                        .max()
                        .orElseThrow(NoSuchElementException::new);
                V.put(s, maxQ);
            }
        } while (getError(V, lastV) > epsilon);

        //generate policy from V
        HashMap<State, Action> policy = new HashMap<>();
        for (State s : possibleStates) {
            //find action that maximises Q()
            Action bestAction = getActionsFromState(s).stream()
                    .max(Comparator.comparing(a -> Q(s, a, V, discount)))
                    .orElseThrow(NoSuchElementException::new);
            policy.put(s, bestAction);
        }
        System.out.println(policy);
        return policy;
    }

    private double R(State state, Action action) {
        double cost = agent.vehicles().get(0).costPerKm();
        double reward = 0, loss = 0;
        if (action instanceof Pickup) {
            reward = td.reward(state.getCity(), state.getCityTask().getDestination());
            loss = cost * state.getCity().distanceTo(state.getCityTask().getDestination());
        } else {
            loss = cost * state.getCity().distanceTo(((Move) action).getDestination());
        }
        return (reward - loss);
    }

    private double Q(State s, Action a, HashMap<State, Double> V, double discount) {
        double sum = 0;
        for (State sp : possibleStates) {
            sum += T(s, a, sp) * V.get(sp);
        }
        return R(s, a) + discount * sum;
    }

    private double getError(HashMap<State, Double> V, HashMap<State, Double> lastV) {
        ArrayList<Double> diffs = new ArrayList<>();
        for (State s : possibleStates) {
            diffs.add(Math.abs(lastV.get(s) - V.get(s)));
        }
        System.out.println("error: " + Collections.max(diffs));
        return Collections.max(diffs);
    }


    //for debugging
    public void displayT() {
        for (State s : possibleStates) {
            for (Action a : getActionsFromState(s)) {
                double sumActions = 0;
                String line = "";
                for (State sp : possibleStates) {
                    if (T(s, a, sp) != 0.0) {
                        line += sp.toString() + ":" + T(s, a, sp) + ",";
                    }
                    sumActions += T(s, a, sp);
                }
                System.out.println(s);
                System.out.println("(" + line + ")");
                System.out.println("sum: " + sumActions);
            }
        }
    }

}
