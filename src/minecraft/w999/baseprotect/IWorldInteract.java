package w999.baseprotect;

import w999.baseprotect.BaseProtect.InteractorType;
import w999.baseprotect.IWorldInteract.Relevant;

//Given to anything that can interact with blocks, Entities or tileentities in the world
//For now this is given to Entities, TileEntities and players

public interface IWorldInteract {
	
	//Relevant: Used to cache whether or not an interactor is in the BaseProtect list of interactors to check
	//This means an object only has to lookup itself in the list once per instance instead of for every check
	public class Relevant{
		int age = -1; //Age increases whenever BaseProtect reloads. If age is not the same, deemed as unknown and require lookup.
		boolean relevant = false; //Set to true if relevant
	}
	
	//Set the owner
	public boolean setItemOwner(PlayerData player);
	
	//Get the owner for this World Interactor
	public PlayerData getItemOwner();
	
	//Position
	long getX();
	long getY();
	long getZ();
	
	//Interactor type
	InteractorType getInteractorType();
	
	//Id(Returns -1 if not applicable)
	int getId();
	
	//(Cache)Is Releveant
	//BaseProtect will take care of setting, getting and updating.
	public Relevant getRelevantCache();
	public void setRelevantCache(Relevant relevant);
	
}
