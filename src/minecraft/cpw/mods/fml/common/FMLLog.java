/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     cpw - implementation
 */

package cpw.mods.fml.common;

import java.util.logging.Level;
import java.util.logging.Logger;

import w999.baseprotect.IWorldInteract;
import net.minecraft.world.World;

public class FMLLog
{
    private static cpw.mods.fml.relauncher.FMLRelaunchLog coreLog = cpw.mods.fml.relauncher.FMLRelaunchLog.log;

    public static void log(String logChannel, Level level, String format, Object... data)
    {
    	//TODO: If turned on, print the current object in each world when logging(To find sources of problems)
    	IWorldInteract item = World.currentTickItem;
		if(item == null)
		{
			format = " (NULLITEM) " + format;
		}
		else
		{
			String className = World.currentTickItem.getClass().getName();
			int last = className.lastIndexOf(".");
			if(last != -1)
				className = className.substring(last);
			
			format = " (" + className + " [X: " + item.getX() + " Y: " + item.getY() + " Z: " + item.getZ() +"]) " + format;
		}
        coreLog.log(logChannel, level, format, data);
    }

    public static void log(Level level, String format, Object... data)
    {
    	IWorldInteract item = World.currentTickItem;
    	if(level.equals(Level.SEVERE))
    	{
    		//TODO: Remove this apply a proper fix!!!
    		//For now, ignore any SEVERE message from TileController, du to spam when part of network is in unloading chunks(Can safely be ignored)
    		try {
				if(Class.forName("appeng.me.tile.TileController").isInstance(item))
				{
					return;
				}
			} catch (ClassNotFoundException e) {
				//Do nothing if TileController doesn't exists
				//SEVERE log's should be rather rare anyway!
			}
    	}
    	
		if(item == null)
		{
			format = " (NULLITEM) " + format;
		}
		else
		{
			String className = World.currentTickItem.getClass().getName();
			int last = className.lastIndexOf(".");
			if(last != -1)
				className = className.substring(last);
			
			format = " (" + className + " [X: " + item.getX() + " Y: " + item.getY() + " Z: " + item.getZ() +"]) " + format;
		}
    	
        coreLog.log(level, format, data);
    }

    public static void log(String logChannel, Level level, Throwable ex, String format, Object... data)
    {
        coreLog.log(logChannel, level, ex, format, data);
    }

    public static void log(Level level, Throwable ex, String format, Object... data)
    {
        coreLog.log(level, ex, format, data);
    }

    public static void severe(String format, Object... data)
    {
        log(Level.SEVERE, format, data);
    }

    public static void warning(String format, Object... data)
    {
    	if(format.contains("API ERROR: ic2.")) //MCPC+ - Filter out IC2 net spamming
    	{
    		System.out.println("Filtered IC2 Warning");
    		return;
    	}
        log(Level.WARNING, format, data);
    }

    public static void info(String format, Object... data)
    {
        log(Level.INFO, format, data);
    }

    public static void fine(String format, Object... data)
    {
        log(Level.FINE, format, data);
    }

    public static void finer(String format, Object... data)
    {
        log(Level.FINER, format, data);
    }

    public static void finest(String format, Object... data)
    {
        log(Level.FINEST, format, data);
    }
    public static Logger getLogger()
    {
        return coreLog.getLogger();
    }

    public static void makeLog(String logChannel)
    {
        coreLog.makeLog(logChannel);
    }
}
