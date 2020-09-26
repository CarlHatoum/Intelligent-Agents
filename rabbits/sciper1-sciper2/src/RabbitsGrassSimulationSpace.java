import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationSpace {
    private Object2DGrid grassSpace;
    private Object2DGrid rabbitSpace;
    private int maxGrassPerCell;
    private RabbitsGrassSimulationModel model;


    public RabbitsGrassSimulationSpace(int gridSize, int maxGrassPerCell) {
        rabbitSpace = new Object2DGrid(gridSize, gridSize);
        grassSpace = new Object2DGrid(gridSize, gridSize);
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                grassSpace.putObjectAt(i, j, 0);
            }
        }
        this.maxGrassPerCell = maxGrassPerCell;
    }

    public void spreadGrass(int number) {
        int i = 0;
        int count = 0;
        int countLimit = maxGrassPerCell * grassSpace.getSizeX() * grassSpace.getSizeY();
        while (i < number && count < countLimit) {
            int x = (int) (Math.random() * (grassSpace.getSizeX()));
            int y = (int) (Math.random() * (grassSpace.getSizeY()));

            int currentGrass;
            if (grassSpace.getObjectAt(x, y) != null) {
                currentGrass = (Integer) grassSpace.getObjectAt(x, y);
            } else {
                currentGrass = 0;
            }
            if (currentGrass < maxGrassPerCell) {
                grassSpace.putObjectAt(x, y, currentGrass + 1);
                i++;
            }
            count++;
        }
    }

    public int getGrassAt(int x, int y) {
        int i;
        if (grassSpace.getObjectAt(x, y) != null) {
            i = (Integer) grassSpace.getObjectAt(x, y);
        } else {
            i = 0;
        }
        return i;
    }

    public Object2DGrid getCurrentGrassSpace() {
        return grassSpace;
    }

    public Object2DGrid getCurrentRabbitSpace() {
        return rabbitSpace;
    }

    public boolean cellIsOccupied(int x, int y) {
        return rabbitSpace.getObjectAt(x, y) != null;
    }

    public boolean addRabbit(RabbitsGrassSimulationAgent rabbit, int x, int y) {
        if (0 <= x && x < rabbitSpace.getSizeX() && 0 <= y && y < rabbitSpace.getSizeY()) {
            if (!cellIsOccupied(x, y)) {
                rabbitSpace.putObjectAt(x, y, rabbit);
                rabbit.setXY(x, y);
                rabbit.setSpace(this);
                rabbit.setModel(model);
                return true;
            }
        }
        return false;
    }

    public void removeRabbitAt(int x, int y) {
        rabbitSpace.putObjectAt(x, y, null);
    }

    public int takeGrassAt(int x, int y) {
        int grass = getGrassAt(x, y);
        grassSpace.putObjectAt(x, y, 0);
        return grass;
    }


    public boolean moveRabbitAt(int x, int y, int newX, int newY) {
        boolean retVal = false;
        if (!cellIsOccupied(newX, newY)) {
            RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(x, y);
            removeRabbitAt(x, y);
            rabbit.setXY(newX, newY);
            rabbitSpace.putObjectAt(newX, newY, rabbit);
            retVal = true;
        }
        return retVal;
    }

    public void setModel(RabbitsGrassSimulationModel model) {
        this.model = model;
    }
}
