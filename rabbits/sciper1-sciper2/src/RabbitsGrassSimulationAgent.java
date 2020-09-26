import java.awt.Color;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

    public static final int NORTH=0, EAST = 1, SOUTH = 2, WEST =3;

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
        return "Rabbit-" + ID;
    }
    
    public void setXY(int newX, int newY){
        x = newX;
        y = newY;
    }
    
    public int getEnergy() {
        return energy;
    }
    
    public int[] moveInDirection(int dir) {
    	
    	int newX  = x, newY = y;
    	
        if (dir == WEST){
        	newX--;
        }
        else if (dir == EAST){
        	newX++;
        }
        else if (dir == SOUTH){
        	newY--;
        }
        else if (dir == NORTH){
        	newY++;
        }
              
      Object2DGrid grid = space.getCurrentRabbitSpace();
      newX = (newX + grid.getSizeX()) % grid.getSizeX();
      newY = (newY + grid.getSizeY()) % grid.getSizeY();
      
      return new int[] {newX, newY};

    }
    
    public void moveAt(int newX, int newY) {
    	
    	setXY(newX, newY);
    	energy += space.takeGrassAt(newX, newY);
        energy--;
        
    }
    
    /**
     * moves if cell is not occupied 
     * after 4 random trial, stays at same position
     */
    public void step(){
		List<Integer> intList = Arrays.asList(NORTH, EAST, SOUTH, WEST);
		Collections.shuffle(intList);
		
		for (int i: intList) {
			
			int res[] = moveInDirection(i);
			
			if(space.moveRabbitAt(x, y, res[0], res[1])){
	    		  moveAt(res[0], res[1]);
	    		  break; 
	          }
		}
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }
}
