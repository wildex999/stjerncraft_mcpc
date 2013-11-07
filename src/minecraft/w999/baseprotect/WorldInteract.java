package w999.baseprotect;

import w999.baseprotect.BaseProtect.InteractorType;

//Given to anything that can interact with blocks, Entities or tileentities in the world
//For now this is given to Entities, TileEntities and Items

public abstract class WorldInteract {
	
	PlayerData entityOwner;
	Relevant relevantCache;
	
	//Set the owner
	final public boolean setItemOwner(PlayerData owner)
	{
    	entityOwner = owner;
		return true;
	}
	
	//Get the owner for this World Interactor
	final public PlayerData getItemOwner()
	{
		return entityOwner;
	}
	
	//Position
	public abstract long getX();
	public abstract long getY();
	public abstract long getZ();
	
	//Interactor type
	public abstract InteractorType getInteractorType();
	
	//(Cache)Is Releveant
	//BaseProtect will take care of setting, getting and updating.
	final public Relevant getRelevantCache()
	{
		return relevantCache;
	}
	
	final public void setRelevantCache(Relevant relevant)
	{
		relevantCache = relevant;
	}
	
}
