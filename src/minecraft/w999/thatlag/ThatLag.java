package w999.thatlag;


/*
 * ThatLag's function is to measure the time used for 
 * Entities, TileEntities and block changes(setBlock or setBlockMetadata),
 * and try to reduce these by skipping ticks for the thing using a lot of time.
 * 
 * For example, if the average server TPS < 19, and Entities are using 50% time, then skip ticks for
 * Entities until the average server TPS is ~20.
 * Likewise, if the free % goes up, increase Entity TPS towards 20 again.
 * 
 * The goal is to reduce Block lag and make the player experience more smooth
 * even in the event of an overloaded server, especially for spikes on peak hours.
 * 
 * 
 */

public class ThatLag {

}
