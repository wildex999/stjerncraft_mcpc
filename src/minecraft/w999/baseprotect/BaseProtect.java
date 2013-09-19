package w999.baseprotect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

public class BaseProtect extends JavaPlugin {
	public static BaseProtect instance;
	
	private List<Class> entities = new ArrayList<Class>(); //List of Entities to check
	private List<Class> tileEntities = new ArrayList<Class>(); //List of TileEntities to check
	
	private HashMap<String, PlayerData> commandPlayers = new HashMap<String, PlayerData>(); //Data for players using commands
	
	
	public BaseProtect(CraftServer server)
	{
		instance = this;
		
		//Check if Grief Prevention exists
		
		//Register command handler
		server.getCommandMap().register("baseprotect2", new CommandBaseProtect());
		
		//Register event handlers
		Bukkit.getPluginManager().registerEvents(new PlayerEventHandler(), Bukkit.getPluginManager().getPlugins()[0]);
		Bukkit.getPluginManager().registerEvents(new PlayerJoinEventHandler(), Bukkit.getPluginManager().getPlugins()[0]);
	}
	
	@Override
	public void onEnable()
	{
		
	}
	
	//get Class from string, verify it's has IBlockInteracte(instanceof IBlockInteract) and store it in the list of entities
	public void addInteractor(String entity)
	{
		
	}
	
	//Check if entity has build permission in target Grief Prevention claim
	public void claimBuildCheck()
	{
		
	}
	
	public void claimInteractCheck()
	{
		
	}
	
	public void claimContainerCheck()
	{
		
	}
	
	//Emit a full Block Break event(For any plugin)
	public void emitBreakEvent()
	{
		
	}
	
	public void emitInteractEvent()
	{
		
	}
	
	public void emitContainerEvent()
	{
		
	}
	
	public PlayerData getPlayerData(String player)
	{
		PlayerData playerData = commandPlayers.get(player);
		if(playerData == null)
		{
			//No player data exist for this player, so we create it
			playerData = new PlayerData(player);
			commandPlayers.put(player, playerData);
		}
		return playerData;
	}
}
