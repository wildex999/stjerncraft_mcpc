package net.minecraft.server.dedicated;

import java.io.OutputStream;
import java.io.PrintStream;

import w999.baseprotect.IWorldInteract;
import net.minecraft.world.World;

//TEMP LOGGER TO FILTER OUT THE EXCEPTION FROM APPLIED ENERGISTICS CONTROLLER
//TODO: REMOVE WHEN ERROR HAS BEEN FIXED

public class TempPrintStream extends PrintStream {

	public TempPrintStream(OutputStream out, boolean autoFlush) {
		super(out, autoFlush);
	}
	
	@Override
	public void println(Object obj)
	{
		//Ignore any sever log from AE Controller
		try {
			if(Class.forName("appeng.me.tile.TileController").isInstance(World.currentTickItem))
			{
				return;
			}
		} catch (ClassNotFoundException e) {
			//Do nothing if TileController doesn't exists
			//SEVERE log's should be rather rare anyway!
		}
		//Print anything else
		super.println(obj);
	}
	
	//TEMP: Print Item and location
	@Override
	public void println(String str)
	{
		//WARNING, NOT THREAD SAFE. World.currentTickItem can be set to null in another thread AFTER null check. 
		IWorldInteract item = World.currentTickItem;
		String format = str;
		if(item == null)
		{
			format = " (NULLITEM) " + format;
		}
		else
		{
			String className = item.getClass().getName(); //Try to avoid thread conflict
			int last = className.lastIndexOf(".");
			if(last != -1)
				className = className.substring(last);
			
			format = " (" + className + " [X: " + item.getX() + " Y: " + item.getY() + " Z: " + item.getZ() +"]) " + format;
		}
		super.println(format);
	}
}
