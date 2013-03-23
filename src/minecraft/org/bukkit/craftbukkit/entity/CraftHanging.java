package org.bukkit.craftbukkit.entity;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;

public class CraftHanging extends CraftEntity implements Hanging {
    public CraftHanging(CraftServer server, net.minecraft.entity.EntityHanging/*was:EntityHanging*/ entity) {
        super(server, entity);
    }

    public BlockFace getAttachedFace() {
        return getFacing().getOppositeFace();
    }

    public void setFacingDirection(BlockFace face) {
        setFacingDirection(face, false);
    }

    public boolean setFacingDirection(BlockFace face, boolean force) {
        Block block = getLocation().getBlock().getRelative(getAttachedFace()).getRelative(face.getOppositeFace()).getRelative(getFacing());
        net.minecraft.entity.EntityHanging/*was:EntityHanging*/ hanging = getHandle();
        int x = hanging.xPosition/*was:x*/, y = hanging.yPosition/*was:y*/, z = hanging.zPosition/*was:z*/, dir = hanging.hangingDirection/*was:direction*/;
        hanging.xPosition/*was:x*/ = block.getX();
        hanging.yPosition/*was:y*/ = block.getY();
        hanging.zPosition/*was:z*/ = block.getZ();
        switch (face) {
            case SOUTH:
            default:
                getHandle().setDirection/*was:setDirection*/(0);
                break;
            case WEST:
                getHandle().setDirection/*was:setDirection*/(1);
                break;
            case NORTH:
                getHandle().setDirection/*was:setDirection*/(2);
                break;
            case EAST:
                getHandle().setDirection/*was:setDirection*/(3);
                break;
        }
        if (!force && !hanging.onValidSurface/*was:survives*/()) {
            // Revert since it doesn't fit
            hanging.xPosition/*was:x*/ = x;
            hanging.yPosition/*was:y*/ = y;
            hanging.zPosition/*was:z*/ = z;
            hanging.setDirection/*was:setDirection*/(dir);
            return false;
        }
        return true;
    }

    public BlockFace getFacing() {
        switch (this.getHandle().hangingDirection/*was:direction*/) {
            case 0:
            default:
                return BlockFace.SOUTH;
            case 1:
                return BlockFace.WEST;
            case 2:
                return BlockFace.NORTH;
            case 3:
                return BlockFace.EAST;
        }
    }

    @Override
    public net.minecraft.entity.EntityHanging/*was:EntityHanging*/ getHandle() {
        return (net.minecraft.entity.EntityHanging/*was:EntityHanging*/) entity;
    }

    @Override
    public String toString() {
        return "CraftHanging";
    }

    public EntityType getType() {
        return EntityType.UNKNOWN;
    }
}
