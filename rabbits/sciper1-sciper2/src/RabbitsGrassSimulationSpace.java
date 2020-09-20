import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
    private Object2DGrid grassSpace;
    private int maxGrassPerCell;


    public RabbitsGrassSimulationSpace(int gridSize, int maxGrassPerCell){
        grassSpace = new Object2DGrid(gridSize, gridSize);
        for(int i = 0; i < gridSize; i++){
            for(int j = 0; j < gridSize; j++){
                grassSpace.putObjectAt(i,j, 0);
            }
        }
        this.maxGrassPerCell = maxGrassPerCell;
    }

    public void initGrass(int number){
        int i = 0;
        while (i < number){
            int x = (int)(Math.random()*(grassSpace.getSizeX()));
            int y = (int)(Math.random()*(grassSpace.getSizeY()));

            int I;
            if(grassSpace.getObjectAt(x,y)!= null){
                I = (Integer) grassSpace.getObjectAt(x, y);
            }
            else{
                I = 0;
            }
            if (I < maxGrassPerCell){
                grassSpace.putObjectAt(x,y, I + 1);
                i++;
            }
        }
    }

    public Object2DGrid getCurrentGrassSpace(){
        return grassSpace;
    }
}
