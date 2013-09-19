package w999.baseprotect;

import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinEventHandler implements Listener {

	//On player join we set the player as it's own owner and registers the fake player if it doesn't already exist
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		CraftEntity player;
		try {
			player = (CraftEntity) event.getPlayer();
		}
		catch(Exception e)
		{
			System.err.println("BASEPROTECT: Could not cast player to Entity");
			return;
		}
		//Set Owner(Itself)
		player.getHandle().setItemOwner(new PlayerData(event.getPlayer().getName()));
		System.out.println("SET PLAYER ON JOIN");
		
	}
}
