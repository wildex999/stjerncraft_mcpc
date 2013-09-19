package w999.baseprotect;

//Given to anything that can interact with blocks, Entities or tileentities in the world
//For now this is given to Entities, TileEntities and players

public interface IWorldInteract {
	
	//Set the owner
	public boolean setItemOwner(PlayerData player);
	
	//Get the owner for this World Interactor
	public PlayerData getItemOwner();
	
	//Position
	long getX();
	long getY();
	long getZ();
}
