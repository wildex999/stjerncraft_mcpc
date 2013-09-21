package w999.baseprotect;

import org.bukkit.entity.Player;

import za.co.mcportcentral.entity.CraftFakePlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.DimensionManager;

//Stores some data for the players, including their fake player


public class PlayerData {
	public boolean inspect; //Whether or not the player is in inspect mode
	private EntityPlayerMP fakePlayer; //A EntityPlayerMP created using CraftFakePlayer
	
	//Set the player from the username
	public boolean setPlayer(String username)
	{
		//Return a player if it exists, create if it doesn't(And login, but don't join. This will give it the right permissions)
		//Uses world 0(Main world) TODO: Make this changeable? (Afraid the player will exist in a world that unloads)
		fakePlayer = CraftFakePlayer.get(DimensionManager.getWorld(0), username, true, false);
		//TODO: Do some testing on the player(Is the player banned? Does the player actually exist? etc.)
		
		return true;
	}
	
	public EntityPlayerMP getPlayer()
	{
		return fakePlayer;
	}
	
	public Player getBukkitPlayer()
	{
		if(fakePlayer != null)
			return fakePlayer.getBukkitEntity();
		
		return null;
	}
	
	public PlayerData(String username)
	{
		setPlayer(username);
	}
	
	@Override
	public String toString()
	{
		if(fakePlayer != null)
			return fakePlayer.username;
		else
			return "Null";
	}
}
