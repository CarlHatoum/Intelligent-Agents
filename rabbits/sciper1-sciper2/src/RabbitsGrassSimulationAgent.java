import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import java.awt.*;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int energy;
	private static int IDNumber = 0;
	private static int maxEnergy = 20;
	private static int minEnergy = 0;
	private int ID;
	private RabbitsGrassSimulationSpace space;
	
	public RabbitsGrassSimulationAgent(){
		x = -1;
		y = -1;
		energy = (int)((Math.random() * (maxEnergy - minEnergy)) + minEnergy);
		IDNumber++;
		ID = IDNumber;
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
    
    public String getID(){
        return "A-" + ID;
    }
    
    public void setXY(int newX, int newY){
        x = newX;
        y = newY;
    }
    
    public int getEnergy() {
        return energy;
    }
    
    /**
     * A basic 'step' for this agent random move to adjacent cells (north, south, east and west)
     * If presence of grass 
     */
    public void step(){
    	
    	int random =  (int)(Math.random())*4+1;
    	int newX  = x, newY = y;
    	
        if (random == 1){
        	newX--; // move west
        }
        else if (random == 2){
        	newX++; // move east
        }
        else if (random == 3){
        	newY--; // move south
        }
        else if (random == 4){
        	newY++; // move north
        }
              
      Object2DGrid grid = space.getCurrentRabbitSpace();
      newX = (newX + grid.getSizeX()) % grid.getSizeX();
      newY = (newY + grid.getSizeY()) % grid.getSizeY();

      if(space.moveRabbitAt(x, y, newX, newY)){
        energy += space.takeGrassAt(newX, newY);
        setXY(newX, newY);
        energy--;
      }
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }
}
