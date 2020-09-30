import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.log4j.chainsaw.Main;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 */

public class RabbitsGrassSimulationAgent implements Drawable {
    private int x;
    private int y;
    private int energy;
    private static int IDNumber = 0;
    private int ID;

    private RabbitsGrassSimulationSpace space;
    private RabbitsGrassSimulationModel model;

    public RabbitsGrassSimulationAgent(int initialEnergy) {
        x = -1;
        y = -1;
        //assign a random energy value
        energy = initialEnergy;
        IDNumber++;
        ID = IDNumber;
    }

    public void draw(SimGraphics G) {
    	URL url = Main.class.getResource("/rabbit.png");
    	ImageIcon img = new ImageIcon(url);
        G.drawImage(img.getImage());
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

    /**
     * method called every tick of simulation to move the rabbit, then eat grass, then reproduce if possible
     */
    public void step() {
        tryMove();
        eatGrass();
        tryReproduce();
    }

    /**
     * move in a random free adjacent cell and lose an energy point
     */
    public void tryMove() {
        int[] nextPos = space.getFreeAdjacentCell(x, y);
        if (nextPos != null) {
            int newX = nextPos[0], newY = nextPos[1];
            if (moveTo(newX, newY)) {
                energy--;
            }
        }
    }

    /**
     * if the rabbits has enough energy, create a new one in an adjacent cell
     */
    public void tryReproduce() {
        if (energy >= model.getBirthThreshold()) {
            int[] newRabbitPos = space.getFreeAdjacentCell(x, y);
            if (newRabbitPos != null) {
                model.addNewRabbit(newRabbitPos[0], newRabbitPos[1]);
                energy -= model.getEnergyToReproduce();
            }
        }
    }

    /**
     * eat the grass at the current rabbit location to gain energy
     */
    public void eatGrass() {
        energy += space.takeGrassAt(x, y) * model.getEnergyPerGrass();
    }

    public boolean moveTo(int newX, int newY) {
        return space.moveRabbitAt(x, y, newX, newY);
    }

    public int getEnergy() {
        return energy;
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }

    public void setModel(RabbitsGrassSimulationModel model) {
        this.model = model;
    }
}
