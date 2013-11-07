package w999.thatlag;

import java.util.ArrayList;
import java.util.Iterator;

//Timing the cpu usage of a tick and give average

public class TimeWatch {

	public enum TimeType {
		Tick, //Global time of one tick
		Entity, //Time of one entity tick
		TileEntity //Time of one tile entity tick
	};
	
	private static long tickStartTime;
	private static long entityStartTime;
	private static long tileEntityStartTime;
	
	private static ArrayList<Long> tickAvgWindow = new ArrayList<Long>(); //Average window for the global tick
	private static ArrayList<Long> entityAvgWindow = new ArrayList<Long>();
	private static ArrayList<Long> tileEntityAvgWindow = new ArrayList<Long>();
	
	private static int windowSize = 20; //Window size for the moving average
	
	
	//Get average tick time in percentage of a tick(50 ms)
	public static double getTickTime()
	{
		double avg = getMovingAverage(tickAvgWindow);
		//1 tick = 50ms, 1 ms = 1000000 nanosecond
		return ((avg/1000000) / 50.0);
	}
	
	//Get average Entity tick time in percentage of a tick
	public static double getEntityTime()
	{
		double avg = getMovingAverage(entityAvgWindow);
		return ((avg/1000000) / 50.0);
	}
	
	//Get average Tile Entity tick time in percentage of a tick
	public static double getTileEntityTime()
	{
		double avg = getMovingAverage(tileEntityAvgWindow);
		return ((avg/1000000) / 50.0);
	}
	
	//Start timing a tick
	public static void timeStart(TimeType type)
	{
		switch(type)
		{
		case Tick:
			//Get the start time, used to calculate how long it ran when timeEnd is called.
			tickStartTime = System.nanoTime();
			break;
		case Entity:
			entityStartTime = System.nanoTime();
			break;
		case TileEntity:
			tileEntityStartTime = System.nanoTime();
			break;
		}
	}
	
	//Pause the timing
	public static void timePause(TimeType type)
	{
		switch(type)
		{
		case Tick:
			//Store the current time used
			break;
		}
	}
	
	//End timing a tick
	public static void timeEnd(TimeType type)
	{
		double avg = 0;
		long currentTime = System.nanoTime();
		switch(type)
		{
		case Tick:
			//Remove the first(oldest) element from window if we are above the window size
			if(tickAvgWindow.size() >= windowSize)
				tickAvgWindow.remove(0);
			//Add new point
			tickAvgWindow.add(currentTime - tickStartTime);
			break;
		case Entity:
			if(entityAvgWindow.size() >= windowSize)
				entityAvgWindow.remove(0);
			entityAvgWindow.add(currentTime - entityStartTime);
			break;
		case TileEntity:
			if(tileEntityAvgWindow.size() >= windowSize)
				tileEntityAvgWindow.remove(0);
			tileEntityAvgWindow.add(currentTime - tileEntityStartTime);
			break;
		}
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
