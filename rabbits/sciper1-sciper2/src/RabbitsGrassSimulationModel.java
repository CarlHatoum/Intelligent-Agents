import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

import java.awt.*;
import java.util.ArrayList;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {
    private static final int GRID_SIZE = 20;
    private static final int NUM_INIT_RABBITS = 100;
    private static final int NUM_INIT_GRASS = 70;
    private static final int GRASS_GROWTH_RATE = 15;
    private static final int BIRTH_THRESHOLD = 20;
    private static final int MAX_GRASS_PER_CELL = 1;
    private static final int ENERGY_PER_GRASS = 5;
    private static final int ENERGY_TO_REPRODUCE = 10;

    private int gridSize = GRID_SIZE;
    private int numInitRabbits = NUM_INIT_RABBITS;
    private int numInitGrass = NUM_INIT_GRASS;
    private int grassGrowthRate = GRASS_GROWTH_RATE;
    private int birthThreshold = BIRTH_THRESHOLD;
    private int maxGrassPerCell = MAX_GRASS_PER_CELL;
    private int energyPerGrass = ENERGY_PER_GRASS;
    private int energyToReproduce = ENERGY_TO_REPRODUCE;

    private Schedule schedule;
    private RabbitsGrassSimulationSpace space;
    private DisplaySurface displaySurf;

    private ArrayList<RabbitsGrassSimulationAgent> rabbitList;

    //population plot and its 2 sequences
    private OpenSequenceGraph populationPlot;

    class rabbitCount implements DataSource, Sequence {

        public Object execute() {
            return getSValue();
        }

        public double getSValue() {
            return rabbitList.size();
        }
    }

    class grassCount implements DataSource, Sequence {

        public Object execute() {
            return getSValue();
        }

        public double getSValue() {
            return space.getNumberOfGrass();
        }
    }

    public static void main(String[] args) {

        System.out.println("Rabbits Grass Simulation");

        SimInit init = new SimInit();
        RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
        // Do "not" modify the following lines of parsing arguments
        if (args.length == 0) // by default, you don't use parameter file nor batch mode
            init.loadModel(model, "", false);
        else
            init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
    }

    public String getName() {
        return "Rabbits Grass Simulation";
    }

    public String[] getInitParam() {
        String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "MaxGrassPerCell", "EnergyPerGrass", "energyToReproduce"};
        return params;
    }

    /**
     * reset the simulation
     * recreate the rabbit list, the schedule and the windows
     */
    public void setup() {
        space = null;
        rabbitList = new ArrayList<>();
        schedule = new Schedule(1);

        //recreate main window
        if (displaySurf != null) {
            displaySurf.dispose();
        }
        displaySurf = null;
        displaySurf = new DisplaySurface(this, "Rabbits Grass Simulation 1");
        registerDisplaySurface("Rabbits Grass Simulation 1", displaySurf);

        //recreate population plot
        if (populationPlot != null) {
            populationPlot.dispose();
        }
        populationPlot = null;
        populationPlot = new OpenSequenceGraph("Population Plot", this);
        populationPlot.setYAutoExpand(false);
        populationPlot.setYRange(0, 200);
        this.registerMediaProducer("Plot", populationPlot);
    }


    /**
     * method called at the start of a run
     */
    public void begin() {
        if (!checkParams()) return;

        buildModel();
        buildSchedule();
        buildDisplay();

        displaySurf.display();
        populationPlot.display();
    }

    public boolean checkParams() {
        if (gridSize <= 0 || numInitRabbits < 0 || numInitGrass < 0
                || grassGrowthRate < 0 || birthThreshold <= 0 || maxGrassPerCell <= 0
                || energyPerGrass < 0 || energyToReproduce < 0) {
            System.out.println("Simulation Error: Parameters should be positive");
            return false;
        }
        if (energyToReproduce > birthThreshold) {
            System.out.println("Simulation Error: BirthThreshold should be greater than EnergyToReproduce");
            return false;
        }
        return true;
    }

    /**
     * initialize the space with random grass and rabbit placement
     */
    public void buildModel() {
        space = new RabbitsGrassSimulationSpace(gridSize);
        space.setModel(this);
        space.spreadGrass(numInitGrass);

        for (int i = 0; i < numInitRabbits; i++) {
            addNewRandomRabbit();
        }
    }

    /**
     * create schedules for the behavior of the rabbits, grass growth and plot updates
     */
    public void buildSchedule() {
        class SimulationStep extends BasicAction {
            public void execute() {
                SimUtilities.shuffle(rabbitList);
                for (int i = 0; i < rabbitList.size(); i++) {
                    RabbitsGrassSimulationAgent rabbit = rabbitList.get(i);
                    rabbit.step();
                }

                reapDeadRabbits();

                displaySurf.updateDisplay();
            }
        }
        schedule.scheduleActionBeginning(0, new SimulationStep());

        class GrassGrowth extends BasicAction {
            public void execute() {
                space.spreadGrass(grassGrowthRate);
            }
        }
        schedule.scheduleActionBeginning(0, new GrassGrowth());

        class UpdatePopulationPlot extends BasicAction {
            public void execute() {
                populationPlot.step();
            }
        }
        schedule.scheduleActionAtInterval(5, new UpdatePopulationPlot());
    }

    /**
     * build the main grid display window with probeable cells and rabbits.
     */
    public void buildDisplay() {
        ColorMap map = new ColorMap();

        for (int i = 1; i <= maxGrassPerCell; i++) {
            map.mapColor(i, new Color(0, Math.max(256 - i * 12, 50), 0));
        }
        map.mapColor(0, Color.white);

        Value2DDisplay displayMoney =
                new Value2DDisplay(space.getCurrentGrassSpace(), map);

        Object2DDisplay displayRabbits = new Object2DDisplay(space.getCurrentRabbitSpace());
        displayRabbits.setObjectList(rabbitList);

        displaySurf.addDisplayableProbeable(displayMoney, "Grass");
        displaySurf.addDisplayableProbeable(displayRabbits, "Rabbits");

        populationPlot.addSequence("Number of Rabbits", new rabbitCount(), Color.BLUE);
        populationPlot.addSequence("Number of Grass", new grassCount(), new Color(0, 150, 0));
    }

    /**
     * create a new rabbit at a free random location on the grid
     */
    private void addNewRandomRabbit() {
        int countLimit = 10 * gridSize ^ 2;
        boolean foundFreeCell = false;
        int count = 0;
        while (!foundFreeCell && count < countLimit) {
            int x = (int) (Math.random() * gridSize);
            int y = (int) (Math.random() * gridSize);
            if (addNewRabbit(x, y)) {
                foundFreeCell = true;
            }
            count++;
        }
    }

    /**
     * create a new rabbit at given position
     */
    public boolean addNewRabbit(int x, int y) {
        //create a new rabbit with a random energy level between 0.2 and 1 times birthThreshold
        int rabbitInitialEnergy = (int) ((Math.random() * (birthThreshold - birthThreshold * 0.2)) + birthThreshold * 0.2);
        RabbitsGrassSimulationAgent newRabbit = new RabbitsGrassSimulationAgent(rabbitInitialEnergy);
        if (space.addRabbit(newRabbit, x, y)) {
            rabbitList.add(newRabbit);
            return true;
        } else return false;
    }

    /**
     * remove all rabbits with zero energy from the grid
     */
    private void reapDeadRabbits() {
        for (int i = (rabbitList.size() - 1); i >= 0; i--) {
            RabbitsGrassSimulationAgent rabbit = rabbitList.get(i);
            if (rabbit.getEnergy() <= 0) {
                space.removeRabbitAt(rabbit.getX(), rabbit.getY());
                rabbitList.remove(i);
            }
        }
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    public int getNumInitRabbits() {
        return numInitRabbits;
    }

    public void setNumInitRabbits(int numInitRabbits) {
        this.numInitRabbits = numInitRabbits;
    }

    public int getNumInitGrass() {
        return numInitGrass;
    }

    public void setNumInitGrass(int numInitGrass) {
        this.numInitGrass = numInitGrass;
    }

    public int getGrassGrowthRate() {
        return grassGrowthRate;
    }

    public void setGrassGrowthRate(int grassGrowthRate) {
        this.grassGrowthRate = grassGrowthRate;
    }

    public int getBirthThreshold() {
        return birthThreshold;
    }

    public void setBirthThreshold(int birthThreshold) {
        this.birthThreshold = birthThreshold;
    }

    public int getMaxGrassPerCell() {
        return maxGrassPerCell;
    }

    public void setMaxGrassPerCell(int maxGrassPerCell) {
        this.maxGrassPerCell = maxGrassPerCell;
    }

    public int getEnergyPerGrass() {
        return energyPerGrass;
    }

    public void setEnergyPerGrass(int energyPerGrass) {
        this.energyPerGrass = energyPerGrass;
    }

    public int getEnergyToReproduce() {
        return energyToReproduce;
    }

    public void setEnergyToReproduce(int energyToReproduce) {
        this.energyToReproduce = energyToReproduce;
    }
}
