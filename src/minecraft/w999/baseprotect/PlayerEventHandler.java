package w999.baseprotect;

import net.minecraft.entity.Entity;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.craftbukkit.entity.CraftEntity;

public class PlayerEventHandler implements Listener {
	
	@EventHandler(priority = EventPriority.HIGHEST) //Right clicking with inspect should skip the rest
	public void onPlayerInspectEntity(PlayerInteractEntityEvent event)
	{
		Entity entity = ((CraftEntity)event.getRightClicked()).getHandle();
		if(entity instanceof Entity)
			System.out.println("IS OF ENTITY");
		else
			System.out.println("IS NOT OF ENTITY");
		if(inspect(event.getPlayer(), entity))
			event.setCancelled(true);
	}
	
	//If the given player is in inspect mode, inspect the given interactor
	//Returns true if inspecting
	private boolean inspect(Player player, IWorldInteract interactor)
	{
		BaseProtect baseProtect = BaseProtect.instance;
		if(!baseProtect.getPlayerData(player.getName()).inspect)
			return false;
		
		//Send data about Entity owner
		if(interactor.getItemOwner() == null || interactor.getItemOwner().getPlayer() == null)
			player.sendMessage(ChatColor.GOLD + "Clicked entity " + interactor.getClass().getName() + " with no owner");
		else
			player.sendMessage(ChatColor.GOLD + "Clicked entity " + interactor.getClass().getName() + " with owner " + interactor.getItemOwner().getPlayer().username);
		
		return true;
	}
}
