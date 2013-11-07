package org.bukkit.event.block;

import net.minecraft.world.World;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import w999.baseprotect.IWorldInteract;
import w999.baseprotect.PlayerData;

/**
 * Called when a block is broken by a player.
 * <p>
 * If you wish to have the block drop experience, you must set the experience value above 0.
 * By default, experience will be set in the event if:
 * <ol>
 * <li />The player is not in creative or adventure mode
 * <li />The player can loot the block (ie: does not destroy it completely, by using the correct tool)
 * <li />The player does not have silk touch
 * <li />The block drops experience in vanilla MineCraft
 * </ol>
 * <p>
 * Note:
 * Plugins wanting to simulate a traditional block drop should set the block to air and utilize their own methods for determining
 *   what the default drop for the block being broken is and what to do about it, if anything.
 * <p>
 * If a Block Break event is cancelled, the block will not break and experience will not drop.
 */
public class BlockBreakEvent extends BlockExpEvent implements Cancellable {
    private final Player player;
    private boolean cancel;

    public BlockBreakEvent(final Block theBlock, final Player player) {
        super(theBlock, 0);
        
        Player realPlayer = null;
        
		//BaseProtect, Attempt to get the real fakePlayer for the current item
		//TODO: Any better way that does not involve doing a lookup? Cache?
		IWorldInteract interactor = World.currentTickItem;
		if(interactor != null && ((player != null && player.getName().startsWith("[")) || player == null))
		{
			PlayerData playerData = interactor.getItemOwner();
			if(playerData != null)
				realPlayer = playerData.getBukkitPlayer();
		}
		
		if(realPlayer == null)
			this.player = player;
		else
			this.player = realPlayer;

    }

    /**
     * Gets the Player that is breaking the block involved in this event.
     *
     * @return The Player that is breaking the block involved in this event
     */
    public Player getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return cancel;
    }

    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}