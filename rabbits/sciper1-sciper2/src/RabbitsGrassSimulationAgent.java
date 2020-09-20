import java.awt.Color;

import RabbitsGrassSimulationSpace;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


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
	private grassSpace RabbitsGrassSimulationSpace;
	
    public void draw(SimGraphics arg0) {
    	arg0.drawFastRoundRect(Color.blue);

    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
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
    	
        if (random == 1){
            x--; // move west
        }
        else if (random == 2){
            x++; // move east
        }
        else if (random == 3){
            y--; // move south
        }
        else if (random == 4){
            y++; // move north
        }
      
        
      Object2DGrid grid = grassSpace.getCurrentGrassSpace();
      int newX = (x + grid.getSizeX()) % grid.getSizeX();
      int newY = (y + grid.getSizeY()) % grid.getSizeY();

      if(tryMove(newX, newY)){
        energy += grassSpace.takeGrassAt(x, y);
      }
      else{
        ///
      }
      energy--;
    }
    
    private boolean tryMove(int newX, int newY){
        return grassSpace.moveAgentAt(x, y, newX, newY);
    }

}
