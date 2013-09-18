package net.minecraft.server.dedicated;

import java.io.OutputStream;
import java.io.PrintStream;

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
}
