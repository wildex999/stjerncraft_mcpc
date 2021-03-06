package net.minecraft.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import w999.baseprotect.PlayerData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.ChunkSampler.ChunkSamples;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/*--ChunkSampler
 * Gather number of updating Blocks(Scheduled), Entities and TileEntities for every chunk,
 * and sample the time used per chunk to run these.
 * (Does not include random block ticks)
 * 
 * Makes it possible to get a list of chunks sorted by the potential load they 
 * put on the server.
 * 
 * 
 * Description of sampling method:
 * The initial idea was to have an external thread that checked the current item(NULL, Block, Entity, Tileentity) currently
 * ticking. However, this would cause a lot of overhead since the main thread would have to store the position and world of the currently ticking item before and after
 * each tick, and this would need to happen inside a lock.
 * Then the question becomes, what has most overhead? nanotime or locking?
 * 
 * Instead we will use atomic Increment on the sampler(Timer thread) side, with getandset on reader(Main thread) side.
 * So when the sampler will call Atomic increment 1000 times a second, and the main thread will getandset before and after 
 * every item tick, to specify where to sample.
 * 
 */

public class ChunkSampler {
	static HashMap<String, ChunkSamples> chunks = new HashMap<String, ChunkSamples>(); //Samples for chunks
	static HashMap<String, ItemSample> customSamples = new HashMap<String, ItemSample>(); //Custom named samples
	static HashMap<String, PlayerSamples> players = new HashMap<String, PlayerSamples>(); //Player samples
	
	static Timer samplerThreadTimer = new Timer(true); //Create the timer thread as a daemon
	static SamplerTask task; //Currently running task
	public static int samplingInterval = 1; //Time in millisecond between time sampling
	public static int detailItemCount = 5; //Number of items to print when printing chunk details
	
	public static AtomicInteger atomicSamples = new AtomicInteger(); //Samples since last count
	public static long totalSamples; //Total number of samples by Sampling thread
	public static long freeSamples; //Samples that did not happen on any item(Free time)
	private static long ticks; //Number of ticks the sammpling has been running
	
	public static Date startTime, stopTime; //Date and time that sampling started and stopped
	public static String startedBy; //Player who started sampling
	public static boolean sampling = false;
	
	public enum SortObject {
		BLOCK, ENTITY, TILEENTITY, TIME
	};
	public static SortObject sortObject = SortObject.TIME;
	
	public enum SortType {
		MIN, AVG, MAX, TIME
	};
	public static SortType sortType = SortType.AVG;
	
	public enum SortOrder {
		DESC, ASC
	};
	public static SortOrder sortOrder = SortOrder.DESC;
	
	//Comparator for sorting list
	static Comparator<ChunkSamples> comparator = new Comparator<ChunkSamples>() {

        public int compare(ChunkSamples o1, ChunkSamples o2) {
        	float o1count = getCountBySorting(o1);
        	float o2count = getCountBySorting(o2);
        	
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
        
        private float getCountBySorting(ChunkSamples chunk)
        {
        	switch(sortObject)
        	{
        	case BLOCK:
        		switch(sortType)
        		{
        		case MIN:
        			return chunk.minBlockCount;
        		case AVG:
        			return chunk.avgBlockCount;
        		case MAX:
        			return chunk.maxBlockCount;
        		case TIME:
        			return (float)chunk.totalBlockSampleCount / (float)totalSamples;
        		}
        		break;
        	case ENTITY:
        		switch(sortType)
        		{
        		case MIN:
        			return chunk.minEntityCount;
        		case AVG:
        			return chunk.avgEntityCount;
        		case MAX:
        			return chunk.maxEntityCount;
        		case TIME:
        			return (float)chunk.totalEntitySampleCount / (float)totalSamples;
        		}
        		break;
        	case TILEENTITY:
        		switch(sortType)
        		{
        		case MIN:
        			return chunk.minTileEntityCount;
        		case AVG:
        			return chunk.avgTileEntityCount;
        		case MAX:
        			return chunk.maxTileEntityCount;
        		case TIME:
        			return (float)chunk.totalTileEntitySampleCount / (float)totalSamples;
        		}
        		break;
        	case TIME:
        		return (float)(chunk.totalBlockSampleCount + chunk.totalEntitySampleCount + chunk.totalTileEntitySampleCount) / (totalSamples);
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
		customSamples.clear();
		players.clear();
		
		startTime = new Date();
		stopTime = null;
		startedBy = username;
		sampling = true;
		
		totalSamples = 0;
		freeSamples = 0;
		ticks = 0;
		atomicSamples.set(0);
		task = new SamplerTask();
		samplerThreadTimer.scheduleAtFixedRate(task, samplingInterval, samplingInterval);
		
		return true;
	}
	
	//Stops gathering sampling data
	public static boolean stopSampling()
	{
		if(sampling == false)
			return false;
		stopTime = new Date();
		sampling = false;
		
		//Stop the timer task
		task.cancel();
		
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
	
	//Pre-Sample will sample before items, so any samples at this point are "Free" time.
	public static void preSample()
	{
		if(!sampling)
			return;
		
		//Get samples and reset
		int samples = atomicSamples.getAndSet(0);
		freeSamples += samples;
		totalSamples += samples;
	}
	
	//Pre-sample, put samples at a custom "item"
	public static void preSample(String customName)
	{
		if(!sampling)
			return; 
		
		if(customName == null || customName.equals(""))
		{
			preSample(); //Mark it as free time
			return;
		}
		//Get samples and reset
		int samples = atomicSamples.getAndSet(0);
		totalSamples += samples;
		
		//Get the custom sample and add the samples(Create if doesn't exist)
		ItemSample sample = getCustomSample(customName, true);
		sample.samples += samples;
	}
	
	//Return a list of custom samplers
	public static List<ItemSample> getCustomSamples()
	{
		return ChunkSamples.getSortedItems(customSamples);
	}
	
	//Returns a Custom Sample with the given name, if create = true it will create it if it doesn't exist
	public static ItemSample getCustomSample(String name, boolean create)
	{
		ItemSample sample = customSamples.get(name);
		if(sample == null)
		{
			if(create)
			{
				sample = new ItemSample(name);
				customSamples.put(name, sample);
			}
		}
		
		return sample;
	}
	
	//Post-Sample will sample after Blocks, so any samples at this point happened while the item was ticking
	public static void postSampleBlock(World world, int chunkX, int chunkZ, int blockId)
	{
		if(!sampling)
			return;
		
		//Get samples and reset
		int samples = atomicSamples.getAndSet(0);
		
		if(samples > 0)
		{
			//Place the samples to the chunk and the item type
			ChunkSamples chunk = getOrCreateChunkSamples(world, chunkX, chunkZ);
			chunk.incrementItemSamples(chunk.blockItems, "BlockId " + blockId, samples);
			
			//Add to Item sampled
			getCustomSample("ItemSampled", true).samples += samples;
			
			chunk.totalBlockSampleCount += samples;
			totalSamples += samples;
		}
	}
	
	//Post-Sample will sample after Entities, so any samples at this point happened while the item was ticking
	public static void postSampleEntity(World world, int chunkX, int chunkZ, Entity entity)
	{
		if(!sampling)
			return;
		
		//Get samples and reset
		int samples = atomicSamples.getAndSet(0);
		
		if(samples > 0)
		{
			//Place the samples to the chunk and the item type
			ChunkSamples chunk = getOrCreateChunkSamples(world, chunkX, chunkZ);
			
			//Add to Item sampled
			getCustomSample("ItemSampled", true).samples += samples;
			
			chunk.totalEntitySampleCount += samples;
			chunk.incrementItemSamples(chunk.entityItems, entity.getClass().getName(), samples);
			totalSamples += samples;
			
			//Check if owner by a player, and add to it's samples count
			PlayerData owner = entity.getItemOwner();
			if(owner != null)
			{
				EntityPlayerMP player = owner.getPlayer();
				if(player != null)
					addPlayerSamples(player.username, chunk, samples);
			}
		}
	}
	
	//Post-Sample will sample after TileEntity, so any samples at this point happened while the item was ticking
	public static void postSampleTileEntity(World world, int chunkX, int chunkZ, TileEntity tileEntity)
	{
		if(!sampling)
			return;
		
		//Get samples and reset
		int samples = atomicSamples.getAndSet(0);
		
		
		if(samples > 0)
		{
			//Place the samples to the chunk and the item type
			ChunkSamples chunk = getOrCreateChunkSamples(world, chunkX, chunkZ);
			
			//Add to Item sampled
			getCustomSample("ItemSampled", true).samples += samples;
			
			chunk.totalTileEntitySampleCount += samples;
			chunk.incrementItemSamples(chunk.tileEntityItems, tileEntity.getClass().getName(), samples);
			totalSamples += samples;
			
			//Check if owner by a player, and add to it's samples count
			PlayerData owner = tileEntity.getItemOwner();
			if(owner != null)
			{
				EntityPlayerMP player = owner.getPlayer();
				if(player != null)
					addPlayerSamples(player.username, chunk, samples);
			}
		}
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
		Iterator<Entry<String, ChunkSamples>> it = chunks.entrySet().iterator();
		while(it.hasNext())
		{
			ChunkSamples chunk = it.next().getValue();
			
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
		
		//Automaticly turn off chunk sampling if it has been running for 10 minutes(~12000 ticks at 20 tps)
		if(++ticks >= 12000)
			stopSampling();
	}
	
	public static List<ChunkSamples> getList(String player)
	{
		//Check if we are to return every chunk or only those for specific player
		List<ChunkSamples> list = null;
		if(player.length() == 0)
			list = new ArrayList<ChunkSamples>(chunks.values());
		else
		{
			PlayerSamples playerSamples = players.get(player);
			if(playerSamples == null)
				return null;
			
			list = new ArrayList<ChunkSamples>(playerSamples.chunks.values());
		}
		
		//Sort
		Collections.sort(list, comparator);
		
		
		return list;
	}
	
	public static ChunkSamples getChunkSamples(int worlddim, int chunkX, int chunkZ)
	{
		String chunkstr = worlddim + ":" + chunkX + ":" + chunkZ;
		return chunks.get(chunkstr);
	}
	
	public static ChunkSamples createChunkSamples(int worlddim, int chunkX, int chunkZ)
	{
		String chunkstr = worlddim + ":" + chunkX + ":" + chunkZ;
		ChunkSamples chunk = new ChunkSamples(worlddim, chunkX, chunkZ);
		chunks.put(chunkstr, chunk);
		return chunk;
	}
	
	private static ChunkSamples getOrCreateChunkSamples(World world, int chunkX, int chunkZ)
	{
		ChunkSamples chunk = getChunkSamples(world.worldInfo.getDimension(), chunkX, chunkZ);
		
		if(chunk == null)
			return createChunkSamples(world.worldInfo.getDimension(), chunkX, chunkZ); 
		
		return chunk;
	}
	
	private static PlayerSamples getOrCreatePlayerSamples(String player)
	{
		PlayerSamples playersamples = players.get(player);
		
		if(playersamples == null)
		{
			playersamples = new PlayerSamples(player);
			players.put(player, playersamples);
		}
		
		return playersamples;
	}
	
	private static void addPlayerSamples(String player, ChunkSamples chunk, long samples)
	{
		if(player.isEmpty())
			return ;
		
		//Get or create PlayerSamples
		PlayerSamples playersamples = getOrCreatePlayerSamples(player);
		
		//Check if chunk exists, add if it doesn't
		if(chunk != null)
		{
			ChunkSamples chunksampler = playersamples.chunks.get(chunk);
			if(chunksampler == null)
				playersamples.chunks.put(chunk, chunk);
		}
		
		//Add samples
		playersamples.samplesCount += samples;
		
	}
	
	//Contain samples for a single chunk
	public static class ChunkSamples {
		public int world;
		public int chunkX, chunkZ;
		int currentBlockCount, currentEntityCount, currentTileEntityCount;
		public long totalBlockSampleCount, totalEntitySampleCount, totalTileEntitySampleCount; //Total samples from sampling thread of the different types
		public float avgBlockCount, avgEntityCount, avgTileEntityCount;
		public int minBlockCount, maxBlockCount;
		public int minEntityCount, maxEntityCount;
		public int minTileEntityCount, maxTileEntityCount;
		int ticks; //Number of ticks entities in this chunk has been counted(Used to calculate avg)
		
		public HashMap<String, ItemSample> blockItems, entityItems, tileEntityItems; //List of item classes and their sample count, for detailed chunk list
		
		public ChunkSamples(int world, int chunkX, int chunkZ)
		{
			this.world = world;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
			
			totalBlockSampleCount = totalEntitySampleCount = totalTileEntitySampleCount = 0;
			
			currentBlockCount = currentEntityCount = currentTileEntityCount = 0;
			maxBlockCount = maxEntityCount = maxTileEntityCount = 0;
			avgBlockCount = avgEntityCount = avgTileEntityCount = 0;
			minBlockCount = minEntityCount = minTileEntityCount = Integer.MAX_VALUE;
			
			
			blockItems = new HashMap<String, ItemSample>();
			entityItems = new HashMap<String, ItemSample>();
			tileEntityItems = new HashMap<String, ItemSample>();
			
			ticks = 0;
		}
		
		//Increment the sample count of item with given class name in map, creates if item doesn't exist
		public void incrementItemSamples(HashMap<String,ItemSample> map, String className, int samples)
		{
			ItemSample itemSamples = map.get(className);
			
			if(itemSamples == null)
			{
				//Create sample
				itemSamples = new ItemSample(className);
				map.put(className, itemSamples);
			}
			itemSamples.samples+=samples;
		}
		
		//Item comparator
		static Comparator<ItemSample> itemcomparator = new Comparator<ItemSample>() {

	        public int compare(ItemSample i1, ItemSample i2) {
	        	if(i1.samples < i2.samples) return 1;
	        	else if(i1.samples > i2.samples) return -1;
	        	else return 0;
	        }
	        
	    };
		
		//Get list of indexes sorted according to the hashmap
		public static List<ItemSample> getSortedItems(HashMap<String,ItemSample> map)
		{
			List<ItemSample> list = new ArrayList<ItemSample>(map.values());
			
			Collections.sort(list, itemcomparator);
			
			return list;
		}
		
		
	}
	
	//Contains samples for sampled for a player
	public static class PlayerSamples {
		public HashMap<ChunkSamples, ChunkSamples> chunks = new HashMap<ChunkSamples, ChunkSamples>();
		public long samplesCount = 0; //Number of total samples for this player
		public String username;
		
		PlayerSamples(String name) 
		{
			username = name;
		}
		
		//Player comparator
		static Comparator<PlayerSamples> playercomparator = new Comparator<PlayerSamples>() {
			
	        public int compare(PlayerSamples i1, PlayerSamples i2) {
	        	if(i1.samplesCount < i2.samplesCount) return 1;
	        	else if(i1.samplesCount > i2.samplesCount) return -1;
	        	else return 0;
	        }
	        
		};
		
		//Get list of players sorted by the time used
		public static List<PlayerSamples> getSortedPlayers()
		{
			List<PlayerSamples> list = new ArrayList<PlayerSamples>(players.values());
			
			Collections.sort(list, playercomparator);
			
			return list;
		}
		
	}
	
	private static class SamplerTask extends TimerTask {

		@Override
		public void run() {
			//Increment the sampling counter
			ChunkSampler.atomicSamples.getAndIncrement();
		}
		
	}
	
	//Wrap the int in a class that we can pass by reference, we also include a String to allow us to return it easily in getSortedItems
	public static class ItemSample {
		public int samples;
		public String className;
		
		public ItemSample(String name) { samples = 0; className=name; };
	}
	
}
