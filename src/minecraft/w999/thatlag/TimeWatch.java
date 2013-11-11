package w999.thatlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//Timing the cpu usage of a tick and give average

public class TimeWatch {

	public enum TimeType {
		Tick, //Global tick time
		Entity, //Entity time
		TileEntity //TileEntity time
	};
	
	static class WatchObject {
		public long startTime;
		public boolean paused;
		public long currentDiff;
		public ArrayList<Long> avgWindow;
		
		WatchObject(long startValMs) { paused = false; avgWindow = new ArrayList<Long>(); avgWindow.add(startValMs*1000000); }
	}
	
	private static HashMap<TimeType, WatchObject> watchObjects;
	
	static {
		watchObjects = new HashMap<TimeType, WatchObject>();
		//Give them some values to start with, or else things crash quite quickly(divide by 0 etc.)
		watchObjects.put(TimeType.Tick, new WatchObject(30));
		watchObjects.put(TimeType.Entity, new WatchObject(15));
		watchObjects.put(TimeType.TileEntity, new WatchObject(15));	
	}
	
	private static int windowSize = 20; //Window size for the moving average
	
	
	//Return the average time used of a tick(1 = 100% of a tick)
	public static double getAvgTime(TimeType type)
	{
		WatchObject obj = watchObjects.get(type);
		double avg = getMovingAverage(obj.avgWindow);
		//1 tick = 50ms, 1 ms = 1000000 nanosecond
		return ((avg/1000000.0) / 50.0);
	}
	
	//Return the previous measured time
	public static double getPreviousTime(TimeType type)
	{
		WatchObject obj = watchObjects.get(type);
		double time = obj.avgWindow.get(obj.avgWindow.size() -1);
		return (time/1000000.0) / 50.0;
	}
	
	//Start timing a tick
	public static void timeStart(TimeType type)
	{
		WatchObject obj = watchObjects.get(type);
		obj.startTime = System.nanoTime();
		obj.currentDiff = 0;
		obj.paused = false;
	}
	
	//Pause the timing
	public static void timePause(TimeType type)
	{
		WatchObject obj = watchObjects.get(type);
		long start = obj.startTime;
		obj.paused = true;
		
		//Get the stored diff, and add the current diff to it
		obj.currentDiff += (System.nanoTime() - start);
	}
	
	//Resume the timing
	public static void timeResume(TimeType type)
	{
		timeStart(type);
	}
	
	//End timing a tick
	public static void timeEnd(TimeType type)
	{
		WatchObject obj = watchObjects.get(type);
		ArrayList<Long> list = obj.avgWindow;
		
		//If we are not paused we need to call pause to add on the last diff
		if(!obj.paused)
			timePause(type);
		
		//Remove the first(oldest) element from window if we are above the window size
		if(list.size() == windowSize)
			list.remove(0);
		//Add new point
		list.add(obj.currentDiff);

		obj.paused = false;
	}
	
	//Calculate moving average
	private static double getMovingAverage(ArrayList<Long> elements)
	{
		double cumulative = 0;
		for(long item : elements)
			cumulative += item;
		
		return cumulative / (double)elements.size();
	}
}
