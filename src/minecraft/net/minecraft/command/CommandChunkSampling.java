package net.minecraft.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.server.ChunkSampler;
import net.minecraft.server.ChunkSampler.ChunkSamples;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.VanillaCommand;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;

public class CommandChunkSampling extends VanillaCommand {
	private static final List<String> CHUNKSAMPLING_COMMANDS = ImmutableList.of("start", "stop", "list", "sort");
	private static int itemsPerPage = 10;
	private static int currentPage = 0;
	
	public CommandChunkSampling() {
        super("chunksampling");
        this.description = "Sample resources used by chunks and then list chunks sorted by resource usage.";
        this.usageMessage = "/chunksampling <start, stop, list, listchunk, warnslow, sort>\n"
        		+ "		Start - Start sampling\n"
        		+ "		Stop - Stop sampling\n"
        		+ "		Sort [page] - Set the sorting method to use when listing\n"
        		+ "		List - Return a sorted list of chunks placed into pages"
        		+ "     ListChunk [detailed] - Show the chunk at the given coordinates, detailed will show the most ticket item of each type."
        		+ "     WarnSlow - Print a warning when an item uses more than n% time";
        this.setPermission("mcpc.command.chunksampling");
    }
	
	@Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if (!testPermission(sender)) return true;
        
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
            return false;
        }
        else if(args.length == 1 && args[0].equals("sort"))
        {
        	sender.sendMessage(ChatColor.RED + "Usage: " + "/chunksampling sort <block, entity, tileentity, time> [min, avg, max, time] [desc, asc]");
        	return false;
        }
        
        String action = args[0];
        
        if(action.equalsIgnoreCase("start"))
        {
        	//Check if it's already sampling
        	if(ChunkSampler.sampling)
        	{
        		sender.sendMessage(ChatColor.WHITE + "Chunk Sampling already running, started at " + ChatColor.RED + 
        				ChunkSampler.startTime + ChatColor.WHITE + " by " + ChatColor.BLUE + ChunkSampler.startedBy);
        		return false;
        	}
        	ChunkSampler.startSampling(sender.getName());
        	currentPage = 0;
        	sender.sendMessage(ChatColor.GREEN + "Chunk Sampling started!");
        }
        else if(action.equalsIgnoreCase("stop"))
        {
        	if(!ChunkSampler.sampling)
        	{
        		sender.sendMessage(ChatColor.RED + "Chunk Sampling not started, type '/chunksampling start' to start sampling.");
        		return false;
        	}
        	ChunkSampler.stopSampling();
        	sender.sendMessage(ChatColor.GREEN + "Chunk Sampling stopped!");
        }
        else if(action.equalsIgnoreCase("list"))
        {
        	if(ChunkSampler.sampling)
        	{
        		sender.sendMessage(ChatColor.RED + "Stop sampling before listing sampled data!");
        		return false;
        	}
        	
        	if(args.length == 2)
        	{
        		//Page supplied
        		try {
        			currentPage = Integer.parseInt(args[1]);
                	if(currentPage <= 0)
                	{
                		sender.sendMessage(ChatColor.RED + "First page is 1!");
                		return false;
                	}
                	currentPage -= 1; //(Index starts at 0)
        		} catch (NumberFormatException error)
        		{
        			sender.sendMessage(ChatColor.RED + "Page Argument is not a number, ignoring!");
        		}
        	}
        	
        	
        	
        	/*
        	 * World | Chunk XZ| min   |  avg   |  max  | Time
        	 * ------------------------------------------             
        	 * Blocks          |
        	 * Entities        |
        	 * TileEntities    |
        	 * Chunk Time      |
        	 */
        	
        	sender.sendMessage("(Chunk Pos) | Min | Avg | Max");
        	List<ChunkSampler.ChunkSamples> list = ChunkSampler.getList();
        	
        	if(currentPage*itemsPerPage >= list.size() || currentPage*itemsPerPage < 0)
        	{
        		sender.sendMessage("Page does not exist!");
        		return false;
        	}
        	
        	
        	//We want the bottom of chat to show the first of the items on the page, so we have to reverse part of the list
        	int i=(currentPage*itemsPerPage)+itemsPerPage-1;
        	if(i>=list.size())
        		i = list.size()-1;
        	
        	for(; i>=currentPage*itemsPerPage; i--)
        	{
        		ChunkSampler.ChunkSamples chunk = list.get(i);
        		float totalChunkSamples = chunk.totalBlockSampleCount + chunk.totalEntitySampleCount + chunk.totalTileEntitySampleCount;
        		//Print top info
        		sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.DARK_GREEN + chunk.world.getWorldInfo().getWorldName() + ChatColor.GOLD + " - " + ChatColor.DARK_GREEN + ((chunk.chunkX*16)+8) + " : " + ((chunk.chunkZ*16)+8) + ChatColor.GOLD + "] | Min | Avg | Max | Time");
        		sender.sendMessage(ChatColor.YELLOW + "Blocks     | " + ChatColor.GREEN + chunk.minBlockCount + ChatColor.GOLD + " | " + ChatColor.GREEN + chunk.avgBlockCount + ChatColor.GOLD +  " | " + ChatColor.GREEN + chunk.maxBlockCount + ChatColor.GOLD + " | " + ChatColor.GREEN + (((float)chunk.totalBlockSampleCount/(float)ChunkSampler.totalSamples)*100) + "%");
        		sender.sendMessage(ChatColor.GOLD + "Entity     | " + ChatColor.DARK_GREEN + chunk.minEntityCount + ChatColor.GOLD + " | " + ChatColor.DARK_GREEN + chunk.avgEntityCount + ChatColor.GOLD +  " | " + ChatColor.DARK_GREEN + chunk.maxEntityCount + ChatColor.GOLD + " | " + ChatColor.DARK_GREEN + (((float)chunk.totalEntitySampleCount/(float)ChunkSampler.totalSamples)*100) + "%");
        		sender.sendMessage(ChatColor.YELLOW + "TileEntity | " + ChatColor.GREEN + chunk.minTileEntityCount + ChatColor.GOLD + " | " + ChatColor.GREEN + chunk.avgTileEntityCount + ChatColor.GOLD +  " | " + ChatColor.GREEN + chunk.maxTileEntityCount + ChatColor.GOLD + " | " + ChatColor.GREEN + (((float)chunk.totalTileEntitySampleCount/(float)ChunkSampler.totalSamples)*100) + "%");
        		sender.sendMessage(ChatColor.GOLD + "Chunk Time | " + ChatColor.DARK_GREEN + ((totalChunkSamples / (float)ChunkSampler.totalSamples)*100) + "%");
        		sender.sendMessage(ChatColor.GOLD + "--------------------------------------------------");
        	}
        	sender.sendMessage(ChatColor.WHITE + "Page " + ChatColor.RED + (currentPage+1) + ChatColor.WHITE + " of " + ChatColor.RED + ((list.size()/itemsPerPage)+1));
        	sender.sendMessage(ChatColor.WHITE + "Unused time: " + ChatColor.RED + (((float)ChunkSampler.freeSamples / (float)ChunkSampler.totalSamples)*100) + "%" + ChatColor.WHITE + " ( " + ChunkSampler.freeSamples + " of " + ChunkSampler.totalSamples + " )" );
        	sender.sendMessage(ChatColor.WHITE + "Sampled from " + ChatColor.RED + 
        				ChunkSampler.startTime + ChatColor.WHITE + " to " + ChatColor.RED + ChunkSampler.stopTime + ChatColor.WHITE +  " by " + ChatColor.BLUE + ChunkSampler.startedBy);
        }
        else if(action.equalsIgnoreCase("sort"))
        {
        	if(args.length < 2)
        	{
        		sender.sendMessage(ChatColor.RED + "At least 1 arguments needed for /chunksampling sort!");
        		return false;
        	}
        	
        	//Get sort object
        	if(args[1].equalsIgnoreCase("block"))
        		ChunkSampler.sortObject = ChunkSampler.SortObject.BLOCK;
        	else if(args[1].equalsIgnoreCase("entity"))
        		ChunkSampler.sortObject = ChunkSampler.SortObject.ENTITY;
        	else if(args[1].equalsIgnoreCase("tileentity"))
        		ChunkSampler.sortObject = ChunkSampler.SortObject.TILEENTITY;
        	else if(args[1].equalsIgnoreCase("time"))
        		ChunkSampler.sortObject = ChunkSampler.SortObject.TIME;
        	else
        	{
        		sender.sendMessage(ChatColor.RED + "Invalid Argument 1 sent to /chunksampling sort! Must be one of [block, entity, tileentity, time]!");
        		return false;
        	}
        	
        	//Get sort type
        	if(args.length == 3)
        	{
	        	if(args[2].equalsIgnoreCase("min"))
	        		ChunkSampler.sortType = ChunkSampler.SortType.MIN;
	        	else if(args[2].equalsIgnoreCase("avg"))
	        		ChunkSampler.sortType = ChunkSampler.SortType.AVG;
	        	else if(args[2].equalsIgnoreCase("max"))
	        		ChunkSampler.sortType = ChunkSampler.SortType.MAX;
	        	else if(args[2].equalsIgnoreCase("time"))
	        		ChunkSampler.sortType = ChunkSampler.SortType.TIME;
	        	else
	        	{
	        		sender.sendMessage(ChatColor.RED + "Invalid Argument 2 sent to /chunksampling sort! Must be one of [min, avg, max, time]!");
	        		return false;
	        	}  
        	}
        	
        	//Sorting order
        	if(args.length == 4)
        	{
        		if(args[3].equalsIgnoreCase("asc"))
        			ChunkSampler.sortOrder = ChunkSampler.SortOrder.ASC;
        		else if(args[3].equalsIgnoreCase("desc"))
        			ChunkSampler.sortOrder = ChunkSampler.SortOrder.DESC;
        		else
        		{
        			sender.sendMessage(ChatColor.RED + "Invalid Argument 3 sent to /chunksampling sort! Must be one of [asc, desc]!");
            		return false;
        		}
        	}
        	currentPage = 0;
        	sender.sendMessage("ChunkSampling Sorting set! Write /chunksampling list to get a sorted list");
        }
        else
        {
	        sender.sendMessage(ChatColor.RED + "Unknown Argument 1");
	        return false;
        }
        
        return true;
	}
	
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], CHUNKSAMPLING_COMMANDS, new ArrayList<String>(CHUNKSAMPLING_COMMANDS.size()));
        } else if (args.length == 2) {
            return super.tabComplete(sender, alias, args);
        }
        return ImmutableList.of();
    }

}
