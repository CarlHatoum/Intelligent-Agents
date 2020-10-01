package template;

import logist.plan.Action;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class PolicyGenerator {
    private int numberOfCities;
    private int numberOfTasks;
    private Topology topology;
    private TaskDistribution td;
    private final ArrayList<State> possibleStates;

    public PolicyGenerator(Topology topology, TaskDistribution tdm) {
        this.topology = topology;
        this.td = td;
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
        }
        return res;
    }

    public final ArrayList<Action> getActionsFromState(State s) {
        City city = s.getCity();
        ArrayList<Action> res = new ArrayList<>();
        for (City neighbour : city.neighbors()) {
            res.add(new Action.Move(neighbour));
        }
        //res.add(new Action.Pickup());//TODO fix
        return res;
    }

    public double T(State state, Action action, State nextState) {
        //current state
        City city = state.getCity();
        Task cityTask = state.getCityTask();

        //next state
        City nextCity = nextState.getCity();
        Task nextCityTask = nextState.getCityTask();

        if (action instanceof Action.Pickup) {
            if (nextCity.equals(cityTask.getDestination())) {
                //if you take a task, you go to the city of the task, so the destination city of the available task (current state) is the city of next state.
                return 1;
            } else return 0;

        } else if (action instanceof Action.Move) {
            //City destination = ((Action.Move) action).getDestination();//TODO fix
            City destination = null;


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
                    //TODO 1-sum(P())
                    return 0;
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
        double epsilon = 1;
        do {
            //copy V
            lastV = new HashMap<>(V);

            for (State s : possibleStates) {
                ArrayList<Action> actions = getActionsFromState(s);
                double bestQ = computeReward(s, actions.get(0), V, discount);
                for (Action a : actions) {
                    double Q = computeReward(s, a, V, discount);
                    if (Q>bestQ){
                        bestQ = Q;
                    }
                }
                V.put(s, bestQ);
            }
        } while (getError(V, lastV) > epsilon);

        //generate policy from V
        HashMap<State, Action> policy = new HashMap<>();
        for (State s : possibleStates) {
            ArrayList<Action> possibleActions = getActionsFromState(s);

            //init at first element
            Action bestAction = possibleActions.get(0);
            double bestQ = computeReward(s, bestAction, V, discount);

            //find highest V action
            for (Action action : possibleActions) {
                double currentQ = computeReward(s, action, V, discount);
                if (currentQ > bestQ) {
                    bestQ = currentQ;
                    bestAction = action;
                }
            }
            policy.put(s, bestAction);
        }

        return policy;
    }

    private double computeReward(State s, Action a, HashMap<State, Double> V, double discount){
        //TODO
        double sum = 0;
        for (State sp : possibleStates) {
            sum += T(s, a, sp) * 0;
        }
        return 0;
    }

    private double getError(HashMap<State, Double> V, HashMap<State, Double> lastV) {
        ArrayList<Double> diffs = new ArrayList<>();
        for (State s : possibleStates) {
            diffs.add(Math.abs(lastV.get(s) - V.get(s)));
        }
        return Collections.max(diffs);
    }

}
