package org.bukkit.craftbukkit.command;

import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;

/**
 * Represents input from a command block
 */
public class CraftBlockCommandSender extends ServerCommandSender implements BlockCommandSender {
    private final net.minecraft.tileentity.TileEntityCommandBlock/*was:TileEntityCommand*/ commandBlock;

    public CraftBlockCommandSender(net.minecraft.tileentity.TileEntityCommandBlock/*was:TileEntityCommand*/ commandBlock) {
        super();
        this.commandBlock = commandBlock;
    }

    public Block getBlock() {
        return commandBlock.worldObj/*was:world*/.getWorld().getBlockAt(commandBlock.xCoord/*was:x*/, commandBlock.yCoord/*was:y*/, commandBlock.zCoord/*was:z*/);
    }

    public void sendMessage(String message) {
    }

    public void sendRawMessage(String message) {
    }

    public void sendMessage(String[] messages) {
    }

    public String getName() {
        return "@";
    }

    public boolean isOp() {
        return true;
    }

    public void setOp(boolean value) {
        throw new UnsupportedOperationException("Cannot change operator status of a block");
    }
}
