package w999.baseprotect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

public class BaseProtect extends JavaPlugin {
	public static BaseProtect instance;
	private static IClaimManager claimManager;
	
	private List<Class> entities = new ArrayList<Class>(); //List of Entities to check
	private List<Class> tileEntities = new ArrayList<Class>(); //List of TileEntities to check
	private List<Class> items = new ArrayList<Class>(); //List of item(Used by Player) to check
	
	private int age; //Current age(= number of reloads)
	
	//players need to be accessed before BaseProtect can be initialized, therefore it's static
	private static HashMap<String, PlayerData> players = new HashMap<String, PlayerData>(); //Data for players
	
	//Special cases
	public static TempWorldInteractor tempInteractor = new TempWorldInteractor();
	public static Class ee3PacketHandler = null;
	public static Class stevescart = null;
	public static Class thaumgolem = null;
	public static Class infoPanel = null;
	public static Class infoPanelAdvanced = null;
	public static Class turtle = null;
	
	
	public enum InteractorType{
		Entity,
		TileEntity,
		PlayerItem
	}
	
	
	public BaseProtect(CraftServer server)
	{
		instance = this;
		
		//Register command handler
		server.getCommandMap().register("baseprotect2", new CommandBaseProtect());
		
		//Register event handlers
		//TODO: Don't depend on other plugins
		Bukkit.getPluginManager().registerEvents(new PlayerEventHandler(), Bukkit.getPluginManager().getPlugins()[0]);
		
		//Temp: Add some Interactors to watch(TODO: Make it load from config)
		addInteractor("ic2.core.item.tool.EntityMiningLaser", InteractorType.Entity);
		addInteractor("mods.railcraft.common.carts.EntityTunnelBore", InteractorType.Entity);
		addInteractor("vswe.stevescarts.Carts.MinecartModular", InteractorType.Entity);
		
		addInteractor("dan200.turtle.shared.TileEntityTurtle", InteractorType.TileEntity);
		addInteractor("dan200.turtle.shared.TileEntityTurtleExpanded", InteractorType.TileEntity);
		addInteractor("buildcraft.builders.TileFiller", InteractorType.TileEntity);
		
		addInteractor("bluedart.item.tool.ItemForceWrench", InteractorType.PlayerItem);
		addInteractor("bluedart.item.ItemTileBox", InteractorType.PlayerItem);
		addInteractor("gravisuite.ItemVajra", InteractorType.PlayerItem);
		addInteractor("thaumcraft.common.items.wands.ItemWandExcavation", InteractorType.PlayerItem);
		addInteractor("thaumcraft.common.items.wands.ItemWandFrost", InteractorType.PlayerItem);
		//Destruction Pickaxe?
		addInteractor("extrautils.item.ItemBuildersWand", InteractorType.PlayerItem);
		//addInteractor("com.pahimar.ee3.item.ItemMiniumStone", InteractorType.PlayerItem); //EE3 Special case
		addInteractor("thaumcraft.common.items.ItemPortableHole", InteractorType.PlayerItem);
		
		addInteractor("w999.baseprotect.TempWorldInteractor", InteractorType.Entity); //Used for special cases where we don't have an item to set as current
		
		//Register special event classes
		try {
			ee3PacketHandler = Class.forName("com.pahimar.ee3.network.PacketHandler");
			stevescart = Class.forName("vswe.stevescarts.Carts.MinecartModular");
			thaumgolem = Class.forName("thaumcraft.common.entities.golems.EntityGolemBase");
			infoPanel = Class.forName("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel");
			infoPanelAdvanced = Class.forName("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityAdvancedInfoPanel");
			turtle = Class.forName("dan200.turtle.shared.TileEntityTurtle");
		} catch (ClassNotFoundException e)
		{
			System.err.println("BASEPROTECT ERROR: Unable to register all special case classes, Claims might NOT be fully respected!");
		}
		
		System.out.println("BaseProtect Initialized!");
		
	}
	
	//Get Class from string, verify it's has IWorldInteracte(instanceof IWorldInteract) and store it in the list of entities
	public boolean addInteractor(String interactor, InteractorType type)
	{
		//Find class from name
		Class cl = null;
		try {
			cl = Class.forName(interactor);
		} catch (ClassNotFoundException e) {
			System.err.println("Could Find Class for " + interactor + ".");
			return false;
		}
		
		//Check InteractorType
		switch(type)
		{
		case Entity:
			entities.add(cl);
			break;
		case TileEntity:
			tileEntities.add(cl);
			break;
		case PlayerItem:
			items.add(cl);
			break;
			default:
				return false;
		}
		
		age++; //Changes have been done, increase the age
		
		System.out.println("BaseProtect, Added Interactor: " + interactor);
		
		return true;
	}
	
	public void removeInteractor(String interactor, InteractorType type)
	{
		age++;
		//TODO
	}
	
	//This will reload the Player data(Reload fakeplayer permissions)
	public static void reloadPlayerData()
	{
		//Go through PlayerData's and recreate the fakePlayer from scratch(TODO: Find a way to clear existing ones from fakePlayer list)
		//TODO: Test if sending join for fakePlayer after real player logs in will keep their permissions in sync(Give same permission object to both?)
	}
	
	//Check if entity has build(Place/break) permission in target claim
	public boolean claimCanBuild(WorldInteract interactor, Location loc)
	{
		if(claimManager == null)
			return true; //No claim manager = anything allowed
		
		//Is the interactor relevant to us?
		if(!isRelevant(interactor))
			return true; //Always return true if not

		return claimManager.claimCanBuild(interactor, loc);
	}
	
	public boolean claimCanInteract(WorldInteract interactor, Location loc)
	{
		if(claimManager == null)
			return true; //No claim manager = anything allowed
		
		//Is the interactor relevant to us?
		if(!isRelevant(interactor))
			return true;
		
		return claimManager.claimCanInteract(interactor, loc);
	}
	
	public boolean claimCanContainer(WorldInteract interactor, Location loc)
	{
		if(claimManager == null)
			return true; //No claim manager = anything allowed
		
		//Is the interactor relevant to us?
		if(!isRelevant(interactor))
			return true;
		
		return claimManager.claimCanContainer(interactor, loc);
	}
	
	//Emit a full Block Break event(For any plugin), return true is not cancelled
	public boolean emitBreakEvent()
	{
		return false;
	}
	
	public boolean emitPlaceEvent()
	{
		return false;
	}
	
	public boolean emitInteractEvent()
	{
		return false;
	}
	
	public boolean emitContainerEvent()
	{
		return false;
	}
	
	//Set's the claim manager(USually a bukkit plugin, who loads before BP, so we set this static)
	public static boolean setClaimManager(IClaimManager manager)
	{
		claimManager = manager;
		return true;
	}
	
	public static IClaimManager getClaimManager()
	{
		return claimManager;
	}
	
	public static PlayerData getPlayerData(String player)
	{
		PlayerData playerData = players.get(player);
		if(playerData == null)
		{
			//No player data exist for this player, so we create it
			playerData = new PlayerData(player);
			players.put(player, playerData);
		}
		return playerData;
	}
	
	public static boolean WriteOwnerNBT(WorldInteract item, NBTTagCompound nbt)
	{
        PlayerData owner = item.getItemOwner();
        if(owner != null && owner.getPlayer() != null)
        {
        	nbt.setString("BPOwner", owner.getPlayer().username);
        	//System.out.println("Wrote Owner(" + owner + ") for: " + item);
        }
        return true;
	}
	
	public static boolean ReadOwnerNBT(WorldInteract item, NBTTagCompound nbt)
	{
        String owner = nbt.getString("BPOwner");
        if(!owner.isEmpty())
        {
        		item.setItemOwner(BaseProtect.getPlayerData(owner));
        		//System.out.println("Read owner(" + owner + ") for: " + item);
        }
		return true;
	}
	
	//Check if interactor is relevant, update cache if changed
	public boolean isRelevant(WorldInteract interactor)
	{
		Relevant relevant = interactor.getRelevantCache();
		
		if(relevant == null)
		{
			relevant = new Relevant();
			interactor.setRelevantCache(relevant);
		}
		
		//Update cache if needed
		if(relevant.age != age)
		{
			//Check list and Update cache
			List list = null;
			switch(interactor.getInteractorType()) //We use multiple lists to avoid searching one large list
			{
			case Entity:
				list = entities;
				break;
			case TileEntity:
				list = tileEntities;
				break;
			case PlayerItem:
				list = items;
				break;
			}
			
			if(list == null)
			{
				System.err.println("BaseProtect: Interactor not of any known type!");
				return false;
			}
			
			if(list.contains(interactor.getClass()))
				relevant.relevant = true;
			else
				relevant.relevant = false;
			
			//Make sure we don't do this test again unless a reload has been done.
			relevant.age = age;
		}
		
		return relevant.relevant;
	}
	
	//Return the owner of the currently ticking interactor
	//TODO: Make it require a world for when multiple worlds run at once.
	public static Player getCurrentOwner()
	{
		WorldInteract item = World.currentTickItem;
		if(item == null)
			return null;
		
		PlayerData owner = item.getItemOwner();
		if(owner == null)
			return null;
		
		Player player = owner.getBukkitPlayer();
		return player;
		
	}
	
	//Whether or not the claim manager should ingore events
	public static void claimIgnoreEvents(boolean skip)
	{
		if(claimManager != null)
			claimManager.setSkipEvent(skip);
	}
}
