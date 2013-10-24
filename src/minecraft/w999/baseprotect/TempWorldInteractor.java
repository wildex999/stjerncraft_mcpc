package w999.baseprotect;

import w999.baseprotect.BaseProtect.InteractorType;

//A Temp World Interactor used to handle special cases

public class TempWorldInteractor implements IWorldInteract {

	Relevant relevant = null;
	PlayerData owner = null;
	
	TempWorldInteractor()
	{
	}
	
	@Override
	public boolean setItemOwner(PlayerData player) {
		owner = player;
		return true;
	}

	@Override
	public PlayerData getItemOwner() {
		return owner;
	}

	@Override
	public long getX() {
		return 0;
	}

	@Override
	public long getY() {
		return 0;
	}

	@Override
	public long getZ() {
		return 0;
	}

	@Override
	public InteractorType getInteractorType() {
		return InteractorType.Entity;
	}

	@Override
	public Relevant getRelevantCache() {
		return relevant;
	}

	@Override
	public void setRelevantCache(Relevant relevant) {
		this.relevant = relevant;
	}

}
