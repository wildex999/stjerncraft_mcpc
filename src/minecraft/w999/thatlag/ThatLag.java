package w999.thatlag;

import java.util.ArrayList;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;


/*
 * ThatLag's function is to measure the time used for 
 * Entities, TileEntities and block changes(setBlock or setBlockMetadata),
 * and try to reduce these by skipping ticks for the thing using a lot of time.
 * 
 * For example, if the average server TPS < 19, and Entities are using 50% time, then skip ticks for
 * Entities until the average server TPS is ~20.
 * Likewise, if the free % goes up, increase Entity TPS towards 20 again.
 * 
 * The goal is to reduce Block lag and make the player experience more smooth
 * even in the event of an overloaded server, especially for spikes on peak hours.
 * 
 * 
 */

public class ThatLag {
    private ArrayList<Integer> previousTileEntityList = new ArrayList<Integer>();
    private ArrayList<Double> tileTPS = new ArrayList<Double>();
    int teToUpdate;
    int tileEntityCount;
    
    public boolean tileEntityEnabled = true; //Whether TPS control for tile entities enabled
    public boolean entityEnabled = true;
    
    //Calculate the number of Entities and TileEntities to update for this tick
    public void tick() {
    
    	tileEntityCount = 0;
        int previousTileEntityCount = 0; //Tile Entities updated previous tick
    	WorldServer[] worlds = DimensionManager.getWorlds();
    	for(World w : worlds)
    	{
    		tileEntityCount += w.loadedTileEntityList.size();
    		//Gather ACTUAL previous updates, might have been LESS than given(I.e, don't update more than number of tile entities!)
    		previousTileEntityCount += w.actualTileEntityCount;
    	}
    	
    	if(tileEntityCount == 0)
    		tileEntityCount = 1;
    	
    	
    	//Use tileEntityTime to figure out how many tileEntities to tick to maintain 20 TPS
    	teToUpdate = 0;
    	double avgTickTime = TimeWatch.getAvgTime(TimeWatch.TimeType.Tick);
    	//Overtime is the amount of time that we need to remove from tileEntities
    	double overTime = avgTickTime - 1.0; 
    	
    	//Calculate previousTileEntityCount average
		if(previousTileEntityList.size() >= 20)
			previousTileEntityList.remove(0);
		//Add new point
		previousTileEntityList.add(previousTileEntityCount);
		
		double cumulative = 0;
		for(long item : previousTileEntityList)
			cumulative += item;
		
		double avgCount = cumulative / (double)previousTileEntityList.size();
    	
    	if(avgCount == 0)
    		avgCount = 1; //Avoid division by zero
    		
        //Calculate time used per TileEntity. We time only those updated on the previous tick, so prevTime/previousTileEntityCount = time per tileEntity
        //We can't use avg as it would be very wrong if the previousTileEntityCount changed a lot per tick
        double tileEntityTime = TimeWatch.getAvgTime(TimeWatch.TimeType.TileEntity)/avgCount;
        
        if(tileEntityTime == 0.0)
        	tileEntityTime = 0.3; //15 ms(Avoid division by zero)
        	
        teToUpdate = (int) ((TimeWatch.getAvgTime(TimeWatch.TimeType.TileEntity)-overTime)/tileEntityTime);
        //Send in to world: (world.loadedTileEntityList.size()/tileEntityCount) * teToUpdate
        //Thus it's normalized, with tileEntityCount being the length
        
        if(teToUpdate < 0)
        	teToUpdate = 0;
        
        //Add TPS to list for calculating average
        if(tileTPS.size() >= 20)
        	tileTPS.remove(0);
        
        tileTPS.add(20.0 * ((double)previousTileEntityCount/(double)tileEntityCount));
        		
    	//TODO: For Entities, normalize the time used by TileEntities and Entities and use that(tileEntityTime = overTime*normalizedTileEntityPreviousTime)    	
    }
    
    //Set the number of items to update for the given world
    public void setWorldUpdateCount(WorldServer worldServer)
    {
    	if(tileEntityEnabled)
    		worldServer.remainingTileEntityCount += (teToUpdate * ((double)worldServer.loadedTileEntityList.size()/(double)tileEntityCount));
    	else
    		worldServer.remainingTileEntityCount = worldServer.loadedTileEntityList.size();
        //System.out.println("To Update: " + teToUpdate + " Sent: " + worldServer.remainingTileEntityCount + " Loaded: " + worldServer.loadedTileEntityList.size() + " Size: " + tileEntityCount);
    }
    
    public void ForceTileEntityTPS(short tps)
    {
    	
    }
    
    public double getAverageTileTPS()
    {
    	double avg = 0.0;
		for(double item : tileTPS)
			avg += item;
		return avg/(double)tileTPS.size();
    }
}
