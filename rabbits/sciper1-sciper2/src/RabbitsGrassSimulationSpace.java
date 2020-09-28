import uchicago.src.sim.space.Object2DGrid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 */

public class RabbitsGrassSimulationSpace {
    public static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;
    public static final Integer[] DIRECTIONS = {NORTH, EAST, SOUTH, WEST};

    private Object2DGrid grassSpace;
    private int gridSize;
    private Object2DGrid rabbitSpace;
    private RabbitsGrassSimulationModel model;


    public RabbitsGrassSimulationSpace(int gridSize) {
        this.gridSize = gridSize;
        rabbitSpace = new Object2DGrid(gridSize, gridSize);
        grassSpace = new Object2DGrid(gridSize, gridSize);

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                grassSpace.putObjectAt(i, j, 0);
            }
        }
    }

    /**
     * create a given amount of grass randomly across the grid
     */
    public void spreadGrass(int number) {
        int i = 0;
        int count = 0;
        int countLimit = model.getMaxGrassPerCell() * grassSpace.getSizeX() * grassSpace.getSizeY();
        while (i < number && count < countLimit) {
            int x = (int) (Math.random() * (grassSpace.getSizeX()));
            int y = (int) (Math.random() * (grassSpace.getSizeY()));

            int currentGrass;
            if (grassSpace.getObjectAt(x, y) != null) {
                currentGrass = (Integer) grassSpace.getObjectAt(x, y);
            } else {
                currentGrass = 0;
            }
            if (currentGrass < model.getMaxGrassPerCell()) {
                grassSpace.putObjectAt(x, y, currentGrass + 1);
                i++;
            }
            count++;
        }
    }

    /**
     * returns the number of grasses at given location
     */
    public int getGrassAt(int x, int y) {
        int i;
        if (grassSpace.getObjectAt(x, y) != null) {
            i = (Integer) grassSpace.getObjectAt(x, y);
        } else {
            i = 0;
        }
        return i;
    }

    /**
     * returns the cell in given direction from given position
     */
    public int[] getCellInDirection(int x, int y, int dir) {
        int newX = x, newY = y;

        if (dir == WEST) {
            newX++;
        } else if (dir == EAST) {
            newX--;
        } else if (dir == SOUTH) {
            newY++;
        } else if (dir == NORTH) {
            newY--;
        }

        newX = (newX + rabbitSpace.getSizeX()) % rabbitSpace.getSizeX();
        newY = (newY + rabbitSpace.getSizeY()) % rabbitSpace.getSizeY();

        return new int[]{newX, newY};
    }

    /**
     * return the coordinate of a random free adjacent cell among the 4 cardinal directions from given location
     */
    public int[] getFreeAdjacentCell(int x, int y) {
        List<Integer> dirList = Arrays.asList(DIRECTIONS);
        Collections.shuffle(dirList);

        for (int dir : dirList) {
            int res[] = getCellInDirection(x, y, dir);
            if (!cellIsOccupied(res[0], res[1])) {
                return res;
            }
        }
        return null;
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

    /**
     * add a given rabbit to the given location, if it is free.
     * return a boolean to indicate success
     */
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

    /**
     * remove grass at given location and return its quantity
     */
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

    /**
     * return the total amount of grass in the grid
     */
    public int getNumberOfGrass() {
        int grassCount = 0;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                grassCount += getGrassAt(i, j);
            }
        }
        return grassCount;
    }
}
