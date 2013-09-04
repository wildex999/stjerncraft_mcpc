package net.minecraft.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.minecraft.server.ChunkSampler.ChunkSamples;
import net.minecraft.world.World;

/*--ChunkSampler
 * Gather number of updating Blocks(Scheduled), Entities and TileEntities for every chunk,
 * and sample the time used per chunk to run these.
 * (Does not include random block ticks)
 * 
 * Makes it possible to get a list of chunks sorted by the potential load they 
 * put on the server.
 * 
 */

public class ChunkSampler {
	static HashMap<String, ChunkSamples> chunks = new HashMap<String, ChunkSamples>();
	
	public static final int chunkSizeX = 16;
	public static final int chunkSizeZ = 16;
	
	public static Date startTime, stopTime; //Date and time that sampling started and stopped
	public static String startedBy; //Player who started sampling
	public static boolean sampling = false;
	
	public enum SortObject {
		BLOCK, ENTITY, TILEENTITY
	};
	public static SortObject sortObject = SortObject.ENTITY;
	
	public enum SortType {
		MIN, AVG, MAX
	};
	public static SortType sortType = SortType.AVG;
	
	public enum SortOrder {
		DESC, ASC
	};
	public static SortOrder sortOrder = SortOrder.DESC;
	
	//Comparator for sorting list
	static Comparator<ChunkSamples> comparator = new Comparator<ChunkSamples>() {

        public int compare(ChunkSamples o1, ChunkSamples o2) {
        	int o1count = getCountBySorting(o1);
        	int o2count = getCountBySorting(o2);
        	
        	if(sortOrder == SortOrder.DESC)
        	{
	            if(o1count < o2count) return 1;
	            else if(o1count > o2count) return -1;
	            else return 0;
        	}
        	else
        	{
	            if(o1count > o2count) return 1;
	            else if(o1count < o2count) return -1;
	            else return 0;
        	}
        }
        
        private int getCountBySorting(ChunkSamples chunk)
        {
        	switch(sortObject)
        	{
        	case BLOCK:
        		switch(sortType)
        		{
        		case MIN:
        			return chunk.minBlockCount;
        		case AVG:
        			return (int)chunk.avgBlockCount;
        		case MAX:
        			return chunk.maxBlockCount;
        		}
        		break;
        	case ENTITY:
        		switch(sortType)
        		{
        		case MIN:
        			return chunk.minEntityCount;
        		case AVG:
        			return (int)chunk.avgEntityCount;
        		case MAX:
        			return chunk.maxEntityCount;
        		}
        		break;
        	case TILEENTITY:
        		switch(sortType)
        		{
        		case MIN:
        			return chunk.minTileEntityCount;
        		case AVG:
        			return (int)chunk.avgTileEntityCount;
        		case MAX:
        			return chunk.maxTileEntityCount;
        		}
        		break;
        	}
        	return 0;
        }
    };
	
	//Clears all existing data and starts a new sampling session
	//username = name of users who started the sampling
	public static boolean startSampling(String username)
	{
		if(sampling == true)
			return false;
		
		chunks.clear();
		
		startTime = new Date();
		stopTime = null;
		startedBy = username;
		sampling = true;
		
		return true;
	}
	
	//Stops gathering sampling data
	public static boolean stopSampling()
	{
		if(sampling == false)
			return false;
		stopTime = new Date();
		sampling = false;
		return true;
	}
	
	//Add a block to the count of ticked blocks for the chunk
	public static void tickedBlock(World world, int chunkX, int chunkZ)
	{
		if(!sampling)
			return;
		ChunkSamples chunk = ChunkSampler.getOrCreateChunkSamples(world, chunkX, chunkZ);
		chunk.currentBlockCount++;
	}
	
	//Add a entity to the count of ticked entities for the chunk
	public static void tickedEntity(World world, int chunkX, int chunkZ)
	{
		if(!sampling)
			return;
		ChunkSamples chunk = ChunkSampler.getOrCreateChunkSamples(world, chunkX, chunkZ);
		chunk.currentEntityCount++;
	}
	
	//Add a tileentity to the count of ticked tileentities for the chunk
	public static void tickedTileEntity(World world, int chunkX, int chunkZ)
	{
		if(!sampling)
			return;
		ChunkSamples chunk = ChunkSampler.getOrCreateChunkSamples(world, chunkX, chunkZ);
		chunk.currentTileEntityCount++;
	}
	
	//Add time as a sampled time for this chunk
	public static void addSampledTime(World world, int chunkX, int chunkZ)
	{
		if(!sampling)
			return;
	}
	
	public static void nextTick()
	{
		if(!sampling)
			return;
		
		//Clear current count, keeping only low, avg and max
		Iterator it = chunks.entrySet().iterator();
		while(it.hasNext())
		{
			ChunkSamples chunk = ((Map.Entry<String, ChunkSamples>)it.next()).getValue();
			
			//Calculate Running Average
			float blockcount = (float)chunk.avgBlockCount * (float)chunk.ticks;
			blockcount += chunk.currentBlockCount;
			chunk.avgBlockCount = ((float)blockcount / (float)(chunk.ticks+1));
			
			float entitycount = (float)chunk.avgEntityCount * (float)chunk.ticks;
			entitycount += chunk.currentEntityCount;
			chunk.avgEntityCount = ((float)entitycount / (float)(chunk.ticks+1));
			
			float tileentitycount = (float)chunk.avgTileEntityCount * (float)chunk.ticks;
			tileentitycount += chunk.currentTileEntityCount;
			chunk.avgTileEntityCount = ((float)tileentitycount / (float)(chunk.ticks+1));
			
			if(chunk.currentBlockCount < chunk.minBlockCount)
				chunk.minBlockCount = chunk.currentBlockCount;
			else if(chunk.currentBlockCount > chunk.maxBlockCount)
				chunk.maxBlockCount = chunk.currentBlockCount;
			
			if(chunk.currentEntityCount < chunk.minEntityCount)
				chunk.minEntityCount = chunk.currentEntityCount;
			else if(chunk.currentEntityCount > chunk.maxEntityCount)
				chunk.maxEntityCount = chunk.currentEntityCount;
			
			if(chunk.currentTileEntityCount < chunk.minTileEntityCount)
				chunk.minTileEntityCount = chunk.currentTileEntityCount;
			else if(chunk.currentTileEntityCount > chunk.maxTileEntityCount)
				chunk.maxTileEntityCount = chunk.currentTileEntityCount;
			
			chunk.ticks++;
			chunk.currentBlockCount = 0;
			chunk.currentEntityCount = 0;
			chunk.currentTileEntityCount = 0;
		}
	}
	
	public static List<ChunkSamples> getList()
	{
		List<ChunkSamples> list = new ArrayList<ChunkSamples>(chunks.values());
		
		//Sort
		Collections.sort(list, comparator);
		
		
		return list;
	}
	
	private static ChunkSamples getOrCreateChunkSamples(World world, int chunkX, int chunkZ)
	{
		String chunkstr = world.worldInfo.getDimension() + ":" + chunkX + ":" + chunkZ;
		ChunkSamples chunk = chunks.get(chunkstr);
		
		if(chunk == null)
		{
			//Create
			chunk = new ChunkSampler.ChunkSamples(world, chunkX, chunkZ);
			chunks.put(chunkstr, chunk);
		}
		
		return chunk;
	}
	
	public static class ChunkSamples {
		public World world;
		public int chunkX, chunkZ;
		int currentBlockCount, currentEntityCount, currentTileEntityCount;
		public float avgBlockCount, avgEntityCount, avgTileEntityCount;
		public int minBlockCount, maxBlockCount;
		public int minEntityCount, maxEntityCount;
		public int minTileEntityCount, maxTileEntityCount;
		int ticks; //Number of ticks entities in this chunk has been counted(Used to calculate avg)
		
		public ChunkSamples(World world, int chunkX, int chunkZ)
		{
			this.world = world;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
			
			currentBlockCount = currentEntityCount = currentTileEntityCount = 0;
			maxBlockCount = maxEntityCount = maxTileEntityCount = 0;
			avgBlockCount = avgEntityCount = avgTileEntityCount = 0;
			minBlockCount = minEntityCount = minTileEntityCount = Integer.MAX_VALUE;
			
			ticks = 0;
		}
	}
	
}
