import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import javax.imageio.ImageIO;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

    public static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;

    private int x;
    private int y;
    private int energy;
    private static int IDNumber = 0;
    private static int maxEnergy = 20;
    private static int minEnergy = 0;
    private int ID;
    private static int energyPerGrass;
    private static int birthThreshold;
    private RabbitsGrassSimulationSpace space;
    private RabbitsGrassSimulationModel model;

    public RabbitsGrassSimulationAgent() {
        x = -1;
        y = -1;
        energy = (int) ((Math.random() * (maxEnergy - minEnergy)) + minEnergy);
        IDNumber++;
        ID = IDNumber;
    }

    public static void setEnergyPerGrass(int energyPerGrass) {
        RabbitsGrassSimulationAgent.energyPerGrass = energyPerGrass;
    }

    public static void setBirthThreshold(int birthThreshold) {
        RabbitsGrassSimulationAgent.birthThreshold = birthThreshold;
    }

    public void draw(SimGraphics G) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("rabbit.png"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        G.drawImage(img);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;

    }

    public String getID() {
        return "Rabbit-" + ID;
    }

    public void setXY(int newX, int newY) {
        x = newX;
        y = newY;
    }

    public int getEnergy() {
        return energy;
    }

    public int[] getCellInDirection(int dir) {

        int newX = x, newY = y;

        if (dir == WEST) {
            newX--;
        } else if (dir == EAST) {
            newX++;
        } else if (dir == SOUTH) {
            newY--;
        } else if (dir == NORTH) {
            newY++;
        }

        Object2DGrid grid = space.getCurrentRabbitSpace();
        newX = (newX + grid.getSizeX()) % grid.getSizeX();
        newY = (newY + grid.getSizeY()) % grid.getSizeY();

        return new int[]{newX, newY};

    }

    /**
     * moves if cell is not occupied
     * after 4 random trial, stays at same position
     */
    public void step() {
        tryMoving();
        eatGrass();
        tryReproduce();
    }

    public void tryMoving() {
        int[] nextPos = getFreeAdjacentCell();
        if (nextPos != null) {
            int newX = nextPos[0], newY = nextPos[1];
            if (moveTo(newX, newY)) {
                energy--;
            }
        }
    }

    public void tryReproduce() {
        if (energy >= birthThreshold) {
            energy -= birthThreshold / 2;
            int[] newRabbitPos = getFreeAdjacentCell();
            if (newRabbitPos != null) {
                model.addNewRabbit(newRabbitPos[0], newRabbitPos[1]);
            }
        }
    }

    public void eatGrass() {
        energy += space.takeGrassAt(x, y) * energyPerGrass;
    }

    public boolean moveTo(int newX, int newY) {
        return space.moveRabbitAt(x, y, newX, newY);
    }

    public int[] getFreeAdjacentCell() {
        List<Integer> dirList = Arrays.asList(NORTH, EAST, SOUTH, WEST);
        Collections.shuffle(dirList);

        for (int dir : dirList) {
            int res[] = getCellInDirection(dir);
            if (!space.cellIsOccupied(res[0], res[1])) {
                return res;
            }
        }
        return null;
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }

    public void setModel(RabbitsGrassSimulationModel model) {
        this.model = model;
    }
}
