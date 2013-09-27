package w999.baseprotect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetServerHandler;
import net.minecraft.tileentity.TileEntity;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public class PlayerEventHandler implements Listener {
	
	@EventHandler(priority = EventPriority.HIGHEST) //Right clicking with inspect should skip the rest
	public void onPlayerInspectEntity(PlayerInteractEntityEvent event)
	{
		Entity entity = null;
		try {
			entity = ((CraftEntity)event.getRightClicked()).getHandle();
		}
		catch (Exception e)
		{
			System.err.println("BASEPROTECT Error casting player in inspect!");
			return;
		}
		if(inspect(event.getPlayer(), entity, true))
			event.setCancelled(true);
	}
	
	//On player join we set the player as it's own owner and registers the fake player if it doesn't already exist
	@EventHandler(priority = EventPriority.LOWEST) //We need to apply permissions, so do this event AFTER everything else
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		CraftPlayer player;
		try {
			player = (CraftPlayer)event.getPlayer();
		}
		catch(Exception e)
		{
			System.err.println("BASEPROTECT: Could not cast player to Entity");
			return;
		}
		//Set Owner(Itself)
		BaseProtect bp = BaseProtect.instance;
		PlayerData fakePlayer = bp.getPlayerData(event.getPlayer().getName());
		if(bp != null)
			player.getHandle().setItemOwner(fakePlayer);
    	else
    		System.err.println("Failed to set Player owner due to BaseProtect not being initialized!");
		
		//Get permissions from player and apply the to fakePlayer
		((CraftPlayer)fakePlayer.getBukkitPlayer()).perm = player.perm;
	}
	
	//On player interact with a block, check if it's a tile entity, and if so get owner
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInspectBlock(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		BaseProtect baseProtect = BaseProtect.instance;
		if(!baseProtect.getPlayerData(player.getName()).inspect)
			return; //Quit if inspection isn't enabled(Allow us to skip the Entity/TileEntity lookup if not needed)
		
		CraftWorld world;
		try {
			world = (CraftWorld) event.getClickedBlock().getWorld();
		} catch (Exception e)
		{
			System.out.println("BaseProtect, Error casting world to CraftWorld on player Inspect block");
			return ;
		}
		
		TileEntity tile = world.getTileEntityAt(block.getX(), block.getY(), block.getZ());
		if(tile != null)
		{
			if(inspect(player, tile, false))
				event.setCancelled(true);
		}
		
	}
	
	//If the given player is in inspect mode, inspect the given interactor
	//Returns true if inspecting
	private boolean inspect(Player player, IWorldInteract interactor, boolean doCheck)
	{
		BaseProtect baseProtect = BaseProtect.instance;
		if(doCheck && !baseProtect.getPlayerData(player.getName()).inspect)
			return false;
		
		//Send data about Entity owner
		if(interactor.getItemOwner() == null || interactor.getItemOwner().getPlayer() == null)
			player.sendMessage(ChatColor.GOLD + "Clicked Interactor " + interactor.getClass().getName() + " with no owner");
		else
		{
			EntityPlayerMP fakePlayer = interactor.getItemOwner().getPlayer();
			player.sendMessage(ChatColor.GOLD + "Clicked Interactor " + interactor.getClass().getName() + " with owner " + fakePlayer.username);
			//Debug: Print some info about the fake player
			player.sendMessage("Owner has the position X:" + fakePlayer.posX + " Y:" + fakePlayer.posY + " Z:" + fakePlayer.posZ);
		}
		
		return true;
	}
}
