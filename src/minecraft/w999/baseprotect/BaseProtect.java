package w999.baseprotect;

import java.util.List;

public class BaseProtect {
	private List<Class> entities; //List of Entities to check
	private List<Class> tileEntities; //List of TileEntities to check
	
	public void initialize()
	{
		//Check if Grief Prevention exists
	}
	
	//get Class from string, verify it's has IBlockInteracte(instanceof IBlockInteract) and store it in the list of entities
	public void addInteractor(String entity)
	{
		
	}
	
	//Check if entity has build permission in target Grief Prevention claim
	public void claimBuildCheck()
	{
		
	}
	
	public void claimInteractCheck()
	{
		
	}
	
	public void claimContainerCheck()
	{
		
	}
	
	//Emit a full Block Break event(For any plugin)
	public void emitBreakEvent()
	{
		
	}
	
	public void emitInteractEvent()
	{
		
	}
	
	public void emitContainerEvent()
	{
		
	}
}
