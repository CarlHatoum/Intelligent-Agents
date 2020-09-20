import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.*;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
    private int x;
    private int y;
    private RabbitsGrassSimulationSpace space;

    public RabbitsGrassSimulationAgent(){

    }

    public void draw(SimGraphics G) {
        G.drawFastRoundRect(Color.blue);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setXY(int newX, int newY){
        x = newX;
        y = newY;
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }
}
