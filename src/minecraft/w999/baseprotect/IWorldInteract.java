package w999.baseprotect;

import net.minecraftforge.common.FakePlayer;

//Given to anything that can interact with blocks, Entities or tileentities in the world
//For now this is given to Entities, TileEntities and players

public interface IWorldInteract {
	
	//Set the fakeplayer that represents the real player
	boolean setItemOwner(FakePlayer owner);
	
	//Find an existing fakeplayer for the given playername, or creates a new one if it doesn't exist
	boolean setItemOwner(String player);
	
	//Get the fake player for this World Interactor
	FakePlayer getItemOwner();
	
	//Position
	long getX();
	long getY();
	long getZ();
}
