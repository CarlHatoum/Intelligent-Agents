import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
    private Object2DGrid grassSpace;

    public RabbitsGrassSimulationSpace(int gridSize){
        grassSpace = new Object2DGrid(gridSize, gridSize);
        for(int i = 0; i < gridSize; i++){
            for(int j = 0; j < gridSize; j++){
                grassSpace.putObjectAt(i,j, 0);
            }
        }
    }

    public void initGrass(int number){
        // TODO
    }
}
