package w999.baseprotect;

import w999.baseprotect.BaseProtect.InteractorType;

//A Temp World Interactor used to handle special cases

public class TempWorldInteractor extends WorldInteract {
	
	TempWorldInteractor()
	{
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

}
