package w999.baseprotect;

import org.bukkit.Location;

public interface IClaimManager {

	//Return whether or not the interactor is allowed to build in the given position
	public boolean claimCanBuild(WorldInteract interactor, Location loc);
	
	//Return whether or not the interactor is allowed to interact with objects in the given position
	public boolean claimCanInteract(WorldInteract interactor, Location loc);
	
	//Return whether or not the interactor is allowed to access containers in the given position
	public boolean claimCanContainer(WorldInteract interactor, Location loc);
	
	//Whether or not to ignore events to Claim Manager
	public void setSkipEvent(boolean skip);

}
