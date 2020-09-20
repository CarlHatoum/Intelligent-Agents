import java.awt.Color;

import RabbitsGrassSimulationSpace;
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
	private int ID;
	private RabbitsGrassSimulationSpace space;
	
	public RabbitsGrassSimulationAgent(){
		/**
		 * Constructor to be implemented
		 * x y random ?
		 * Fixed initial energy ?
		 */
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
    	
    	int random =  (int)Math.random()*4+1;
    	int newX  = x, newY = y;
    	
        if (random == 1){
        	newX = x--; // move west
        }
        else if (random == 2){
        	newX = x++; // move east
        }
        else if (random == 3){
        	newY = y--; // move south
        }
        else if (random == 4){
        	newY = y++; // move north
        }
      
        
      Object2DGrid grid = space.getCurrentRabbitSpace();
      newX = (x + grid.getSizeX()) % grid.getSizeX();
      newY = (y + grid.getSizeY()) % grid.getSizeY();

      if(space.moveRabbitAt(x, y, newX, newY)){
        energy += space.takeGrassAt(newX, newY);
        setXY(newX, newY);
      }
      else{
        ///
      }
      
      energy--;
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }
}
